package com.fitcoach.roster;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.User;
import com.fitcoach.auth.UserRepository;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.checkin.WeeklyCheckInService;
import com.fitcoach.checkin.dto.ProgressStatsDto;
import com.fitcoach.common.ForbiddenException;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.roster.dto.RedeemInviteRequest;
import com.fitcoach.roster.dto.TrainerInviteDto;
import com.fitcoach.workout.WorkoutPlanRepository;
import com.fitcoach.workout.WorkoutPlanService;
import com.fitcoach.workout.domain.PlanOption;
import com.fitcoach.workout.dto.SelectPlanRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerRosterServiceTest {

    @Mock private TrainerInviteRepository inviteRepository;
    @Mock private TrainerClientRepository trainerClientRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkoutPlanRepository planRepository;
    @Mock private WorkoutPlanService planService;
    @Mock private WeeklyCheckInService checkInService;

    @InjectMocks
    private TrainerRosterService service;

    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final CurrentUser TRAINER = new CurrentUser(TRAINER_ID, "coach@example.com", Role.TRAINER);
    private static final CurrentUser CLIENT = new CurrentUser(CLIENT_ID, "client@example.com", Role.USER);

    // ─── invite code lifecycle ─────────────────────────────────────────────────

    @Test
    void getOrCreateInviteCode_createsNewCodeWhenNoneExists() {
        when(inviteRepository.findByTrainerId(TRAINER_ID)).thenReturn(Optional.empty());
        when(inviteRepository.existsByCode(any())).thenReturn(false);
        when(inviteRepository.save(any(TrainerInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainerInviteDto result = service.getOrCreateInviteCode(TRAINER);

        assertThat(result.code()).hasSize(8);
        assertThat(result.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void getOrCreateInviteCode_returnsExistingCodeWithinValidity_withoutSaving() {
        TrainerInvite existing = new TrainerInvite(TRAINER_ID, "ABCD2345", Instant.now().plus(10, ChronoUnit.DAYS));
        when(inviteRepository.findByTrainerId(TRAINER_ID)).thenReturn(Optional.of(existing));

        TrainerInviteDto result = service.getOrCreateInviteCode(TRAINER);

        assertThat(result.code()).isEqualTo("ABCD2345");
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void getOrCreateInviteCode_rotatesWhenExpired() {
        TrainerInvite expired = new TrainerInvite(TRAINER_ID, "OLDCODE1", Instant.now().minus(1, ChronoUnit.DAYS));
        when(inviteRepository.findByTrainerId(TRAINER_ID)).thenReturn(Optional.of(expired));
        when(inviteRepository.existsByCode(any())).thenReturn(false);
        when(inviteRepository.save(any(TrainerInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainerInviteDto result = service.getOrCreateInviteCode(TRAINER);

        verify(inviteRepository).save(any(TrainerInvite.class));
        assertThat(result.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void regenerateInviteCode_alwaysRotatesEvenWithinValidity() {
        TrainerInvite existing = new TrainerInvite(TRAINER_ID, "ABCD2345", Instant.now().plus(10, ChronoUnit.DAYS));
        when(inviteRepository.findByTrainerId(TRAINER_ID)).thenReturn(Optional.of(existing));
        when(inviteRepository.existsByCode(any())).thenReturn(false);
        when(inviteRepository.save(any(TrainerInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        service.regenerateInviteCode(TRAINER);

        verify(inviteRepository).save(any(TrainerInvite.class));
    }

    @Test
    void inviteCodeActions_rejectNonTrainerCaller() {
        assertThatThrownBy(() -> service.getOrCreateInviteCode(CLIENT))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.regenerateInviteCode(CLIENT))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── redemption ─────────────────────────────────────────────────────────────

    @Test
    void redeemInviteCode_createsNewLink() {
        TrainerInvite invite = new TrainerInvite(TRAINER_ID, "GOODCODE", Instant.now().plus(10, ChronoUnit.DAYS));
        when(inviteRepository.findByCode("GOODCODE")).thenReturn(Optional.of(invite));
        when(trainerClientRepository.findByTrainerIdAndClientId(TRAINER_ID, CLIENT_ID)).thenReturn(Optional.empty());
        when(trainerClientRepository.save(any(TrainerClient.class))).thenAnswer(inv -> inv.getArgument(0));

        service.redeemInviteCode(CLIENT, new RedeemInviteRequest("GOODCODE"));

        ArgumentCaptor<TrainerClient> captor = ArgumentCaptor.forClass(TrainerClient.class);
        verify(trainerClientRepository).save(captor.capture());
        assertThat(captor.getValue().getTrainerId()).isEqualTo(TRAINER_ID);
        assertThat(captor.getValue().getClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    void redeemInviteCode_alreadyLinked_isIdempotent_doesNotDuplicate() {
        TrainerInvite invite = new TrainerInvite(TRAINER_ID, "GOODCODE", Instant.now().plus(10, ChronoUnit.DAYS));
        TrainerClient existingLink = new TrainerClient(TRAINER_ID, CLIENT_ID);
        when(inviteRepository.findByCode("GOODCODE")).thenReturn(Optional.of(invite));
        when(trainerClientRepository.findByTrainerIdAndClientId(TRAINER_ID, CLIENT_ID))
                .thenReturn(Optional.of(existingLink));

        service.redeemInviteCode(CLIENT, new RedeemInviteRequest("GOODCODE"));

        verify(trainerClientRepository, never()).save(any());
    }

    @Test
    void redeemInviteCode_expiredCode_throwsNotFound() {
        TrainerInvite expired = new TrainerInvite(TRAINER_ID, "DEADCODE", Instant.now().minus(1, ChronoUnit.DAYS));
        when(inviteRepository.findByCode("DEADCODE")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.redeemInviteCode(CLIENT, new RedeemInviteRequest("DEADCODE")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void redeemInviteCode_unknownCode_throwsNotFound() {
        when(inviteRepository.findByCode("NOPE0000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemInviteCode(CLIENT, new RedeemInviteRequest("NOPE0000")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void redeemInviteCode_trainerRoleCaller_throwsForbidden() {
        assertThatThrownBy(() -> service.redeemInviteCode(TRAINER, new RedeemInviteRequest("ANYCODE1")))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── roster / ownership ───────────────────────────────────────────────────

    @Test
    void listClients_nonTrainerRole_throwsForbidden() {
        assertThatThrownBy(() -> service.listClients(CLIENT))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getClientDetail_unownedClient_throwsNotFound() {
        when(trainerClientRepository.findByTrainerIdAndClientId(TRAINER_ID, CLIENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClientDetail(TRAINER, CLIENT_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getClientPlanOptions_unownedClient_throwsNotFound() {
        when(trainerClientRepository.findByTrainerIdAndClientId(TRAINER_ID, CLIENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClientPlanOptions(TRAINER, CLIENT_ID, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getClientPlanOptions_passesClientIdNotTrainerId() {
        TrainerClient link = new TrainerClient(TRAINER_ID, CLIENT_ID);
        UUID trainerPhilosophyId = UUID.randomUUID();
        when(trainerClientRepository.findByTrainerIdAndClientId(TRAINER_ID, CLIENT_ID))
                .thenReturn(Optional.of(link));

        service.getClientPlanOptions(TRAINER, CLIENT_ID, trainerPhilosophyId);

        verify(planService).getPlanOptionsForUser(eq(CLIENT_ID), eq(trainerPhilosophyId));
    }

    @Test
    void assignPlanToClient_passesClientIdNotTrainerId() {
        TrainerClient link = new TrainerClient(TRAINER_ID, CLIENT_ID);
        when(trainerClientRepository.findByTrainerIdAndClientId(TRAINER_ID, CLIENT_ID))
                .thenReturn(Optional.of(link));
        SelectPlanRequest request = new SelectPlanRequest(PlanOption.RECOMMENDED, null);

        service.assignPlanToClient(TRAINER, CLIENT_ID, request);

        verify(planService).selectPlanForUser(eq(CLIENT_ID), eq(request));
    }

    @Test
    void assignPlanToClient_unownedClient_throwsNotFound() {
        when(trainerClientRepository.findByTrainerIdAndClientId(TRAINER_ID, CLIENT_ID))
                .thenReturn(Optional.empty());
        SelectPlanRequest request = new SelectPlanRequest(PlanOption.RECOMMENDED, null);

        assertThatThrownBy(() -> service.assignPlanToClient(TRAINER, CLIENT_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listClients_buildsSummaryFromLinkedClients() {
        TrainerClient link = new TrainerClient(TRAINER_ID, CLIENT_ID);
        User clientUser = mock(User.class);
        when(clientUser.getId()).thenReturn(CLIENT_ID);
        when(clientUser.getDisplayName()).thenReturn("Jamie");
        when(clientUser.getEmail()).thenReturn("client@example.com");
        when(trainerClientRepository.findAllByTrainerId(TRAINER_ID)).thenReturn(List.of(link));
        when(userRepository.findById(CLIENT_ID)).thenReturn(Optional.of(clientUser));
        when(planRepository.findByUserIdAndIsActiveTrue(CLIENT_ID)).thenReturn(Optional.empty());
        when(checkInService.getStatsForUser(CLIENT_ID))
                .thenReturn(new ProgressStatsDto(10, 2, 3, 75, List.of()));

        List<com.fitcoach.roster.dto.ClientSummaryDto> result = service.listClients(TRAINER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).displayName()).isEqualTo("Jamie");
        assertThat(result.get(0).adherenceRate4Weeks()).isEqualTo(75);
        assertThat(result.get(0).activePlanName()).isNull();
    }
}
