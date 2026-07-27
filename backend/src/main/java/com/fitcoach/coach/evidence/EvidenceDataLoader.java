package com.fitcoach.coach.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Loads real coaching claims extracted from trainer YouTube transcripts and research
 * papers (bundled as JSONL under {@code classpath:coach-data/}) into {@code evidence_claims}
 * on first boot. Runs once — skips silently if the table is already populated.
 *
 * Domains related to injury/pain diagnosis are deliberately excluded at load time so
 * they can never surface through the coach, regardless of future retrieval logic —
 * the product must never let the AI diagnose or give medical guidance (see CLAUDE.md).
 */
@Component
public class EvidenceDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EvidenceDataLoader.class);
    private static final Set<String> EXCLUDED_DOMAINS = Set.of("injury_safety", "safety");

    private final EvidenceClaimRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EvidenceDataLoader(EvidenceClaimRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (repository.count() > 0) {
            log.info("Evidence claims already loaded — skipping.");
            return;
        }

        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] files = resolver.getResources("classpath:coach-data/*.jsonl");

        List<EvidenceClaim> batch = new ArrayList<>();
        int skipped = 0;

        for (Resource file : files) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode card = objectMapper.readTree(line);
                    JsonNode meta = card.path("sourceMetadata");
                    String sourceId = card.path("sourceId").asText(null);
                    String creatorName = meta.path("creator_name").asText("Unknown");
                    String sourceType = meta.path("source_type").asText("unknown");
                    String sourceTitle = meta.hasNonNull("title") ? meta.get("title").asText() : null;
                    String sourceUrl = meta.hasNonNull("url") ? meta.get("url").asText() : null;

                    for (JsonNode claimNode : card.path("claims")) {
                        String domain = claimNode.path("domain").asText(null);
                        String claimText = claimNode.path("claim").asText(null);
                        if (domain == null || claimText == null || claimText.isBlank()) {
                            skipped++;
                            continue;
                        }
                        if (EXCLUDED_DOMAINS.contains(domain.toLowerCase())) {
                            skipped++;
                            continue;
                        }
                        String claimType = claimNode.path("claimType").asText("explicit");
                        String confidence = claimNode.path("confidence").asText("medium");
                        String quote = claimNode.hasNonNull("evidenceQuote")
                                ? claimNode.get("evidenceQuote").asText() : null;

                        batch.add(new EvidenceClaim(
                                sourceId, creatorName, sourceType, sourceTitle, sourceUrl,
                                domain, claimText, claimType, confidence, quote));
                    }
                }
            }
        }

        repository.saveAll(batch);
        log.info("Loaded {} evidence claims from {} files ({} skipped — missing fields or excluded domain).",
                batch.size(), files.length, skipped);
    }
}
