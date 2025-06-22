package com.example.config.concurrency;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class ThreadPoolFactory {

    // Thread pool instances
    private static ExecutorService userRegistrationExecutor;
    private static ExecutorService otpExecutor;
    private static ExecutorService loginExecutor;
    private static ExecutorService userOperationsExecutor;
    private static ExecutorService emailServiceExecutor;

    /**
     * Generalized thread pool creation method
     */
    private ExecutorService createThreadPool(
            String namePrefix,
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            long keepAliveSeconds) {
        
        return new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new CustomThreadFactory(namePrefix),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Get the user registration thread pool (singleton)
     */
    public ExecutorService getUserRegistrationThreadPool() {
        if (userRegistrationExecutor == null || userRegistrationExecutor.isShutdown()) {
            userRegistrationExecutor = createThreadPool("user-registration", 2, 4, 50, 60L);
        }
        return userRegistrationExecutor;
    }

    /**
     * Get the OTP operations thread pool (singleton)
     */
    public ExecutorService getOtpThreadPool() {
        if (otpExecutor == null || otpExecutor.isShutdown()) {
            otpExecutor = createThreadPool("otp", 3, 6, 100, 60L);
        }
        return otpExecutor;
    }

    /**
     * Get the login operations thread pool (singleton)
     */
    public ExecutorService getLoginThreadPool() {
        if (loginExecutor == null || loginExecutor.isShutdown()) {
            loginExecutor = createThreadPool("login", 2, 4, 50, 60L);
        }
        return loginExecutor;
    }

    /**
     * Get the user operations thread pool (singleton)
     */
    public ExecutorService getUserOperationsThreadPool() {
        if (userOperationsExecutor == null || userOperationsExecutor.isShutdown()) {
            userOperationsExecutor = createThreadPool("user-operations", 2, 4, 50, 60L);
        }
        return userOperationsExecutor;
    }

    /**
     * Get the email service thread pool (singleton)
     */
    public ExecutorService getEmailServiceThreadPool() {
        if (emailServiceExecutor == null || emailServiceExecutor.isShutdown()) {
            emailServiceExecutor = createThreadPool("email-service", 3, 5, 100, 60L);
        }
        return emailServiceExecutor;
    }

    /**
     * Shutdown all thread pools
     */
    public void shutdownAllThreadPools() {
        shutdownThreadPool(userRegistrationExecutor, "userRegistrationExecutor");
        shutdownThreadPool(otpExecutor, "otpExecutor");
        shutdownThreadPool(loginExecutor, "loginExecutor");
        shutdownThreadPool(userOperationsExecutor, "userOperationsExecutor");
        shutdownThreadPool(emailServiceExecutor, "emailServiceExecutor");
    }

    /**
     * Generalized shutdown method
     */
    private void shutdownThreadPool(ExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            System.out.println("Shutdown " + name + " thread pool");
        }
    }
} 