package com.fitcoach.coach.evidence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs against the real bundled JSONL files under src/main/resources/coach-data —
 * this is the safety-critical check that injury/pain claims never make it into the
 * table the coach reads from (see CLAUDE.md: AI must never diagnose or give medical
 * guidance).
 */
@ExtendWith(MockitoExtension.class)
class EvidenceDataLoaderTest {

    @Mock
    private EvidenceClaimRepository repository;

    @Test
    void skipsLoadingWhenAlreadyPopulated() throws Exception {
        when(repository.count()).thenReturn(5L);

        new EvidenceDataLoader(repository).run(null);

        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.<List<EvidenceClaim>>any());
    }

    @Test
    void loadsRealBundledFilesAndExcludesInjuryAndSafetyDomains() throws Exception {
        when(repository.count()).thenReturn(0L);

        new EvidenceDataLoader(repository).run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EvidenceClaim>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<EvidenceClaim> loaded = captor.getValue();
        assertThat(loaded).isNotEmpty();

        Set<String> excluded = Set.of("injury_safety", "safety");
        assertThat(loaded)
                .extracting(EvidenceClaim::getDomain)
                .noneMatch(domain -> excluded.contains(domain.toLowerCase()));

        assertThat(loaded).allSatisfy(c -> {
            assertThat(c.getClaim()).isNotBlank();
            assertThat(c.getDomain()).isNotBlank();
            assertThat(c.getCreatorName()).isNotBlank();
        });
    }
}
