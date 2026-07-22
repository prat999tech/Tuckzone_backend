package com.school.canteen.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness check. Public (permitted in SecurityConfig) so we can confirm the app is up
 * without a token. Later phases add richer role-protected endpoints alongside this.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "canteen",
                "time", Instant.now().toString());
    }
}
