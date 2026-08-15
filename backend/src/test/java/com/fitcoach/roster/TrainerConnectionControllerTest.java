package com.fitcoach.roster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.auth.jwt.JwtService;
import com.fitcoach.common.ForbiddenException;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.roster.dto.RedeemInviteRequest;
import com.fitcoach.roster.dto.SendTrainerMessageRequest;
import com.fitcoach.roster.dto.TrainerConnectionSummaryDto;
import com.fitcoach.roster.dto.TrainerMessageDto;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TrainerConnectionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TrainerConnectionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TrainerRosterService rosterService;
    @MockBean JwtService jwtService;

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final CurrentUser CLIENT = new CurrentUser(CLIENT_ID, "client@example.com", Role.USER);

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CLIENT, null, List.of())
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

    @Test
    void listMyTrainers_returns200WithList() throws Exception {
        TrainerConnectionSummaryDto trainer = new TrainerConnectionSummaryDto(
                UUID.randomUUID(), "Coach Sam", "coach@example.com", Instant.now());
        when(rosterService.listMyTrainers(any())).thenReturn(List.of(trainer));

        mockMvc.perform(get("/api/trainer-connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Coach Sam"));
    }

    @Test
    void getMessages_returns200() throws Exception {
        TrainerMessageDto message = new TrainerMessageDto(UUID.randomUUID(), "Hi coach", Instant.now(), true);
        when(rosterService.getMessagesWithTrainer(any(), any())).thenReturn(List.of(message));

        mockMvc.perform(get("/api/trainer-connections/" + UUID.randomUUID() + "/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hi coach"));
    }

    @Test
    void sendMessage_returns200WithDto() throws Exception {
        TrainerMessageDto saved = new TrainerMessageDto(UUID.randomUUID(), "Question about my plan", Instant.now(), true);
        when(rosterService.sendMessageToTrainer(any(), any(), any())).thenReturn(saved);

        mockMvc.perform(post("/api/trainer-connections/" + UUID.randomUUID() + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendTrainerMessageRequest("Question about my plan"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Question about my plan"));
    }

    @Test
    void sendMessage_returns403ForTrainerRoleCaller() throws Exception {
        when(rosterService.sendMessageToTrainer(any(), any(), any()))
                .thenThrow(new ForbiddenException("Trainer accounts don't have their own trainer connections."));

        mockMvc.perform(post("/api/trainer-connections/" + UUID.randomUUID() + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendTrainerMessageRequest("hi"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMessages_returns404ForUnlinkedTrainer() throws Exception {
        when(rosterService.getMessagesWithTrainer(any(), any()))
                .thenThrow(new NotFoundException("Trainer not found."));

        mockMvc.perform(get("/api/trainer-connections/" + UUID.randomUUID() + "/messages"))
                .andExpect(status().isNotFound());
    }
}
