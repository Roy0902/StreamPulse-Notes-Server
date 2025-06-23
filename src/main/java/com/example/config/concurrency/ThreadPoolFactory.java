package com.example.config.concurrency;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.*;

@Configuration
public class ThreadPoolFactory {
    @Bean(name = "emailServiceThreadPool")
    public ExecutorService emailServiceThreadPool() {
        return new ThreadPoolExecutor(
            3, // core pool size
            5, // max pool size
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new CustomThreadFactory("email-service"),
            new CustomRejectedExecutionHandler()
        );
    }

    @Bean(name = "userServiceThreadPool")
    public ExecutorService userServiceThreadPool() {
        return new ThreadPoolExecutor(
            5, // core pool size
            10, // max pool size
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200),
            new CustomThreadFactory("user-service"),
            new CustomRejectedExecutionHandler()
        );
    }

    @Bean(name = "redisServiceThreadPool")
    public ExecutorService redisServiceThreadPool() {
        return new ThreadPoolExecutor(
            3, // core pool size
            5, // max pool size
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50),
            new CustomThreadFactory("redis-service"),
            new CustomRejectedExecutionHandler()
        );
    }
} 