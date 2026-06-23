package com.fitcoach.auth.jwt;

import java.util.UUID;

/**
 * Authenticated principal placed in the security context by {@link JwtAuthenticationFilter}.
 * Inject into controllers with {@code @AuthenticationPrincipal CurrentUser}.
 */
public record CurrentUser(UUID id, String email) {
}
