package com.fitcoach.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata + a bearer-JWT security scheme so the Swagger UI shows an
 * "Authorize" button. Swagger UI: {@code /swagger-ui.html}; raw spec: {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fitcoachOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FitCoach API")
                        .description("Personalized AI gym coach — REST API")
                        .version("0.1.0")
                        .license(new License().name("Proprietary")))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
