package com.fitcoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FitCoach backend entrypoint.
 *
 * <p>Modular monolith: feature packages (health, and from later phases auth, onboarding,
 * workout, progress, coach) live side by side under {@code com.fitcoach} and share one
 * deployable unit and one database.</p>
 */
@SpringBootApplication
public class FitcoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitcoachApplication.class, args);
    }
}
