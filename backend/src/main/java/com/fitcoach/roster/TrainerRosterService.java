package com.fitcoach.roster;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.User;
import com.fitcoach.auth.UserRepository;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.checkin.WeeklyCheckInService;
import com.fitcoach.checkin.dto.ProgressStatsDto;
import com.fitcoach.common.ForbiddenException;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.profile.ProfileService;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.OnboardingRequest;
import com.fitcoach.roster.dto.ClientDetailDto;
import com.fitcoach.roster.dto.ClientSummaryDto;
import com.fitcoach.roster.dto.RedeemInviteRequest;
import com.fitcoach.roster.dto.SendTrainerMessageRequest;
import com.fitcoach.roster.dto.TrainerConnectionSummaryDto;
import com.fitcoach.roster.dto.TrainerInviteDto;
import com.fitcoach.roster.dto.TrainerMessageDto;
import com.fitcoach.workout.WorkoutPlanRepository;
import com.fitcoach.workout.WorkoutPlanService;
import com.fitcoach.workout.dto.CreateCustomPlanRequest;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.SelectPlanRequest;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class TrainerRosterService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // no 0/O, 1/I/L
    private static final int CODE_LENGTH = 8;
    private static final int CODE_EXPIRY_DAYS = 30;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final TrainerInviteRepository inviteRepository;
    private final TrainerClientRepository trainerClientRepository;
    private final UserRepository userRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanService planService;
    private final WeeklyCheckInService checkInService;
    private final ProfileService profileService;
    private final TrainerMessageService messageService;
    private final TransactionTemplate requiresNewTransaction;
    private final SecureRandom random = new SecureRandom();

    public TrainerRosterService(TrainerInviteRepository inviteRepository,
                                 TrainerClientRepository trainerClientRepository,
                                 UserRepository userRepository,
                                 WorkoutPlanRepository planRepository,
                                 WorkoutPlanService planService,
                                 WeeklyCheckInService checkInService,
                                 ProfileService profileService,
                                 TrainerMessageService messageService,
                                 PlatformTransactionManager transactionManager) {
        this.inviteRepository = inviteRepository;
        this.trainerClientRepository = trainerClientRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planService = planService;
        this.checkInService = checkInService;
        this.profileService = profileService;
        this.messageService = messageService;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public TrainerInviteDto getOrCreateInviteCode(CurrentUser trainer) {
        requireTrainerRole(trainer);
        TrainerInvite invite = inviteRepository.findByTrainerId(trainer.id())
                .orElseGet(() -> createInviteRaceSafe(trainer.id()));

        if (invite.isExpired(Instant.now())) {
            invite.rotate(generateUniqueCode(), newExpiry());
            invite = inviteRepository.save(invite);
        }
        return TrainerInviteDto.from(invite);
    }

    /**
     * Two concurrent first-time requests (e.g. React StrictMode's double effect
     * invocation in dev, or a client retry) can both see "no invite yet" and both
     * try to insert one, tripping uq_trainer_invites_trainer. Postgres aborts the
     * whole transaction on a constraint violation, so the losing insert must run
     * in its own REQUIRES_NEW transaction — that way only the failed insert rolls
     * back, and the caller's still-healthy outer transaction can just read the
     * row the winner created.
     */
    private TrainerInvite createInviteRaceSafe(UUID trainerId) {
        try {
            return requiresNewTransaction.execute(status ->
                    inviteRepository.saveAndFlush(new TrainerInvite(trainerId, generateUniqueCode(), newExpiry())));
        } catch (DataIntegrityViolationException e) {
            return inviteRepository.findByTrainerId(trainerId).orElseThrow(() -> e);
        }
    }

    @Transactional
    public TrainerInviteDto regenerateInviteCode(CurrentUser trainer) {
        requireTrainerRole(trainer);
        TrainerInvite invite = inviteRepository.findByTrainerId(trainer.id())
                .orElseGet(() -> new TrainerInvite(trainer.id(), generateUniqueCode(), newExpiry()));
        invite.rotate(generateUniqueCode(), newExpiry());
        return TrainerInviteDto.from(inviteRepository.save(invite));
    }

    @Transactional
    public void redeemInviteCode(CurrentUser client, RedeemInviteRequest request) {
        if (client.role() == Role.TRAINER) {
            throw new ForbiddenException("Trainer accounts can't connect to another trainer.");
        }

        TrainerInvite invite = inviteRepository.findByCode(request.code().trim().toUpperCase())
                .filter(i -> !i.isExpired(Instant.now()))
                .orElseThrow(() -> new NotFoundException("Invite code not found or expired."));

        // Idempotent: redeeming a code you've already redeemed just confirms the link.
        trainerClientRepository.findByTrainerIdAndClientId(invite.getTrainerId(), client.id())
                .orElseGet(() -> trainerClientRepository.save(
                        new TrainerClient(invite.getTrainerId(), client.id())));
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryDto> listClients(CurrentUser trainer) {
        requireTrainerRole(trainer);
        return trainerClientRepository.findAllByTrainerId(trainer.id()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientDetailDto getClientDetail(CurrentUser trainer, UUID clientId) {
        requireTrainerRole(trainer);
        TrainerClient link = requireOwnedClient(trainer.id(), clientId);
        ProgressStatsDto stats = checkInService.getStatsForUser(clientId);
        WorkoutPlanDto activePlan = planService.getActivePlanForUser(clientId);
        return new ClientDetailDto(toSummary(link), stats, activePlan);
    }

    @Transactional(readOnly = true)
    public PlanOptionsResponse getClientPlanOptions(CurrentUser trainer, UUID clientId, UUID trainerPhilosophyId) {
        requireTrainerRole(trainer);
        requireOwnedClient(trainer.id(), clientId);
        return planService.getPlanOptionsForUser(clientId, trainerPhilosophyId);
    }

    @Transactional
    public WorkoutPlanDto assignPlanToClient(CurrentUser trainer, UUID clientId, SelectPlanRequest request) {
        requireTrainerRole(trainer);
        requireOwnedClient(trainer.id(), clientId);
        return planService.selectPlanForUser(clientId, request);
    }

    @Transactional
    public WorkoutPlanDto createCustomPlanForClient(CurrentUser trainer, UUID clientId,
                                                      CreateCustomPlanRequest request) {
        requireTrainerRole(trainer);
        requireOwnedClient(trainer.id(), clientId);
        return planService.createCustomPlanForUser(clientId, request);
    }

    @Transactional(readOnly = true)
    public FitnessProfileDto getClientProfile(CurrentUser trainer, UUID clientId) {
        requireTrainerRole(trainer);
        requireOwnedClient(trainer.id(), clientId);
        return profileService.getProfileForUser(clientId);
    }

    @Transactional
    public FitnessProfileDto updateClientProfile(CurrentUser trainer, UUID clientId, OnboardingRequest request) {
        requireTrainerRole(trainer);
        requireOwnedClient(trainer.id(), clientId);
        return profileService.completeOnboardingForUser(clientId, request);
    }

    @Transactional
    public TrainerMessageDto sendMessageToClient(CurrentUser trainer, UUID clientId, SendTrainerMessageRequest request) {
        requireTrainerRole(trainer);
        TrainerClient link = requireOwnedClient(trainer.id(), clientId);
        return messageService.sendMessage(link, trainer.id(), request.content());
    }

    @Transactional(readOnly = true)
    public List<TrainerMessageDto> getMessagesWithClient(CurrentUser trainer, UUID clientId) {
        requireTrainerRole(trainer);
        TrainerClient link = requireOwnedClient(trainer.id(), clientId);
        return messageService.getHistory(link, trainer.id());
    }

    @Transactional(readOnly = true)
    public List<TrainerConnectionSummaryDto> listMyTrainers(CurrentUser client) {
        requireClientRole(client);
        return trainerClientRepository.findAllByClientId(client.id()).stream()
                .map(this::toConnectionSummary)
                .toList();
    }

    @Transactional
    public TrainerMessageDto sendMessageToTrainer(CurrentUser client, UUID trainerId, SendTrainerMessageRequest request) {
        requireClientRole(client);
        TrainerClient link = requireLinkedTrainer(trainerId, client.id());
        return messageService.sendMessage(link, client.id(), request.content());
    }

    @Transactional(readOnly = true)
    public List<TrainerMessageDto> getMessagesWithTrainer(CurrentUser client, UUID trainerId) {
        requireClientRole(client);
        TrainerClient link = requireLinkedTrainer(trainerId, client.id());
        return messageService.getHistory(link, client.id());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ClientSummaryDto toSummary(TrainerClient link) {
        User client = userRepository.findById(link.getClientId())
                .orElseThrow(() -> new NotFoundException("Client not found."));
        String activePlanName = planRepository.findByUserIdAndIsActiveTrue(link.getClientId())
                .map(p -> p.getName())
                .orElse(null);
        ProgressStatsDto stats = checkInService.getStatsForUser(link.getClientId());
        return new ClientSummaryDto(
                client.getId(),
                client.getDisplayName(),
                client.getEmail(),
                link.getCreatedAt(),
                activePlanName,
                stats.adherenceRate4Weeks(),
                stats.currentStreakWeeks()
        );
    }

    private void requireTrainerRole(CurrentUser user) {
        if (user.role() != Role.TRAINER) {
            throw new ForbiddenException("This action is only available to trainer accounts.");
        }
    }

    /** Mirrors redeemInviteCode's existing guard — a trainer account has no trainer of its own. */
    private void requireClientRole(CurrentUser user) {
        if (user.role() == Role.TRAINER) {
            throw new ForbiddenException("Trainer accounts don't have their own trainer connections.");
        }
    }

    /** Ownership check, not a role check — matches WorkoutSessionService.requireOwnedSession's
     *  pattern exactly (404, not 403, so a trainer can't distinguish "not your client" from
     *  "doesn't exist" by probing UUIDs). */
    private TrainerClient requireOwnedClient(UUID trainerId, UUID clientId) {
        return trainerClientRepository.findByTrainerIdAndClientId(trainerId, clientId)
                .orElseThrow(() -> new NotFoundException("Client not found."));
    }

    /** Same lookup, reversed perspective — a client resolving a link to one of their trainers. */
    private TrainerClient requireLinkedTrainer(UUID trainerId, UUID clientId) {
        return trainerClientRepository.findByTrainerIdAndClientId(trainerId, clientId)
                .orElseThrow(() -> new NotFoundException("Trainer not found."));
    }

    private TrainerConnectionSummaryDto toConnectionSummary(TrainerClient link) {
        User trainer = userRepository.findById(link.getTrainerId())
                .orElseThrow(() -> new NotFoundException("Trainer not found."));
        return new TrainerConnectionSummaryDto(trainer.getId(), trainer.getDisplayName(), trainer.getEmail(), link.getCreatedAt());
    }

    private Instant newExpiry() {
        return Instant.now().plus(CODE_EXPIRY_DAYS, ChronoUnit.DAYS);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = randomCode();
            if (!inviteRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique invite code.");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
