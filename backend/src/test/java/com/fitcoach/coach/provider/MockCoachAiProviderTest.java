package com.fitcoach.coach.provider;

import com.fitcoach.coach.CoachContext;
import com.fitcoach.coach.evidence.EvidenceCitationService;
import com.fitcoach.coach.evidence.EvidenceClaim;
import com.fitcoach.coach.evidence.EvidenceTopic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockCoachAiProviderTest {

    @Mock
    private EvidenceCitationService citationService;

    private MockCoachAiProvider provider;

    private CoachContext context() {
        return new CoachContext(
                "Alex", "MUSCLE_GAIN", "REGULAR", 4,
                List.of(), "Upper / Lower", List.of(), List.of(), List.of());
    }

    private EvidenceClaim claim(String creator, String quote) {
        return new EvidenceClaim("src", creator, "youtube", "title", "url",
                "recovery", "some claim", "explicit", "high", quote);
    }

    @Test
    void painResponse_neverConsultsEvidenceService() {
        provider = new MockCoachAiProvider(citationService);

        String response = provider.generateResponse("my knee hurts during squats", context());

        assertThat(response).doesNotContain("backed by real coaching content");
        verify(citationService, never()).findOne(any());
    }

    @Test
    void recoveryResponse_appendsCitationWhenAvailable() {
        when(citationService.findOne(eq(EvidenceTopic.RECOVERY)))
                .thenReturn(List.of(claim("Renaissance Periodization", "sleep is when adaptation happens")));
        provider = new MockCoachAiProvider(citationService);

        String response = provider.generateResponse("how important is sleep for recovery?", context());

        assertThat(response).contains("Renaissance Periodization");
        assertThat(response).contains("sleep is when adaptation happens");
    }

    @Test
    void recoveryResponse_omitsCitationWhenNoneFound() {
        when(citationService.findOne(eq(EvidenceTopic.RECOVERY))).thenReturn(List.of());
        provider = new MockCoachAiProvider(citationService);

        String response = provider.generateResponse("I need a rest day", context());

        assertThat(response).doesNotContain("backed by real coaching content");
    }
}
