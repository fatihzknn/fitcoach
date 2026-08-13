package com.fitcoach.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.profile.domain.BarbellComfort;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.OnboardingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ProfileController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ProfileService profileService;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
    }

    private OnboardingRequest validRequest() {
        return new OnboardingRequest(MainGoal.MUSCLE_GAIN, TrainingBackground.REGULAR, 4, 60,
                28, 178, 80.0, Sex.MALE, Set.of(PainArea.NONE), BarbellComfort.COMFORTABLE);
    }

    private FitnessProfileDto dto() {
        return new FitnessProfileDto(UUID.randomUUID(), MainGoal.MUSCLE_GAIN, TrainingBackground.REGULAR,
                4, 60, 28, 178, 80.0, Sex.MALE, Set.of(PainArea.NONE), BarbellComfort.COMFORTABLE,
                true, java.time.Instant.now());
    }

    @Test
    void completeOnboarding_returns201() throws Exception {
        when(profileService.completeOnboarding(any(), any())).thenReturn(dto());

        mockMvc.perform(post("/api/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mainGoal").value("MUSCLE_GAIN"));
    }

    @Test
    void completeOnboarding_missingMainGoal_returns400() throws Exception {
        mockMvc.perform(post("/api/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProfile_returns200() throws Exception {
        when(profileService.getProfile(any())).thenReturn(dto());

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barbellComfort").value("COMFORTABLE"));
    }

    @Test
    void getProfile_returns404WhenNoneExists() throws Exception {
        when(profileService.getProfile(any()))
                .thenThrow(new NotFoundException("No fitness profile yet. Complete onboarding first."));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isNotFound());
    }
}
