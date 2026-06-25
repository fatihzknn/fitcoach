package com.fitcoach.session;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.session.dto.CompleteSessionRequest;
import com.fitcoach.session.dto.ExerciseHistoryEntryDto;
import com.fitcoach.session.dto.LogSetRequest;
import com.fitcoach.session.dto.PreviousSetDto;
import com.fitcoach.session.dto.StartSessionRequest;
import com.fitcoach.session.dto.WorkoutSessionDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Workout Sessions", description = "Start, log, and complete workout sessions")
@SecurityRequirement(name = "bearer-jwt")
public class WorkoutSessionController {

    private final WorkoutSessionService sessionService;

    public WorkoutSessionController(WorkoutSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new workout session for a given workout day")
    public ResponseEntity<WorkoutSessionDto> startSession(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody StartSessionRequest request) {
        WorkoutSessionDto dto = sessionService.startSession(currentUser, request.workoutDayId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/active")
    @Operation(summary = "Get the current in-progress session, if any")
    public WorkoutSessionDto getActiveSession(@AuthenticationPrincipal CurrentUser currentUser) {
        return sessionService.getActiveSession(currentUser);
    }

    @PostMapping("/{id}/sets")
    @Operation(summary = "Log or update a single set within a session")
    public WorkoutSessionDto logSet(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody LogSetRequest request) {
        return sessionService.logSet(currentUser, id, request);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark a session as complete")
    public WorkoutSessionDto completeSession(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteSessionRequest request) {
        return sessionService.completeSession(currentUser, id,
                request != null ? request : new CompleteSessionRequest(null));
    }

    @GetMapping("/history")
    @Operation(summary = "Get all completed workout sessions for the current user, newest first")
    public List<WorkoutSessionDto> getHistory(@AuthenticationPrincipal CurrentUser currentUser) {
        return sessionService.getHistory(currentUser);
    }

    @GetMapping("/previous-sets")
    @Operation(summary = "Get the most recent completed set logs for a specific exercise")
    public List<PreviousSetDto> getPreviousSets(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam UUID exerciseId) {
        return sessionService.getPreviousSets(currentUser, exerciseId);
    }

    @GetMapping("/exercise-history")
    @Operation(summary = "Get per-session max weight and rep history for a specific exercise (for progression chart)")
    public List<ExerciseHistoryEntryDto> getExerciseHistory(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam UUID exerciseId) {
        return sessionService.getExerciseHistory(currentUser, exerciseId);
    }
}
