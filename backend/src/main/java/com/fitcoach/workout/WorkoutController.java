package com.fitcoach.workout;

import com.fitcoach.auth.jwt.CurrentUser;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plan")
@Tag(name = "Workout Plan", description = "Workout plan generation and selection")
@SecurityRequirement(name = "bearer-jwt")
public class WorkoutController {

    private final WorkoutPlanService planService;

    public WorkoutController(WorkoutPlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/options")
    @Operation(summary = "Generate recommended and alternative plan options from the user's profile")
    public PlanOptionsResponse getPlanOptions(@AuthenticationPrincipal CurrentUser currentUser) {
        return planService.getPlanOptions(currentUser);
    }

    @PostMapping("/select")
    @Operation(summary = "Save the chosen plan option as the user's active workout plan")
    public ResponseEntity<WorkoutPlanDto> selectPlan(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SelectPlanRequest request) {
        WorkoutPlanDto dto = planService.selectPlan(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/active")
    @Operation(summary = "Get the user's current active workout plan")
    public WorkoutPlanDto getActivePlan(@AuthenticationPrincipal CurrentUser currentUser) {
        return planService.getActivePlan(currentUser);
    }
}
