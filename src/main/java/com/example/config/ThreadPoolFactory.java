package com.example.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class ThreadPoolFactory {

    public enum ThreadPoolType {
        USER_REGISTRATION,
        LOGIN,
        OTP,
        USER_OPERATIONS
    }

    // Cache for created thread pools
    private final ConcurrentHashMap<ThreadPoolType, Executor> threadPoolCache = new ConcurrentHashMap<>();

    /**
     * Get or create a thread pool based on the specified type
     * This is the actual factory method
     */
    public Executor getThreadPool(ThreadPoolType type) {
        return threadPoolCache.computeIfAbsent(type, this::createThreadPool);
    }

    /**
     * Create a new thread pool instance
     */
    private Executor createThreadPool(ThreadPoolType type) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        switch (type) {
            case USER_REGISTRATION:
                configureUserRegistrationPool(executor);
                break;
            case LOGIN:
                configureLoginPool(executor);
                break;
            case OTP:
                configureOtpPool(executor);
                break;
            case USER_OPERATIONS:
                configureUserOperationsPool(executor);
                break;
            default:
                configureDefaultPool(executor);
        }
        
        executor.initialize();
        return executor;
    }

    /**
     * Shutdown all thread pools
     */
    public void shutdownAll() {
        threadPoolCache.values().forEach(executor -> {
            if (executor instanceof ThreadPoolTaskExecutor) {
                ((ThreadPoolTaskExecutor) executor).shutdown();
            }
        });
        threadPoolCache.clear();
    }

    /**
     * Shutdown specific thread pool
     */
    public void shutdown(ThreadPoolType type) {
        Executor executor = threadPoolCache.remove(type);
        if (executor instanceof ThreadPoolTaskExecutor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    }

    private void configureUserRegistrationPool(ThreadPoolTaskExecutor executor) {
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("user-reg-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void configureLoginPool(ThreadPoolTaskExecutor executor) {
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(300);
        executor.setThreadNamePrefix("login-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void configureOtpPool(ThreadPoolTaskExecutor executor) {
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("otp-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void configureUserOperationsPool(ThreadPoolTaskExecutor executor) {
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(150);
        executor.setThreadNamePrefix("user-ops-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void configureDefaultPool(ThreadPoolTaskExecutor executor) {
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("default-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }
} 