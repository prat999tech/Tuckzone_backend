package com.school.canteen.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Thread pool for notification delivery.
 *
 * A bare {@code @Async} would use Spring's default executor, which starts a brand-new
 * thread for every call. Five hundred orders placed in the same minute would then mean
 * five hundred threads all blocked on a network call to FCM — enough to destabilise the
 * JVM. This pool caps concurrent senders and queues the rest.
 *
 * CallerRunsPolicy is the deliberate choice for the overflow case: rather than discarding
 * a notification, the submitting thread performs the send itself, which also applies
 * natural back-pressure. Anything that still cannot be sent stays PENDING in the outbox
 * and the scheduler picks it up, so nothing is ever lost.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String NOTIFICATION_EXECUTOR = "notificationExecutor";

    @Bean(name = NOTIFICATION_EXECUTOR)
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Let in-flight sends finish on shutdown instead of losing them mid-flight.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
