package com.fitcoach.checkin;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.checkin.dto.ProgressStatsDto;
import com.fitcoach.checkin.dto.SubmitCheckInRequest;
import com.fitcoach.checkin.dto.WeeklyCheckInDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/check-ins")
@Tag(name = "Weekly Check-ins", description = "Weekly progress check-ins and stats")
@SecurityRequirement(name = "bearer-jwt")
public class WeeklyCheckInController {

    private final WeeklyCheckInService checkInService;

    public WeeklyCheckInController(WeeklyCheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    @Operation(summary = "Submit or update the check-in for the current week (idempotent)")
    public WeeklyCheckInDto submitCheckIn(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SubmitCheckInRequest request) {
        return checkInService.submitCheckIn(currentUser, request);
    }

    @GetMapping("/history")
    @Operation(summary = "Get all past check-ins, newest first")
    public List<WeeklyCheckInDto> getHistory(@AuthenticationPrincipal CurrentUser currentUser) {
        return checkInService.getHistory(currentUser);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get progress stats: totals, streak, adherence, check-in history")
    public ProgressStatsDto getStats(@AuthenticationPrincipal CurrentUser currentUser) {
        return checkInService.getStats(currentUser);
    }
}
