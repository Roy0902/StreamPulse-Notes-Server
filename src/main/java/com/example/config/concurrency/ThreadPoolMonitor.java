package com.example.config.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j
public class ThreadPoolMonitor {

    private final ThreadPoolFactory threadPoolFactory;

    public ThreadPoolMonitor(ThreadPoolFactory threadPoolFactory) {
        this.threadPoolFactory = threadPoolFactory;
    }

    /**
     * Log thread pool metrics every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logThreadPoolMetrics() {
        log.info("=== Thread Pool Metrics ===");
        logThreadPoolStatus("User Registration", threadPoolFactory.getUserRegistrationThreadPool());
        logThreadPoolStatus("OTP Operations", threadPoolFactory.getOtpThreadPool());
        logThreadPoolStatus("Login Operations", threadPoolFactory.getLoginThreadPool());
        logThreadPoolStatus("User Operations", threadPoolFactory.getUserOperationsThreadPool());
        logThreadPoolStatus("Email Service", threadPoolFactory.getEmailServiceThreadPool());
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
        return isThreadPoolHealthy(threadPoolFactory.getUserRegistrationThreadPool()) &&
               isThreadPoolHealthy(threadPoolFactory.getOtpThreadPool()) &&
               isThreadPoolHealthy(threadPoolFactory.getLoginThreadPool()) &&
               isThreadPoolHealthy(threadPoolFactory.getUserOperationsThreadPool()) &&
               isThreadPoolHealthy(threadPoolFactory.getEmailServiceThreadPool());
    }

    private boolean isThreadPoolHealthy(ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
            return threadPoolExecutor.getActiveCount() < threadPoolExecutor.getMaximumPoolSize();
        }
        return true; // Assume healthy if we can't determine
    }
} 