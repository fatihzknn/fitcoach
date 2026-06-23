package com.fitcoach.health.dto;

import java.time.Instant;

/**
 * Public health payload consumed by the frontend connectivity check.
 *
 * @param status    "UP" when the service is serving requests
 * @param service   service identifier
 * @param version   application version
 * @param timestamp server time when the response was produced
 */
public record HealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp
) {}
