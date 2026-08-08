package com.fitcoach.coach;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.User;
import com.fitcoach.auth.UserRepository;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.coach.domain.MessageRole;
import com.fitcoach.coach.dto.ChatMessageDto;
import com.fitcoach.coach.provider.CoachAiProvider;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.session.WorkoutSession;
import com.fitcoach.session.WorkoutSessionRepository;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.WorkoutDay;
import com.fitcoach.workout.WorkoutPlan;
import com.fitcoach.workout.WorkoutPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CoachService had zero dedicated unit tests before this — safety-critical (pain
 * handling lives in MockCoachAiProvider, which is tested separately), but the
 * context-building and conversation-persistence logic here was untested.
 */
@ExtendWith(MockitoExtension.class)
class CoachServiceTest {

    @Mock private ChatConversationRepository conversationRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private CoachPrincipleRepository principleRepository;
    @Mock private UserRepository userRepository;
    @Mock private FitnessProfileRepository profileRepository;
    @Mock private WorkoutPlanRepository planRepository;
    @Mock private WorkoutSessionRepository sessionRepository;
    @Mock private CoachAiProvider aiProvider;

    @InjectMocks
    private CoachService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    private void stubNoProfileNoPlanNoSessions() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(List.of());
        when(principleRepository.findAll()).thenReturn(List.of());
    }

    private ChatMessage message(ChatConversation conv, MessageRole role, String content) {
        return conv.addMessage(role, content);
    }

    // ─── sendMessage — conversation lifecycle ──────────────────────────────────

    @Test
    void sendMessage_createsNewConversationWhenNoneExists() {
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(aiProvider.generateResponse(any(), any())).thenReturn("How can I help?");
        stubNoProfileNoPlanNoSessions();

        service.sendMessage(CURRENT_USER, "Hi coach");

        ArgumentCaptor<ChatConversation> captor = ArgumentCaptor.forClass(ChatConversation.class);
        verify(conversationRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void sendMessage_reusesExistingConversation() {
        ChatConversation existing = new ChatConversation(USER_ID);
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(aiProvider.generateResponse(any(), any())).thenReturn("Sure, tell me more.");
        stubNoProfileNoPlanNoSessions();

        service.sendMessage(CURRENT_USER, "Follow-up question");

        assertThat(existing.getMessages()).hasSize(2);
        assertThat(existing.getMessages().get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(existing.getMessages().get(0).getContent()).isEqualTo("Follow-up question");
        assertThat(existing.getMessages().get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(existing.getMessages().get(1).getContent()).isEqualTo("Sure, tell me more.");
    }

    @Test
    void sendMessage_returnsTheLastTwoMessagesFromTheRepository() {
        ChatConversation conv = new ChatConversation(USER_ID);
        message(conv, MessageRole.USER, "old question");
        message(conv, MessageRole.ASSISTANT, "old answer");

        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Live view: reflects whatever the service has appended to `conv` by the time
        // this is read, matching the real repository's "read what was just written" behavior.
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId()))
                .thenReturn(conv.getMessages());
        when(aiProvider.generateResponse(any(), any())).thenReturn("new answer");
        stubNoProfileNoPlanNoSessions();

        List<ChatMessageDto> result = service.sendMessage(CURRENT_USER, "new question");

        assertThat(conv.getMessages()).hasSize(4);
        ChatMessage newUser = conv.getMessages().get(2);
        ChatMessage newAssistant = conv.getMessages().get(3);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(newUser.getId());
        assertThat(result.get(0).content()).isEqualTo("new question");
        assertThat(result.get(1).id()).isEqualTo(newAssistant.getId());
        assertThat(result.get(1).content()).isEqualTo("new answer");
    }

    // ─── sendMessage — AI context building ─────────────────────────────────────

    @Test
    void sendMessage_buildsContextWithProfileDataWhenProfileExists() {
        User user = new User("jane@example.com", "hash", "Jane", Role.USER);
        FitnessProfile profile = new FitnessProfile(USER_ID);
        profile.setMainGoal(MainGoal.FAT_LOSS);
        profile.setTrainingBackground(TrainingBackground.RETURNING);
        profile.setTrainingDaysPerWeek(4);
        profile.setPainAreas(Set.of(PainArea.KNEE));

        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(List.of());
        when(principleRepository.findAll()).thenReturn(List.of());
        when(aiProvider.generateResponse(any(), any())).thenReturn("ok");

        service.sendMessage(CURRENT_USER, "hi");

        ArgumentCaptor<CoachContext> captor = ArgumentCaptor.forClass(CoachContext.class);
        verify(aiProvider).generateResponse(eq("hi"), captor.capture());
        CoachContext context = captor.getValue();

        assertThat(context.userName()).isEqualTo("Jane");
        assertThat(context.mainGoal()).isEqualTo("FAT_LOSS");
        assertThat(context.trainingBackground()).isEqualTo("RETURNING");
        assertThat(context.trainingDaysPerWeek()).isEqualTo(4);
        assertThat(context.painAreas()).containsExactly("KNEE");
    }

    @Test
    void sendMessage_fallsBackToDefaultsWhenNoProfileExists() {
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(aiProvider.generateResponse(any(), any())).thenReturn("ok");
        stubNoProfileNoPlanNoSessions();

        service.sendMessage(CURRENT_USER, "hi");

        ArgumentCaptor<CoachContext> captor = ArgumentCaptor.forClass(CoachContext.class);
        verify(aiProvider).generateResponse(eq("hi"), captor.capture());
        CoachContext context = captor.getValue();

        assertThat(context.userName()).isEqualTo("there");
        assertThat(context.mainGoal()).isEqualTo("GENERAL_FITNESS");
        assertThat(context.trainingBackground()).isEqualTo("STARTING");
        assertThat(context.trainingDaysPerWeek()).isEqualTo(3);
        assertThat(context.painAreas()).isEmpty();
        assertThat(context.activePlanName()).isNull();
    }

    @Test
    void sendMessage_includesActivePlanNameWhenPresent() {
        WorkoutPlan plan = new WorkoutPlan(USER_ID, "Upper/Lower Split", MainGoal.MUSCLE_GAIN, 4);
        plan.activate();

        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(List.of());
        when(principleRepository.findAll()).thenReturn(List.of());
        when(aiProvider.generateResponse(any(), any())).thenReturn("ok");

        service.sendMessage(CURRENT_USER, "hi");

        ArgumentCaptor<CoachContext> captor = ArgumentCaptor.forClass(CoachContext.class);
        verify(aiProvider).generateResponse(eq("hi"), captor.capture());
        assertThat(captor.getValue().activePlanName()).isEqualTo("Upper/Lower Split");
    }

    @Test
    void sendMessage_capsRecentSessionsAtFive() {
        WorkoutPlan plan = new WorkoutPlan(USER_ID, "Plan", MainGoal.MUSCLE_GAIN, 4);
        List<WorkoutSession> sessions = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            WorkoutDay day = new WorkoutDay(plan, i + 1, "Day " + i);
            sessions.add(new WorkoutSession(USER_ID, plan, day));
        }

        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(sessions);
        when(principleRepository.findAll()).thenReturn(List.of());
        when(aiProvider.generateResponse(any(), any())).thenReturn("ok");

        service.sendMessage(CURRENT_USER, "hi");

        ArgumentCaptor<CoachContext> captor = ArgumentCaptor.forClass(CoachContext.class);
        verify(aiProvider).generateResponse(eq("hi"), captor.capture());
        assertThat(captor.getValue().recentSessionSummaries()).hasSize(5);
    }

    @Test
    void sendMessage_historyExcludesTheJustAddedPairAndCapsAtTen() {
        ChatConversation conv = new ChatConversation(USER_ID);
        // 13 pre-existing messages already "in the DB" per the repository stub;
        // the service's own addMessage() calls during this run are on top of these.
        List<ChatMessage> preExisting = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            preExisting.add(message(conv, i % 2 == 0 ? MessageRole.USER : MessageRole.ASSISTANT, "msg " + i));
        }

        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Simulates: by the time buildContext() reads it, the DB already reflects the
        // 13 prior messages plus the just-persisted new user message (14 total).
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId()))
                .thenReturn(preExisting);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(List.of());
        when(principleRepository.findAll()).thenReturn(List.of());
        when(aiProvider.generateResponse(any(), any())).thenReturn("ok");

        service.sendMessage(CURRENT_USER, "newest question");

        ArgumentCaptor<CoachContext> captor = ArgumentCaptor.forClass(CoachContext.class);
        verify(aiProvider).generateResponse(eq("newest question"), captor.capture());
        // historyEnd = 13 - 2 = 11; window = [max(0, 11-10), 11) = [1, 11) -> 10 messages
        assertThat(captor.getValue().recentHistory()).hasSize(10);
        assertThat(captor.getValue().recentHistory().get(0).getContent()).isEqualTo("msg 1");
        assertThat(captor.getValue().recentHistory().get(9).getContent()).isEqualTo("msg 10");
    }

    // ─── getHistory ─────────────────────────────────────────────────────────────

    @Test
    void getHistory_returnsEmptyWhenNoConversationExists() {
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        List<ChatMessageDto> result = service.getHistory(CURRENT_USER);

        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_returnsFullOrderedHistoryWhenConversationExists() {
        ChatConversation conv = new ChatConversation(USER_ID);
        message(conv, MessageRole.USER, "q1");
        message(conv, MessageRole.ASSISTANT, "a1");

        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(conv));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId()))
                .thenReturn(conv.getMessages());

        List<ChatMessageDto> result = service.getHistory(CURRENT_USER);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).content()).isEqualTo("q1");
        assertThat(result.get(1).content()).isEqualTo("a1");
    }
}
