package com.fitcoach.coach;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.coach.dto.ChatMessageDto;
import com.fitcoach.coach.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coach")
@Tag(name = "Coach", description = "AI coach chat")
@SecurityRequirement(name = "bearer-jwt")
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Send a message to the AI coach; returns the user message + assistant reply")
    public List<ChatMessageDto> chat(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SendMessageRequest request) {
        return coachService.sendMessage(currentUser, request.message());
    }

    @GetMapping("/history")
    @Operation(summary = "Get the full chat history for the current user")
    public List<ChatMessageDto> history(@AuthenticationPrincipal CurrentUser currentUser) {
        return coachService.getHistory(currentUser);
    }
}
