package com.school.canteen;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for full-stack integration tests.
 *
 * Runs against a real PostgreSQL container with the actual Flyway migrations applied, so
 * these tests exercise the same schema, constraints and row-locking behaviour as
 * production. An in-memory database would not do: the code depends on Postgres-only
 * features (ON CONFLICT DO NOTHING, FOR UPDATE SKIP LOCKED, LEAST(), partial unique
 * indexes), so a substitute could pass while production broke.
 *
 * The container is static and started once, then shared by every test class.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        // Deterministic secret so tests never depend on the developer's environment.
        registry.add("app.security.jwt.secret",
                () -> "test-secret-value-that-is-long-enough-for-hmac-sha");
        // Park the sweeper: these tests assert on the immediate delivery path, and a
        // background thread mutating outbox rows mid-assertion would make them flaky
        // rather than catching real bugs.
        registry.add("app.notification.sweep-interval-ms", () -> "3600000");
    }
}
