package com.fitcoach.auth.jwt;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void generatesAndParsesToken() {
        User user = new User("rider@example.com", "hash", "Rider");

        String token = jwtService.generateToken(user);
        CurrentUser parsed = jwtService.parse(token);

        assertThat(parsed).isNotNull();
        assertThat(parsed.id()).isEqualTo(user.getId());
        assertThat(parsed.email()).isEqualTo("rider@example.com");
        assertThat(parsed.role()).isEqualTo(Role.USER);
    }

    @Test
    void generatesAndParsesTokenForTrainer() {
        User trainer = new User("coach@example.com", "hash", "Coach", Role.TRAINER);

        String token = jwtService.generateToken(trainer);
        CurrentUser parsed = jwtService.parse(token);

        assertThat(parsed).isNotNull();
        assertThat(parsed.role()).isEqualTo(Role.TRAINER);
    }

    @Test
    void returnsNullForGarbageToken() {
        assertThat(jwtService.parse("not-a-real-token")).isNull();
    }

    @Test
    void tokenMintedBeforeRoleClaimExisted_defaultsToUser() {
        // Simulates a token issued before the TRAINER role/role claim existed —
        // every such account genuinely was a USER, so a missing claim must default
        // to USER rather than reject the token (nobody should be logged out by
        // this change).
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String legacyToken = Jwts.builder()
                .subject(java.util.UUID.randomUUID().toString())
                .claim("email", "legacy@example.com")
                // deliberately no "role" claim
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(60, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();

        CurrentUser parsed = jwtService.parse(legacyToken);

        assertThat(parsed).isNotNull();
        assertThat(parsed.email()).isEqualTo("legacy@example.com");
        assertThat(parsed.role()).isEqualTo(Role.USER);
    }
}
