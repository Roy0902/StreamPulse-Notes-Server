package com.example.config.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class ThreadPoolMonitor {

    private final ExecutorService userRegistrationThreadPool;
    private final ExecutorService otpThreadPool;
    private final ExecutorService loginThreadPool;
    private final ExecutorService userOperationsThreadPool;
    private final ExecutorService emailServiceThreadPool;

    public ThreadPoolMonitor(
            @Qualifier("userRegistrationThreadPool") ExecutorService userRegistrationThreadPool,
            @Qualifier("otpThreadPool") ExecutorService otpThreadPool,
            @Qualifier("loginThreadPool") ExecutorService loginThreadPool,
            @Qualifier("userOperationsThreadPool") ExecutorService userOperationsThreadPool,
            @Qualifier("emailServiceThreadPool") ExecutorService emailServiceThreadPool) {
        this.userRegistrationThreadPool = userRegistrationThreadPool;
        this.otpThreadPool = otpThreadPool;
        this.loginThreadPool = loginThreadPool;
        this.userOperationsThreadPool = userOperationsThreadPool;
        this.emailServiceThreadPool = emailServiceThreadPool;
    }

    /**
     * Log thread pool metrics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logThreadPoolMetrics() {
        log.info("=== Thread Pool Metrics ===");
        logThreadPoolStatus("User Registration", userRegistrationThreadPool);
        logThreadPoolStatus("OTP Operations", otpThreadPool);
        logThreadPoolStatus("Login Operations", loginThreadPool);
        logThreadPoolStatus("User Operations", userOperationsThreadPool);
        logThreadPoolStatus("Email Service", emailServiceThreadPool);
        log.info("==========================");
    }

    private void logThreadPoolStatus(String name, ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
            log.info("{} - Active: {}, Pool Size: {}, Core Pool Size: {}, Max Pool Size: {}, Queue Size: {}, Completed Tasks: {}",
                    name,
                    threadPoolExecutor.getActiveCount(),
                    threadPoolExecutor.getPoolSize(),
                    threadPoolExecutor.getCorePoolSize(),
                    threadPoolExecutor.getMaximumPoolSize(),
                    threadPoolExecutor.getQueue().size(),
                    threadPoolExecutor.getCompletedTaskCount());
        } else {
            log.info("{} - Executor type not supported for detailed metrics", name);
        }
    }

    /**
     * Get thread pool health status
     */
    public boolean isThreadPoolHealthy() {
        return isThreadPoolHealthy(userRegistrationThreadPool) &&
               isThreadPoolHealthy(otpThreadPool) &&
               isThreadPoolHealthy(loginThreadPool) &&
               isThreadPoolHealthy(userOperationsThreadPool) &&
               isThreadPoolHealthy(emailServiceThreadPool);
    }

    private boolean isThreadPoolHealthy(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
            return threadPoolExecutor.getActiveCount() < threadPoolExecutor.getMaximumPoolSize();
        }
        return true; // Assume healthy if we can't determine
    }
} 