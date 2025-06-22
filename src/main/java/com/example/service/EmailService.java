package com.example.service;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    
    /**
     * Send OTP email asynchronously using thread pool
     */
    CompletableFuture<Void> sendOtpEmailAsync(String to, String username, String otp);
} 