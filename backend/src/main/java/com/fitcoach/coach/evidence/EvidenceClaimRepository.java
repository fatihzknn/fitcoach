package com.fitcoach.coach.evidence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceClaimRepository extends JpaRepository<EvidenceClaim, UUID> {

    List<EvidenceClaim> findByDomainInAndConfidence(List<String> domains, String confidence, Pageable pageable);

    List<EvidenceClaim> findByDomainIn(List<String> domains, Pageable pageable);
}
