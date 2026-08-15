package com.fitcoach.roster;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.roster.dto.RedeemInviteRequest;
import com.fitcoach.roster.dto.SendTrainerMessageRequest;
import com.fitcoach.roster.dto.TrainerConnectionSummaryDto;
import com.fitcoach.roster.dto.TrainerMessageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Client-facing side of the trainer connection — deliberately not nested under
 * /api/trainer/..., which reads as panel-only.
 */
@RestController
@RequestMapping("/api/trainer-connections")
@Tag(name = "Trainer Connections", description = "A client linking themselves to a trainer via invite code")
@SecurityRequirement(name = "bearer-jwt")
public class TrainerConnectionController {

    private final TrainerRosterService rosterService;

    public TrainerConnectionController(TrainerRosterService rosterService) {
        this.rosterService = rosterService;
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem a trainer's invite code, linking the current account as their client")
    public ResponseEntity<Void> redeem(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody RedeemInviteRequest request) {
        rosterService.redeemInviteCode(currentUser, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List the trainer(s) this client account is linked to")
    public List<TrainerConnectionSummaryDto> listMyTrainers(@AuthenticationPrincipal CurrentUser currentUser) {
        return rosterService.listMyTrainers(currentUser);
    }

    @GetMapping("/{trainerId}/messages")
    @Operation(summary = "Get the message thread with a linked trainer")
    public List<TrainerMessageDto> getMessages(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID trainerId) {
        return rosterService.getMessagesWithTrainer(currentUser, trainerId);
    }

    @PostMapping("/{trainerId}/messages")
    @Operation(summary = "Send a message to a linked trainer")
    public TrainerMessageDto sendMessage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID trainerId,
            @Valid @RequestBody SendTrainerMessageRequest request) {
        return rosterService.sendMessageToTrainer(currentUser, trainerId, request);
    }
}
