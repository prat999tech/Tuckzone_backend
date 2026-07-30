package com.school.canteen.security;

import com.school.canteen.exception.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Fixed-window counter guarding credential and OTP endpoints against brute force.
 *
 * Deliberately in-memory and dependency-free: the app runs as a single instance, so a
 * shared store would be extra infrastructure for no gain. If it is ever scaled to multiple
 * instances this must move to Redis, otherwise each instance enforces its own quota.
 *
 * Without this, a 6-digit OTP is guessable in seconds and passwords can be attacked at
 * network speed.
 */
@Service
public class RateLimiterService {

    private record Counter(Instant windowStart, AtomicInteger hits) {
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /**
     * Records one attempt against {@code key} and throws once the limit is exceeded.
     *
     * @param key      identifies the caller + action, e.g. "login:user@example.com"
     * @param limit    maximum attempts allowed inside the window
     * @param window   length of the window
     * @param message  user-facing message when the limit trips
     */
    public void checkAndConsume(String key, int limit, Duration window, String message) {
        Instant now = Instant.now();

        Counter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.windowStart().plus(window).isBefore(now)) {
                return new Counter(now, new AtomicInteger(0)); // start a fresh window
            }
            return existing;
        });

        if (counter.hits().incrementAndGet() > limit) {
            throw new TooManyRequestsException(message);
        }
    }

    /** Clears the counter after a successful attempt so honest users are not penalised. */
    public void reset(String key) {
        counters.remove(key);
    }

    /** Drops windows that can no longer be active, keeping the map bounded. */
    @Scheduled(fixedDelay = 600_000L)
    public void evictStaleCounters() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        counters.entrySet().removeIf(entry -> entry.getValue().windowStart().isBefore(cutoff));
    }
}
