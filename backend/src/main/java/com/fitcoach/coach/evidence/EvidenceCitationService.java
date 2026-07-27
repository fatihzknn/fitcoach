package com.fitcoach.coach.evidence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retrieves real, sourced coaching claims for a given topic so responses can be
 * grounded in actual trainer content instead of generic text. Prefers high-confidence
 * claims; falls back to any confidence if none are found for the topic.
 */
@Service
public class EvidenceCitationService {

    private final EvidenceClaimRepository repository;

    public EvidenceCitationService(EvidenceClaimRepository repository) {
        this.repository = repository;
    }

    public List<EvidenceClaim> findFor(EvidenceTopic topic, int limit) {
        Pageable page = PageRequest.of(0, limit);
        List<EvidenceClaim> highConfidence = repository.findByDomainInAndConfidence(topic.domains(), "high", page);
        if (!highConfidence.isEmpty()) return highConfidence;
        return repository.findByDomainIn(topic.domains(), page);
    }

    /** Returns one claim for the topic, picked at random from a small candidate pool for variety. */
    public List<EvidenceClaim> findOne(EvidenceTopic topic) {
        List<EvidenceClaim> pool = findFor(topic, 8);
        if (pool.isEmpty()) return Collections.emptyList();
        return List.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }
}
