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

    private final ExecutorService userServiceThreadPool;
    private final ExecutorService emailServiceThreadPool;
    private final ExecutorService redisServiceThreadPool;

    public ThreadPoolMonitor(
            @Qualifier("userServiceThreadPool") ExecutorService userServiceThreadPool,
            @Qualifier("emailServiceThreadPool") ExecutorService emailServiceThreadPool,
            @Qualifier("redisServiceThreadPool") ExecutorService redisServiceThreadPool) {
            this.userServiceThreadPool = userServiceThreadPool;
            this.emailServiceThreadPool = emailServiceThreadPool;
            this.redisServiceThreadPool = redisServiceThreadPool;
    }

    /**
     * Log thread pool metrics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logThreadPoolMetrics() {
        log.info("=== Thread Pool Metrics ===");
        logThreadPoolStatus("User Service", userServiceThreadPool);
        logThreadPoolStatus("Email Service", emailServiceThreadPool);
        logThreadPoolStatus("Redis Service", redisServiceThreadPool);
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
        return isThreadPoolHealthy(userServiceThreadPool) &&
               isThreadPoolHealthy(emailServiceThreadPool) &&
               isThreadPoolHealthy(redisServiceThreadPool);
    }

    private boolean isThreadPoolHealthy(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
            return threadPoolExecutor.getActiveCount() < threadPoolExecutor.getMaximumPoolSize();
        }
        return true; // Assume healthy if we can't determine
    }
} 