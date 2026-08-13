package com.fitcoach.checkin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.checkin.domain.CheckInPainStatus;
import com.fitcoach.checkin.dto.ProgressStatsDto;
import com.fitcoach.checkin.dto.SubmitCheckInRequest;
import com.fitcoach.checkin.dto.WeeklyCheckInDto;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WeeklyCheckInController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class WeeklyCheckInControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean WeeklyCheckInService checkInService;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
    }

    private WeeklyCheckInDto dto() {
        return new WeeklyCheckInDto(UUID.randomUUID(), LocalDate.now(), new BigDecimal("80.0"),
                4, 4, 2, CheckInPainStatus.NO_PAIN, null);
    }

    @Test
    void submitCheckIn_returns200() throws Exception {
        when(checkInService.submitCheckIn(any(), any())).thenReturn(dto());
        SubmitCheckInRequest request = new SubmitCheckInRequest(new BigDecimal("80.0"), 4, 4, 2,
                CheckInPainStatus.NO_PAIN, null);

        mockMvc.perform(post("/api/check-ins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.painStatus").value("NO_PAIN"));
    }

    @Test
    void submitCheckIn_ratingOutOfRange_returns400() throws Exception {
        String body = "{\"sleepQualityRating\":9,\"painStatus\":\"NO_PAIN\"}";

        mockMvc.perform(post("/api/check-ins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_returns200() throws Exception {
        when(checkInService.getHistory(any())).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/check-ins/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getStats_returns200() throws Exception {
        when(checkInService.getStats(any())).thenReturn(new ProgressStatsDto(10, 2, 3, 75, List.of()));

        mockMvc.perform(get("/api/check-ins/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adherenceRate4Weeks").value(75));
    }
}
