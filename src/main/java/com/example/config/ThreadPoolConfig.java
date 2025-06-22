package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ThreadPoolConfig {

    private final ThreadPoolFactory threadPoolFactory;

    /**
     * Simple thread pool for user service operations
     */
    @Bean("userServiceExecutor")
    public Executor userServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("user-service-");
        executor.initialize();
        return executor;
    }

    @Bean("userRegistrationExecutor")
    public Executor userRegistrationExecutor() {
        return threadPoolFactory.createThreadPool(ThreadPoolFactory.ThreadPoolType.USER_REGISTRATION);
    }

    @Bean("loginExecutor")
    public Executor loginExecutor() {
        return threadPoolFactory.createThreadPool(ThreadPoolFactory.ThreadPoolType.LOGIN);
    }

    @Bean("otpExecutor")
    public Executor otpExecutor() {
        return threadPoolFactory.createThreadPool(ThreadPoolFactory.ThreadPoolType.OTP);
    }

    @Bean("userOperationsExecutor")
    public Executor userOperationsExecutor() {
        return threadPoolFactory.createThreadPool(ThreadPoolFactory.ThreadPoolType.USER_OPERATIONS);
    }
} 