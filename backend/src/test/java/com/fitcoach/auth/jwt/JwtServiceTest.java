package com.fitcoach.auth.jwt;

import com.fitcoach.auth.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("test-secret-test-secret-test-secret-123456", 60);

    @Test
    void generatesAndParsesToken() {
        User user = new User("rider@example.com", "hash", "Rider");

        String token = jwtService.generateToken(user);
        CurrentUser parsed = jwtService.parse(token);

        assertThat(parsed).isNotNull();
        assertThat(parsed.id()).isEqualTo(user.getId());
        assertThat(parsed.email()).isEqualTo("rider@example.com");
    }

    @Test
    void returnsNullForGarbageToken() {
        assertThat(jwtService.parse("not-a-real-token")).isNull();
    }
}
