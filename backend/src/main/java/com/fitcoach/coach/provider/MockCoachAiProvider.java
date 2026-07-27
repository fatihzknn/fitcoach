package com.fitcoach.coach.provider;

import com.fitcoach.coach.CoachContext;
import com.fitcoach.coach.CoachPrinciple;
import com.fitcoach.coach.evidence.EvidenceCitationService;
import com.fitcoach.coach.evidence.EvidenceClaim;
import com.fitcoach.coach.evidence.EvidenceTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Rule-based coach that responds to keywords. Active when no real AI provider bean
 * is present (i.e. always in development until OpenAI key is wired).
 *
 * Responses are grounded with real, cited claims from EvidenceCitationService where
 * safe to do so (recovery, technique, volume, strength, muscle, nutrition, progress,
 * motivation). Pain guidance is deliberately never enriched with retrieved data —
 * it stays a fixed, reviewed safety response.
 */
@Component
@Primary
@ConditionalOnMissingBean(name = "openAiCoachAiProvider")
public class MockCoachAiProvider implements CoachAiProvider {

    private final EvidenceCitationService citationService;

    public MockCoachAiProvider(EvidenceCitationService citationService) {
        this.citationService = citationService;
    }

    @Override
    public String generateResponse(String userMessage, CoachContext ctx) {
        String lower = userMessage.toLowerCase();

        // Safety first — pain/injury keywords take highest priority
        if (containsAny(lower, "pain", "hurt", "injury", "injured", "ache", "crack", "pop")) {
            return painResponse(ctx);
        }

        // Recovery & rest
        if (containsAny(lower, "recovery", "rest day", "sleep", "tired", "fatigue", "sore", "soreness", "deload")) {
            return recoveryResponse(ctx);
        }

        // Motivation & consistency
        if (containsAny(lower, "motivat", "skip", "miss", "quit", "hard", "difficult", "struggle", "give up", "consistent")) {
            return motivationResponse(ctx);
        }

        // Technique & form
        if (containsAny(lower, "form", "technique", "how to", "how do i", "correct", "posture")) {
            return techniqueResponse(ctx);
        }

        // Volume & frequency
        if (containsAny(lower, "volume", "sets", "reps", "how many", "frequency", "how often")) {
            return volumeResponse(ctx);
        }

        // Goal-specific: strength
        if (containsAny(lower, "strength", "strong", "heavy", "max", "1rm", "powerl")) {
            return strengthResponse(ctx);
        }

        // Goal-specific: muscle gain
        if (containsAny(lower, "muscle", "bulk", "hypertrophy", "size", "gain", "protein")) {
            return muscleResponse(ctx);
        }

        // Goal-specific: fat loss
        if (containsAny(lower, "fat", "weight loss", "lose weight", "calorie", "cardio", "cut")) {
            return fatLossResponse(ctx);
        }

        // Plateau / progress
        if (containsAny(lower, "plateau", "stuck", "not improving", "no progress", "progress")) {
            return progressResponse(ctx);
        }

        // Greeting / intro
        if (containsAny(lower, "hello", "hi", "hey", "start", "begin", "new", "advice")) {
            return greetingResponse(ctx);
        }

        return defaultResponse(ctx);
    }

    // ─── Response builders ────────────────────────────────────────────────────

    private String painResponse(CoachContext ctx) {
        return "I want to take this seriously, " + ctx.userName() + ". " +
                "Pain during or after training is your body's signal to stop and pay attention.\n\n" +
                "There's an important difference between muscle soreness — which is dull and diffuse " +
                "and peaks 24–48 hours after training — and joint or tendon pain, which tends to be " +
                "sharp, localized, and occurs during the movement itself.\n\n" +
                "If what you're feeling is the second kind, I'd strongly recommend:\n" +
                "1. Stop the exercise that causes it\n" +
                "2. Try an alternative exercise that targets the same muscle without discomfort\n" +
                "3. If the pain persists or is severe, consult a healthcare professional\n\n" +
                "I can help you find alternative exercises for any movement that's causing pain. " +
                "What would you like to swap out? And please — never push through sharp or worsening pain.";
    }

    private String recoveryResponse(CoachContext ctx) {
        CoachPrinciple p = findPrinciple(ctx, "RECOVERY", "DELOAD");
        String base = "Recovery is where the actual adaptation happens, " + ctx.userName() + ". " +
                "Training is just the stimulus.\n\n";
        if (p != null) {
            base += p.getContent() + "\n\n";
        }
        base += "With " + ctx.trainingDaysPerWeek() + " sessions per week, you have " +
                (7 - ctx.trainingDaysPerWeek()) + " recovery days built in. " +
                "Make sure you're sleeping 7–9 hours — that's when the muscle repair actually occurs.";
        return base + citeQuote(EvidenceTopic.RECOVERY);
    }

    private String motivationResponse(CoachContext ctx) {
        CoachPrinciple p = findPrinciple(ctx, "CONSISTENCY", "MOTIVATION");
        String base = "Here's the truth about motivation, " + ctx.userName() + ": " +
                "it comes and goes. Discipline is what gets you there on the days you don't feel like it.\n\n";
        if (p != null) {
            base += p.getContent() + "\n\n";
        }
        if (!ctx.recentSessionSummaries().isEmpty()) {
            base += "You've been showing up — " + ctx.recentSessionSummaries().size() +
                    " recent sessions in the books. That's the work. Keep building that streak.";
        } else {
            base += "Every expert was once a beginner who just decided to start. " +
                    "Your plan is ready — the only thing left is to begin.";
        }
        return base + citeQuote(EvidenceTopic.MOTIVATION);
    }

    private String techniqueResponse(CoachContext ctx) {
        CoachPrinciple p = findPrinciple(ctx, "EXERCISE_TECHNIQUE");
        String base = "Great question — technique is the foundation everything else is built on.\n\n";
        if (p != null) {
            base += p.getContent() + "\n\n";
        }
        base += "For any specific exercise in your " + (ctx.activePlanName() != null ? ctx.activePlanName() : "plan") +
                ", I've included form cues and common mistakes in the exercise detail. " +
                "Could you tell me which exercise you want help with? I can give you more targeted coaching.";
        return base + citeQuote(EvidenceTopic.TECHNIQUE);
    }

    private String volumeResponse(CoachContext ctx) {
        CoachPrinciple p = findPrinciple(ctx, "TRAINING_VOLUME");
        String base = "Volume is one of the most important — and most misunderstood — training variables.\n\n";
        if (p != null) {
            base += p.getContent() + "\n\n";
        }
        base += "Your current plan is built specifically for your " + formatBackground(ctx.trainingBackground()) +
                " level and " + ctx.trainingDaysPerWeek() + " training days. " +
                "It lands in the right volume range for your experience. " +
                "If you want to add more, start by adding one extra set per muscle group per week — not per session.";
        return base + citeQuote(EvidenceTopic.VOLUME);
    }

    private String strengthResponse(CoachContext ctx) {
        return "Building strength is about progressive overload with heavy compound movements.\n\n" +
                "The core principle: track your weights, and when you hit the top of your rep range " +
                "with 2 reps in reserve across all sets, add the smallest possible weight increment next session.\n\n" +
                "For " + ctx.userName() + " with your background, the key lifts to focus on are the " +
                "compound movements in your plan — they give you the most strength return per session. " +
                "Isolation work supports them but shouldn't replace them.\n\n" +
                "Are you hitting a specific lift you want to improve? Tell me more." +
                citeQuote(EvidenceTopic.STRENGTH);
    }

    private String muscleResponse(CoachContext ctx) {
        return "For muscle gain, three things matter most: progressive overload, adequate protein, and enough volume.\n\n" +
                "Progressive overload: add reps or weight regularly — your plan's RIR guidance helps you know when to push.\n\n" +
                "Protein: aim for 1.6–2.2g per kg of bodyweight daily. This is the range research consistently supports for hypertrophy.\n\n" +
                "Volume: 10–20 sets per muscle group per week. Your " + ctx.trainingDaysPerWeek() +
                "-day plan is designed to hit this for your main muscle groups.\n\n" +
                "Muscle gain is slow — expect 0.5–1kg per month in ideal conditions as a " +
                formatBackground(ctx.trainingBackground()) + ". Don't let the scale fool you; take progress photos and track strength." +
                citeQuote(EvidenceTopic.MUSCLE);
    }

    private String fatLossResponse(CoachContext ctx) {
        return "For fat loss, training is the tool — but energy balance is the driver.\n\n" +
                "Your " + ctx.trainingDaysPerWeek() + " weekly sessions will preserve and build muscle while you're in a calorie deficit, " +
                "which is critical. Muscle is metabolically active and shapes how you look as the fat comes off.\n\n" +
                "A few evidence-based points:\n" +
                "• A modest deficit (300–500 kcal/day) is more sustainable than aggressive restriction\n" +
                "• Keep protein high (1.8–2.2g/kg) to protect muscle during a cut\n" +
                "• Don't add excessive cardio on top of your programme straight away — it adds fatigue without proportional return\n\n" +
                "Keep showing up, log your sessions, and the compound effect will do the work." +
                citeQuote(EvidenceTopic.NUTRITION);
    }

    private String progressResponse(CoachContext ctx) {
        CoachPrinciple p = findPrinciple(ctx, "PROGRESSIVE_OVERLOAD");
        String base = "Plateaus are normal and temporary, " + ctx.userName() + ". " +
                "Here's what usually causes them and how to address each:\n\n" +
                "1. **Not tracking weights** — if you're not recording what you lifted last session, you can't progressively overload\n" +
                "2. **Insufficient recovery** — poor sleep or too little rest kills adaptation\n" +
                "3. **Too much too soon** — jumping weight too fast stalls progress faster\n" +
                "4. **Need for a deload** — after 6–8 weeks of hard training, a light week resets your capacity\n\n";
        if (p != null) {
            base += p.getContent();
        }
        return base + citeQuote(EvidenceTopic.PROGRESS);
    }

    private String greetingResponse(CoachContext ctx) {
        String goal = formatGoal(ctx.mainGoal());
        String plan = ctx.activePlanName() != null ? ctx.activePlanName() : "your plan";
        return "Hey " + ctx.userName() + "! I'm your FitCoach AI — here to help you train smarter.\n\n" +
                "I know you're training for " + goal + " with " + ctx.trainingDaysPerWeek() +
                " sessions per week on " + plan + ".\n\n" +
                "I can help you with:\n" +
                "• Technique tips for specific exercises\n" +
                "• Understanding progressive overload\n" +
                "• Recovery and deload guidance\n" +
                "• Staying consistent when motivation dips\n" +
                "• Goal-specific advice for " + goal + "\n\n" +
                "What's on your mind?";
    }

    private String defaultResponse(CoachContext ctx) {
        CoachPrinciple p = ctx.principles().isEmpty() ? null :
                ctx.principles().get((int) (System.currentTimeMillis() % ctx.principles().size()));
        String base = "Good question, " + ctx.userName() + ". Let me give you something useful.\n\n";
        if (p != null) {
            base += "**" + p.getTitle() + "**\n" + p.getContent() + "\n\n";
        }
        base += "Is there a more specific aspect of your training you'd like to dig into?";
        return base;
    }

    // ─── Evidence citation ──────────────────────────────────────────────────────

    /** Appends a short, cited quote from real trainer content, or "" if none found. */
    private String citeQuote(EvidenceTopic topic) {
        List<EvidenceClaim> found = citationService.findOne(topic);
        if (found.isEmpty()) return "";
        EvidenceClaim c = found.get(0);
        String quote = c.getEvidenceQuote() != null ? c.getEvidenceQuote() : c.getClaim();
        return "\n\nOne more thing, backed by real coaching content — " + c.getCreatorName() + ": \"" + quote + "\"";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    private static CoachPrinciple findPrinciple(CoachContext ctx, String... keys) {
        List<String> keyList = Arrays.asList(keys);
        return ctx.principles().stream()
                .filter(p -> keyList.contains(p.getKey()))
                .findFirst()
                .orElse(null);
    }

    private static String formatGoal(String goal) {
        if (goal == null) return "your goal";
        return switch (goal) {
            case "FAT_LOSS"        -> "fat loss";
            case "MUSCLE_GAIN"     -> "muscle gain";
            case "STRENGTH"        -> "strength";
            case "GENERAL_FITNESS" -> "general fitness";
            default                -> goal.toLowerCase().replace('_', ' ');
        };
    }

    private static String formatBackground(String bg) {
        if (bg == null) return "your level";
        return switch (bg) {
            case "STARTING"   -> "beginner";
            case "RETURNING"  -> "returning lifter";
            case "REGULAR"    -> "intermediate lifter";
            default           -> bg.toLowerCase();
        };
    }
}
