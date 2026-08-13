package com.fitcoach.roster;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.OnboardingRequest;
import com.fitcoach.roster.dto.ClientDetailDto;
import com.fitcoach.roster.dto.ClientSummaryDto;
import com.fitcoach.roster.dto.TrainerInviteDto;
import com.fitcoach.workout.dto.CreateCustomPlanRequest;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.SelectPlanRequest;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Trainer-only endpoints — every method rejects non-TRAINER callers with 403. */
@RestController
@RequestMapping("/api/trainer")
@Tag(name = "Trainer Roster", description = "Trainer-facing client roster: invite codes, client list, plan assignment")
@SecurityRequirement(name = "bearer-jwt")
public class TrainerRosterController {

    private final TrainerRosterService rosterService;

    public TrainerRosterController(TrainerRosterService rosterService) {
        this.rosterService = rosterService;
    }

    @GetMapping("/invite-code")
    @Operation(summary = "Get this trainer's standing invite code, creating or rotating it if expired")
    public TrainerInviteDto getInviteCode(@AuthenticationPrincipal CurrentUser currentUser) {
        return rosterService.getOrCreateInviteCode(currentUser);
    }

    @PostMapping("/invite-code/regenerate")
    @Operation(summary = "Rotate this trainer's invite code (e.g. if it leaked)")
    public TrainerInviteDto regenerateInviteCode(@AuthenticationPrincipal CurrentUser currentUser) {
        return rosterService.regenerateInviteCode(currentUser);
    }

    @GetMapping("/clients")
    @Operation(summary = "List clients linked to this trainer")
    public List<ClientSummaryDto> getClients(@AuthenticationPrincipal CurrentUser currentUser) {
        return rosterService.listClients(currentUser);
    }

    @GetMapping("/clients/{clientId}")
    @Operation(summary = "Get one client's progress stats and active plan")
    public ClientDetailDto getClientDetail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID clientId) {
        return rosterService.getClientDetail(currentUser, clientId);
    }

    @GetMapping("/clients/{clientId}/plan-options")
    @Operation(summary = "Generate plan options for a client, optionally shaped by a trainer philosophy")
    public PlanOptionsResponse getClientPlanOptions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID clientId,
            @RequestParam(required = false) UUID trainerId) {
        return rosterService.getClientPlanOptions(currentUser, clientId, trainerId);
    }

    @PostMapping("/clients/{clientId}/plan")
    @Operation(summary = "Assign a generated plan option as the client's active workout plan")
    public WorkoutPlanDto assignClientPlan(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID clientId,
            @Valid @RequestBody SelectPlanRequest request) {
        return rosterService.assignPlanToClient(currentUser, clientId, request);
    }

    @PostMapping("/clients/{clientId}/custom-plan")
    @Operation(summary = "Build and assign a fully custom exercise-by-exercise plan for a client")
    public ResponseEntity<WorkoutPlanDto> createCustomPlan(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateCustomPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rosterService.createCustomPlanForClient(currentUser, clientId, request));
    }

    @GetMapping("/clients/{clientId}/profile")
    @Operation(summary = "Get a client's fitness profile (goal, days, injuries, etc.) for editing")
    public FitnessProfileDto getClientProfile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID clientId) {
        return rosterService.getClientProfile(currentUser, clientId);
    }

    @PutMapping("/clients/{clientId}/profile")
    @Operation(summary = "Edit a client's fitness profile on their behalf")
    public FitnessProfileDto updateClientProfile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID clientId,
            @Valid @RequestBody OnboardingRequest request) {
        return rosterService.updateClientProfile(currentUser, clientId, request);
    }
}
