package com.fitcoach.roster;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.common.ForbiddenException;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.roster.dto.ClientSummaryDto;
import com.fitcoach.roster.dto.TrainerInviteDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TrainerRosterController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TrainerRosterControllerTest {

    @Autowired MockMvc mockMvc;
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
}
