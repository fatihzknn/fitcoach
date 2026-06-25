package com.fitcoach.coach;

import com.fitcoach.auth.UserRepository;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.coach.domain.MessageRole;
import com.fitcoach.coach.dto.ChatMessageDto;
import com.fitcoach.coach.provider.CoachAiProvider;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.session.WorkoutSessionRepository;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.WorkoutPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CoachService {

    private static final int MAX_HISTORY_FOR_AI = 10;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final CoachPrincipleRepository principleRepository;
    private final UserRepository userRepository;
    private final FitnessProfileRepository profileRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final CoachAiProvider aiProvider;

    public CoachService(ChatConversationRepository conversationRepository,
                        ChatMessageRepository messageRepository,
                        CoachPrincipleRepository principleRepository,
                        UserRepository userRepository,
                        FitnessProfileRepository profileRepository,
                        WorkoutPlanRepository planRepository,
                        WorkoutSessionRepository sessionRepository,
                        CoachAiProvider aiProvider) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.principleRepository = principleRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.planRepository = planRepository;
        this.sessionRepository = sessionRepository;
        this.aiProvider = aiProvider;
    }

    @Transactional
    public List<ChatMessageDto> sendMessage(CurrentUser currentUser, String userMessage) {
        ChatConversation conversation = conversationRepository.findByUserId(currentUser.id())
                .orElseGet(() -> conversationRepository.save(new ChatConversation(currentUser.id())));

        // Persist user message
        conversation.addMessage(MessageRole.USER, userMessage);
        conversationRepository.save(conversation);

        // Build context and generate AI response
        CoachContext context = buildContext(currentUser, conversation);
        String aiResponse = aiProvider.generateResponse(userMessage, context);

        // Persist AI response
        conversation.addMessage(MessageRole.ASSISTANT, aiResponse);
        ChatConversation saved = conversationRepository.save(conversation);

        // Return the last two messages (user + assistant)
        List<ChatMessage> all = messageRepository.findByConversationIdOrderByCreatedAtAsc(saved.getId());
        int size = all.size();
        return all.subList(Math.max(0, size - 2), size)
                .stream()
                .map(ChatMessageDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getHistory(CurrentUser currentUser) {
        return conversationRepository.findByUserId(currentUser.id())
                .map(conv -> messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId())
                        .stream()
                        .map(ChatMessageDto::from)
                        .toList())
                .orElse(Collections.emptyList());
    }

    // ─── Context builder ──────────────────────────────────────────────────────

    private CoachContext buildContext(CurrentUser currentUser, ChatConversation conversation) {
        String userName = userRepository.findById(currentUser.id())
                .map(u -> u.getDisplayName())
                .orElse("there");

        Optional<FitnessProfile> profileOpt = profileRepository.findByUserId(currentUser.id());

        String mainGoal = profileOpt.map(p -> p.getMainGoal().name()).orElse("GENERAL_FITNESS");
        String background = profileOpt.map(p -> p.getTrainingBackground().name()).orElse("STARTING");
        int trainingDays = profileOpt.map(FitnessProfile::getTrainingDaysPerWeek).orElse(3);
        List<String> painAreas = profileOpt
                .map(p -> p.getPainAreas().stream().map(Enum::name).toList())
                .orElse(Collections.emptyList());

        String planName = planRepository.findByUserIdAndIsActiveTrue(currentUser.id())
                .map(plan -> plan.getName())
                .orElse(null);

        List<String> recentSessions = sessionRepository
                .findAllByUserIdAndStatusOrderByStartedAtDesc(currentUser.id(), SessionStatus.COMPLETED)
                .stream()
                .limit(5)
                .map(s -> s.getWorkoutDay().getWorkoutName())
                .toList();

        List<CoachPrinciple> principles = principleRepository.findAll();

        // Last N messages for AI context (excluding the messages just added)
        List<ChatMessage> allMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        int historyEnd = Math.max(0, allMessages.size() - 2); // exclude the pair just added
        List<ChatMessage> history = allMessages.subList(
                Math.max(0, historyEnd - MAX_HISTORY_FOR_AI), historyEnd);

        return new CoachContext(
                userName,
                mainGoal,
                background,
                trainingDays,
                painAreas,
                planName,
                recentSessions,
                principles,
                history
        );
    }
}
