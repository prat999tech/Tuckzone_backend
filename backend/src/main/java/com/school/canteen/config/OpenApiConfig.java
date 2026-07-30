package com.school.canteen.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Generated API contract for the mobile client.
 *
 * Declaring the bearer scheme here means Swagger UI gets an "Authorize" button, so the
 * whole authenticated API can be exercised from a browser while building the Android app.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI canteenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TuckZone Canteen API")
                        .version("v1")
                        .description("""
                                Backend for the school canteen pre-order app.

                                Authentication: obtain tokens from /api/auth/login or
                                /api/auth/otp/login, then send `Authorization: Bearer <accessToken>`.
                                When a call returns 401, exchange the refresh token at
                                /api/auth/refresh rather than forcing the user to sign in again.

                                Ordering is idempotent: generate one idempotencyKey when the
                                checkout screen opens and reuse it for every retry of that
                                same order, otherwise a double tap creates two orders."""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
