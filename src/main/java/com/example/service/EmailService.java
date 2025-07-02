package com.example.service;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    /**
     * Send OTP email asynchronously using thread pool, returns CompletableFuture<Boolean> for success/failure
     */
    CompletableFuture<Boolean> sendOtpEmailAsync(String to, String username, String otp);
} 