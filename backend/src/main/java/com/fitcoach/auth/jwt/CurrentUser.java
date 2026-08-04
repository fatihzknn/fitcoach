package com.fitcoach.auth.jwt;

import com.fitcoach.auth.Role;

import java.util.UUID;

/**
 * Authenticated principal placed in the security context by {@link JwtAuthenticationFilter}.
 * Inject into controllers with {@code @AuthenticationPrincipal CurrentUser}.
 */
public record CurrentUser(UUID id, String email, Role role) {
}
