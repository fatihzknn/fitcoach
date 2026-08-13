package com.fitcoach.roster;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.common.ForbiddenException;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.profile.domain.BarbellComfort;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.profile.dto.FitnessProfileDto;
import com.fitcoach.profile.dto.OnboardingRequest;
import com.fitcoach.roster.dto.ClientSummaryDto;
import com.fitcoach.roster.dto.TrainerInviteDto;
import com.fitcoach.workout.dto.CreateCustomPlanRequest;
import com.fitcoach.workout.dto.CustomPlanDayRequest;
import com.fitcoach.workout.dto.CustomPlanExerciseRequest;
import com.fitcoach.workout.dto.WorkoutDayDto;
import com.fitcoach.workout.dto.WorkoutPlanDto;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TrainerRosterController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TrainerRosterControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TrainerRosterService rosterService;
    @MockBean JwtService jwtService;

    private static final UUID TRAINER_ID = UUID.randomUUID();
    private static final CurrentUser TRAINER = new CurrentUser(TRAINER_ID, "coach@example.com", Role.TRAINER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TRAINER, null, List.of())
        );
    }

    @Test
    void getClients_returns200WithList() throws Exception {
        ClientSummaryDto client = new ClientSummaryDto(
                UUID.randomUUID(), "Jamie", "jamie@example.com", Instant.now(), "Upper / Lower", 80, 4);
        when(rosterService.listClients(any())).thenReturn(List.of(client));

        mockMvc.perform(get("/api/trainer/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Jamie"));
    }

    @Test
    void getClients_returns403WhenCallerIsNotATrainer() throws Exception {
        when(rosterService.listClients(any()))
                .thenThrow(new ForbiddenException("This action is only available to trainer accounts."));

        mockMvc.perform(get("/api/trainer/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getClientDetail_returns404ForUnownedClient() throws Exception {
        when(rosterService.getClientDetail(any(), any()))
                .thenThrow(new NotFoundException("Client not found."));

        mockMvc.perform(get("/api/trainer/clients/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInviteCode_returns200WithCode() throws Exception {
        when(rosterService.getOrCreateInviteCode(any()))
                .thenReturn(new TrainerInviteDto("ABCD2345", Instant.now().plusSeconds(3600)));

        mockMvc.perform(get("/api/trainer/invite-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABCD2345"));
    }

    @Test
    void createCustomPlan_returns201() throws Exception {
        WorkoutPlanDto saved = new WorkoutPlanDto(
                UUID.randomUUID(), "Custom Plan", MainGoal.MUSCLE_GAIN, 1,
                true, null,
                List.of(new WorkoutDayDto(UUID.randomUUID(), 1, "Day A", List.of())),
                null, null, false, true
        );
        when(rosterService.createCustomPlanForClient(any(), any(), any())).thenReturn(saved);

        String body = objectMapper.writeValueAsString(customPlanRequest());

        mockMvc.perform(post("/api/trainer/clients/" + UUID.randomUUID() + "/custom-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Custom Plan"))
                .andExpect(jsonPath("$.isCustom").value(true));
    }

    @Test
    void createCustomPlan_returns404ForUnownedClient() throws Exception {
        when(rosterService.createCustomPlanForClient(any(), any(), any()))
                .thenThrow(new NotFoundException("Client not found."));

        String body = objectMapper.writeValueAsString(customPlanRequest());

        mockMvc.perform(post("/api/trainer/clients/" + UUID.randomUUID() + "/custom-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    private CreateCustomPlanRequest customPlanRequest() {
        return new CreateCustomPlanRequest("Custom Plan", MainGoal.MUSCLE_GAIN,
                List.of(new CustomPlanDayRequest("Day A",
                        List.of(new CustomPlanExerciseRequest(UUID.randomUUID(), 3, 8, 12, "2 RIR", 90)))));
    }

    private OnboardingRequest profileEditRequest() {
        return new OnboardingRequest(MainGoal.STRENGTH, TrainingBackground.REGULAR, 5, 75,
                30, 180, 82.0, Sex.MALE, Set.of(PainArea.KNEE), BarbellComfort.PREFER_ALTERNATIVES);
    }

    private FitnessProfileDto profileDto() {
        return new FitnessProfileDto(UUID.randomUUID(), MainGoal.STRENGTH, TrainingBackground.REGULAR,
                5, 75, 30, 180, 82.0, Sex.MALE, Set.of(PainArea.KNEE), BarbellComfort.PREFER_ALTERNATIVES,
                true, Instant.now());
    }

    @Test
    void getClientProfile_returns200() throws Exception {
        when(rosterService.getClientProfile(any(), any())).thenReturn(profileDto());

        mockMvc.perform(get("/api/trainer/clients/" + UUID.randomUUID() + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainGoal").value("STRENGTH"));
    }

    @Test
    void getClientProfile_returns404ForUnownedClient() throws Exception {
        when(rosterService.getClientProfile(any(), any()))
                .thenThrow(new NotFoundException("Client not found."));

        mockMvc.perform(get("/api/trainer/clients/" + UUID.randomUUID() + "/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateClientProfile_returns200() throws Exception {
        when(rosterService.updateClientProfile(any(), any(), any())).thenReturn(profileDto());

        mockMvc.perform(put("/api/trainer/clients/" + UUID.randomUUID() + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileEditRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barbellComfort").value("PREFER_ALTERNATIVES"));
    }

    @Test
    void updateClientProfile_missingMainGoal_returns400() throws Exception {
        mockMvc.perform(put("/api/trainer/clients/" + UUID.randomUUID() + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateClientProfile_returns404ForUnownedClient() throws Exception {
        when(rosterService.updateClientProfile(any(), any(), any()))
                .thenThrow(new NotFoundException("Client not found."));

        mockMvc.perform(put("/api/trainer/clients/" + UUID.randomUUID() + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileEditRequest())))
                .andExpect(status().isNotFound());
    }
}
