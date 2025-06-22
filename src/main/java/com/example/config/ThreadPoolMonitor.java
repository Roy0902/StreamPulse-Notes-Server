package com.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ThreadPoolMonitor {

    private final ThreadPoolTaskExecutor userRegistrationExecutor;
    private final ThreadPoolTaskExecutor otpExecutor;
    private final ThreadPoolTaskExecutor loginExecutor;
    private final ThreadPoolTaskExecutor userOperationsExecutor;

    public ThreadPoolMonitor(
            @Qualifier("userRegistrationExecutor") ThreadPoolTaskExecutor userRegistrationExecutor,
            @Qualifier("otpExecutor") ThreadPoolTaskExecutor otpExecutor,
            @Qualifier("loginExecutor") ThreadPoolTaskExecutor loginExecutor,
            @Qualifier("userOperationsExecutor") ThreadPoolTaskExecutor userOperationsExecutor) {
        this.userRegistrationExecutor = userRegistrationExecutor;
        this.otpExecutor = otpExecutor;
        this.loginExecutor = loginExecutor;
        this.userOperationsExecutor = userOperationsExecutor;
    }

    /**
     * Log thread pool metrics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logThreadPoolMetrics() {
        log.info("=== Thread Pool Metrics ===");
        logThreadPoolStatus("User Registration", userRegistrationExecutor);
        logThreadPoolStatus("OTP Operations", otpExecutor);
        logThreadPoolStatus("Login Operations", loginExecutor);
        logThreadPoolStatus("User Operations", userOperationsExecutor);
        log.info("==========================");
    }

    private void logThreadPoolStatus(String name, ThreadPoolTaskExecutor executor) {
        log.info("{} - Active: {}, Pool Size: {}, Core Pool Size: {}, Max Pool Size: {}, Queue Size: {}, Completed Tasks: {}",
                name,
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getThreadPoolExecutor().getQueue().size(),
                executor.getThreadPoolExecutor().getCompletedTaskCount());
    }

    /**
     * Get thread pool health status
     */
    public boolean isThreadPoolHealthy() {
        return userRegistrationExecutor.getActiveCount() < userRegistrationExecutor.getMaxPoolSize() &&
               otpExecutor.getActiveCount() < otpExecutor.getMaxPoolSize() &&
               loginExecutor.getActiveCount() < loginExecutor.getMaxPoolSize() &&
               userOperationsExecutor.getActiveCount() < userOperationsExecutor.getMaxPoolSize();
    }
} 