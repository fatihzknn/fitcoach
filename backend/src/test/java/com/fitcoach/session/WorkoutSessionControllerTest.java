package com.fitcoach.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.session.dto.LogSetRequest;
import com.fitcoach.session.dto.StartSessionRequest;
import com.fitcoach.session.dto.WorkoutSessionDto;
import com.fitcoach.workout.dto.WorkoutDayDto;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WorkoutSessionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class WorkoutSessionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean WorkoutSessionService sessionService;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
    }

    private WorkoutSessionDto sessionDto() {
        return new WorkoutSessionDto(
                UUID.randomUUID(), UUID.randomUUID(),
                new WorkoutDayDto(UUID.randomUUID(), 1, "Upper A", List.of()),
                SessionStatus.IN_PROGRESS, Instant.now(), null, List.of(), Set.of()
        );
    }

    @Test
    void startSession_returns201() throws Exception {
        when(sessionService.startSession(any(), any())).thenReturn(sessionDto());

        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void startSession_missingDayId_returns400() throws Exception {
        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startSession_dayNotFound_returns404() throws Exception {
        when(sessionService.startSession(any(), any())).thenThrow(new NotFoundException("Workout day not found."));

        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(UUID.randomUUID()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActiveSession_returns200() throws Exception {
        when(sessionService.getActiveSession(any())).thenReturn(sessionDto());

        mockMvc.perform(get("/api/sessions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutDay.workoutName").value("Upper A"));
    }

    @Test
    void getActiveSession_returns404WhenNone() throws Exception {
        when(sessionService.getActiveSession(any())).thenThrow(new NotFoundException("No active session."));

        mockMvc.perform(get("/api/sessions/active"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logSet_returns200() throws Exception {
        when(sessionService.logSet(any(), any(), any())).thenReturn(sessionDto());
        LogSetRequest request = new LogSetRequest(UUID.randomUUID(), 1, new BigDecimal("60.0"), 10, null);

        mockMvc.perform(post("/api/sessions/" + UUID.randomUUID() + "/sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void logSet_negativeReps_returns400() throws Exception {
        String body = "{\"workoutExerciseId\":\"" + UUID.randomUUID() + "\",\"setNumber\":1,\"repsCompleted\":-1}";

        mockMvc.perform(post("/api/sessions/" + UUID.randomUUID() + "/sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeSession_withoutBody_returns200() throws Exception {
        when(sessionService.completeSession(any(), any(), any())).thenReturn(sessionDto());

        mockMvc.perform(post("/api/sessions/" + UUID.randomUUID() + "/complete"))
                .andExpect(status().isOk());
    }

    @Test
    void getHistory_returns200() throws Exception {
        when(sessionService.getHistory(any())).thenReturn(List.of(sessionDto()));

        mockMvc.perform(get("/api/sessions/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPreviousSets_returns200() throws Exception {
        when(sessionService.getPreviousSets(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/sessions/previous-sets").param("exerciseId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getExerciseHistory_returns200() throws Exception {
        when(sessionService.getExerciseHistory(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/sessions/exercise-history").param("exerciseId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }
}
