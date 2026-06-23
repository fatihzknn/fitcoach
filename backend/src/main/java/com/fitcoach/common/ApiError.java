package com.fitcoach.common;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body returned by the API. Keeps responses predictable for the frontend
 * and avoids leaking stack traces or internal entity shapes.
 *
 * @param timestamp when the error was produced
 * @param status    HTTP status code
 * @param error     short machine-ish label (e.g. "Validation Failed")
 * @param message   user-friendly, human-readable explanation
 * @param path      request path that failed
 * @param details   optional field-level messages (validation)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldDetail> details
) {
    public record FieldDetail(String field, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldDetail> details) {
        return new ApiError(Instant.now(), status, error, message, path, details);
    }
}
