package com.fitcoach.measurement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.measurement.dto.BodyMeasurementDto;
import com.fitcoach.measurement.dto.SaveMeasurementRequest;
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

@WebMvcTest(value = BodyMeasurementController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class BodyMeasurementControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean BodyMeasurementService service;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
    }

    private BodyMeasurementDto dto() {
        return new BodyMeasurementDto(UUID.randomUUID(), LocalDate.now(), new BigDecimal("80.0"),
                null, new BigDecimal("85.0"), null, null, null, null, null,
                new BigDecimal("18.5"), "NAVY", null);
    }

    @Test
    void save_returns201() throws Exception {
        when(service.save(any(), any())).thenReturn(dto());
        SaveMeasurementRequest request = new SaveMeasurementRequest(null, new BigDecimal("80.0"),
                null, new BigDecimal("85.0"), null, null, null, null, null, null);

        mockMvc.perform(post("/api/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bodyFatMethod").value("NAVY"));
    }

    @Test
    void getHistory_returns200() throws Exception {
        when(service.getHistory(any())).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/measurements/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getLatest_returns200() throws Exception {
        when(service.getLatest(any())).thenReturn(dto());

        mockMvc.perform(get("/api/measurements/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(80.0));
    }
}
