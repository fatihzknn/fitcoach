package com.fitcoach.coach.evidence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceCitationServiceTest {

    @Mock
    private EvidenceClaimRepository repository;

    @InjectMocks
    private EvidenceCitationService service;

    private EvidenceClaim claim(String domain, String confidence, String creator) {
        return new EvidenceClaim("src1", creator, "youtube", "title", "url",
                domain, "some claim", "explicit", confidence, "some quote");
    }

    @Test
    void findFor_prefersHighConfidenceWhenAvailable() {
        EvidenceClaim high = claim("recovery", "high", "Jeff Nippard");
        when(repository.findByDomainInAndConfidence(eq(EvidenceTopic.RECOVERY.domains()), eq("high"), any()))
                .thenReturn(List.of(high));

        List<EvidenceClaim> result = service.findFor(EvidenceTopic.RECOVERY, 3);

        assertThat(result).containsExactly(high);
    }

    @Test
    void findFor_fallsBackToAnyConfidenceWhenNoHighConfidenceFound() {
        when(repository.findByDomainInAndConfidence(eq(EvidenceTopic.RECOVERY.domains()), eq("high"), any()))
                .thenReturn(List.of());
        EvidenceClaim medium = claim("recovery", "medium", "Renaissance Periodization");
        when(repository.findByDomainIn(eq(EvidenceTopic.RECOVERY.domains()), any()))
                .thenReturn(List.of(medium));

        List<EvidenceClaim> result = service.findFor(EvidenceTopic.RECOVERY, 3);

        assertThat(result).containsExactly(medium);
    }

    @Test
    void findOne_returnsEmptyWhenNothingFound() {
        when(repository.findByDomainInAndConfidence(anyList(), eq("high"), any())).thenReturn(List.of());
        when(repository.findByDomainIn(anyList(), any())).thenReturn(List.of());

        assertThat(service.findOne(EvidenceTopic.MUSCLE)).isEmpty();
    }

    @Test
    void findOne_returnsSingleClaimFromPool() {
        EvidenceClaim a = claim("training_volume", "high", "Jeff Nippard");
        EvidenceClaim b = claim("training_volume", "high", "Renaissance Periodization");
        when(repository.findByDomainInAndConfidence(eq(EvidenceTopic.VOLUME.domains()), eq("high"), any()))
                .thenReturn(List.of(a, b));

        List<EvidenceClaim> result = service.findOne(EvidenceTopic.VOLUME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isIn(a, b);
    }

    @Test
    void findFor_queriesRepositoryWithTopicDomains() {
        when(repository.findByDomainInAndConfidence(any(), eq("high"), any())).thenReturn(List.of());
        when(repository.findByDomainIn(any(), any())).thenReturn(List.of());

        service.findFor(EvidenceTopic.STRENGTH, 5);

        verify(repository).findByDomainInAndConfidence(eq(EvidenceTopic.STRENGTH.domains()), eq("high"), any());
    }
}
