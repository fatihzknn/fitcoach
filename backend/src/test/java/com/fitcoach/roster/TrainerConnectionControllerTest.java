package com.fitcoach.roster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.common.ForbiddenException;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.roster.dto.RedeemInviteRequest;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TrainerConnectionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TrainerConnectionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TrainerRosterService rosterService;
    @MockBean JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "client@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, List.of())
        );
    }

    @Test
    void redeem_returns204() throws Exception {
        doNothing().when(rosterService).redeemInviteCode(any(), any());

        mockMvc.perform(post("/api/trainer-connections/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemInviteRequest("ABCD2345"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void redeem_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/trainer-connections/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redeem_expiredOrUnknownCode_returns404() throws Exception {
        doThrow(new NotFoundException("Invite code not found or expired."))
                .when(rosterService).redeemInviteCode(any(), any());

        mockMvc.perform(post("/api/trainer-connections/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemInviteRequest("DEADCODE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void redeem_trainerRoleCaller_returns403() throws Exception {
        doThrow(new ForbiddenException("Trainer accounts can't connect to another trainer."))
                .when(rosterService).redeemInviteCode(any(), any());

        mockMvc.perform(post("/api/trainer-connections/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemInviteRequest("ABCD2345"))))
                .andExpect(status().isForbidden());
    }
}
