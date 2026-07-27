package com.fitcoach.coach.evidence;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A single, citable coaching claim extracted from real trainer content (YouTube
 * transcripts, research papers), paired with the quote and source it came from.
 * Used to ground AI coach responses in real material instead of generic output.
 */
@Entity
@Table(name = "evidence_claims")
public class EvidenceClaim extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private String sourceId;

    @Column(name = "creator_name", nullable = false)
    private String creatorName;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_title", columnDefinition = "TEXT")
    private String sourceTitle;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "domain", nullable = false)
    private String domain;

    @Column(name = "claim", nullable = false, columnDefinition = "TEXT")
    private String claim;

    @Column(name = "claim_type", nullable = false)
    private String claimType;

    @Column(name = "confidence", nullable = false)
    private String confidence;

    @Column(name = "evidence_quote", columnDefinition = "TEXT")
    private String evidenceQuote;

    protected EvidenceClaim() {}

    public EvidenceClaim(String sourceId, String creatorName, String sourceType,
                          String sourceTitle, String sourceUrl, String domain,
                          String claim, String claimType, String confidence,
                          String evidenceQuote) {
        this.id = UUID.randomUUID();
        this.sourceId = sourceId;
        this.creatorName = creatorName;
        this.sourceType = sourceType;
        this.sourceTitle = sourceTitle;
        this.sourceUrl = sourceUrl;
        this.domain = domain;
        this.claim = claim;
        this.claimType = claimType;
        this.confidence = confidence;
        this.evidenceQuote = evidenceQuote;
    }

    public UUID getId() { return id; }
    public String getSourceId() { return sourceId; }
    public String getCreatorName() { return creatorName; }
    public String getSourceType() { return sourceType; }
    public String getSourceTitle() { return sourceTitle; }
    public String getSourceUrl() { return sourceUrl; }
    public String getDomain() { return domain; }
    public String getClaim() { return claim; }
    public String getClaimType() { return claimType; }
    public String getConfidence() { return confidence; }
    public String getEvidenceQuote() { return evidenceQuote; }
}
