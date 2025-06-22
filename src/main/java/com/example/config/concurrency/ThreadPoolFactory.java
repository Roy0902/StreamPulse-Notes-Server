package com.example.config.concurrency;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class ThreadPoolFactory {

    // Single instance of email service thread pool
    private static ExecutorService emailServiceExecutor;

    /**
     * Get the email service thread pool (singleton)
     */
    public ExecutorService getEmailServiceThreadPool() {
        if (emailServiceExecutor == null || emailServiceExecutor.isShutdown()) {
            emailServiceExecutor = createEmailServiceThreadPool();
        }
        return emailServiceExecutor;
    }

    /**
     * Create email service thread pool
     */
    private ExecutorService createEmailServiceThreadPool() {
        return new ThreadPoolExecutor(
            3,  // core pool size
            5, // max pool size
            60L, // keep alive time
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), // queue capacity
            new CustomThreadFactory("email-service"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Shutdown email service thread pool
     */
    public void shutdownEmailServiceThreadPool() {
        if (emailServiceExecutor != null && !emailServiceExecutor.isShutdown()) {
            emailServiceExecutor.shutdown();
            emailServiceExecutor = null;
        }
    }
} 