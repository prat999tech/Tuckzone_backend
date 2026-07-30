package com.school.canteen.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies a {@link Clock} fixed to the school's timezone.
 *
 * Services must resolve "today" and cutoff comparisons through this clock instead of
 * calling {@code LocalDate.now()} / {@code LocalDateTime.now()} directly: those use the
 * JVM default zone, which is UTC on hosts like Render. With a 5:30 offset that silently
 * shifts every order cutoff and flips "today's menu" over at the wrong moment.
 *
 * Injecting a Clock also makes time-dependent logic testable.
 */
@Configuration
public class ClockConfig {

    @Bean
    public ZoneId appZoneId(AppProperties appProperties) {
        return ZoneId.of(appProperties.timezone());
    }

    @Bean
    public Clock clock(ZoneId appZoneId) {
        return Clock.system(appZoneId);
    }
}
