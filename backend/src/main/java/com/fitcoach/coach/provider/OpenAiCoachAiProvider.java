package com.fitcoach.coach.provider;

import com.fitcoach.coach.CoachContext;
import com.fitcoach.coach.CoachPrinciple;
import com.fitcoach.coach.ChatMessage;
import com.fitcoach.coach.domain.MessageRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Skeleton for a real OpenAI-backed coach. Activates only when app.openai.api-key
 * is set in config — MockCoachAiProvider is used otherwise.
 *
 * TODO: replace buildPrompt() call with an actual OpenAI Chat Completion request
 *       using the official openai-java SDK or a plain HTTP client.
 */
@Component("openAiCoachAiProvider")
@ConditionalOnProperty(name = "app.openai.api-key")
public class OpenAiCoachAiProvider implements CoachAiProvider {

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String model;

    @Override
    public String generateResponse(String userMessage, CoachContext context) {
        String systemPrompt = buildSystemPrompt(context);
        // TODO: POST to https://api.openai.com/v1/chat/completions
        //       with model, messages (system + history + user), max_tokens=600
        throw new UnsupportedOperationException("OpenAI provider not yet implemented. Set app.openai.api-key and implement the HTTP call.");
    }

    private String buildSystemPrompt(CoachContext ctx) {
        String principles = ctx.principles().stream()
                .map(p -> "### " + p.getTitle() + "\n" + p.getContent())
                .collect(Collectors.joining("\n\n"));

        String history = ctx.recentHistory().stream()
                .map(m -> (m.getRole() == MessageRole.USER ? "User: " : "Coach: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        return """
                You are a friendly, knowledgeable personal fitness coach. You must never diagnose injuries,
                prescribe medical treatment, recommend aggressive weight loss, suggest excessive training
                volume, or tell users to push through pain.

                User profile:
                - Name: %s
                - Goal: %s
                - Background: %s
                - Training days/week: %d
                - Pain areas: %s
                - Active plan: %s
                - Recent sessions: %s

                Coach principles you follow:
                %s

                Recent conversation:
                %s

                Respond in plain text (no markdown). Be concise, warm, and practical.
                """.formatted(
                ctx.userName(),
                ctx.mainGoal(),
                ctx.trainingBackground(),
                ctx.trainingDaysPerWeek(),
                String.join(", ", ctx.painAreas()),
                ctx.activePlanName() != null ? ctx.activePlanName() : "none",
                String.join("; ", ctx.recentSessionSummaries()),
                principles,
                history
        );
    }
}
