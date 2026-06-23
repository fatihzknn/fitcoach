package com.fitcoach.profile;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.OnboardingRequest;
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
@RequestMapping("/api")
@Tag(name = "Profile", description = "Onboarding and fitness profile")
@SecurityRequirement(name = "bearer-jwt")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/onboarding")
    @Operation(summary = "Submit onboarding answers and complete setup")
    public ResponseEntity<FitnessProfileDto> completeOnboarding(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody OnboardingRequest request) {
        FitnessProfileDto dto = profileService.completeOnboarding(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/profile")
    @Operation(summary = "Get the current user's fitness profile")
    public FitnessProfileDto getProfile(@AuthenticationPrincipal CurrentUser currentUser) {
        return profileService.getProfile(currentUser);
    }
}
