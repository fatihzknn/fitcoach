package com.fitcoach.health;

import com.fitcoach.health.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Public health endpoint used by the frontend to verify backend connectivity.
 * Separate from Spring Actuator's {@code /actuator/health}, which is for operations.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Service availability")
public class HealthController {

    private final String version;

    public HealthController(@Value("${app.version:0.1.0}") String version) {
        this.version = version;
    }

    @GetMapping
    @Operation(summary = "Liveness/connectivity check for the web client")
    public HealthResponse health() {
        return new HealthResponse("UP", "fitcoach-backend", version, Instant.now());
    }
}
