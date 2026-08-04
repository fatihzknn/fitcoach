package com.fitcoach.workout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.workout.domain.PlanOption;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.SelectPlanRequest;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WorkoutController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class WorkoutControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean WorkoutPlanService planService;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
    }

    @Test
    void getPlanOptionsReturns200WithBothOptions() throws Exception {
        WorkoutPlanDto recommended = planDto("Full Body A/B", 3);
        WorkoutPlanDto alternative = planDto("Machine-Friendly Full Body", 3);
        when(planService.getPlanOptions(any(), any())).thenReturn(new PlanOptionsResponse(recommended, alternative));

        mockMvc.perform(get("/api/plan/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommended.name").value("Full Body A/B"))
                .andExpect(jsonPath("$.alternative.name").value("Machine-Friendly Full Body"));
    }

    @Test
    void selectPlanReturns201WithActivePlan() throws Exception {
        WorkoutPlanDto saved = planDto("Full Body A/B", 3);
        when(planService.selectPlan(any(), any())).thenReturn(saved);

        String body = objectMapper.writeValueAsString(new SelectPlanRequest(PlanOption.RECOMMENDED, null));

        mockMvc.perform(post("/api/plan/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Full Body A/B"));
    }

    @Test
    void selectPlanWithNullOptionReturns400() throws Exception {
        mockMvc.perform(post("/api/plan/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"option\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getActivePlanReturns200WhenPlanExists() throws Exception {
        WorkoutPlanDto active = planDto("Upper / Lower Split", 4);
        when(planService.getActivePlan(any())).thenReturn(active);

        mockMvc.perform(get("/api/plan/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Upper / Lower Split"));
    }

    @Test
    void getActivePlanReturns404WhenNoPlanSelected() throws Exception {
        when(planService.getActivePlan(any()))
                .thenThrow(new NotFoundException("No active workout plan. Select a plan first."));

        mockMvc.perform(get("/api/plan/active"))
                .andExpect(status().isNotFound());
    }

    private WorkoutPlanDto planDto(String name, int days) {
        return new WorkoutPlanDto(
                UUID.randomUUID(), name, MainGoal.MUSCLE_GAIN, days,
                true, null,
                List.of(new WorkoutDayDto(UUID.randomUUID(), 1, "Day 1", List.of())),
                null, null, false
        );
    }
}
