package com.example.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OtpCacheService {
    
    /**
     * Store OTP in cache with expiration asynchronously, returns CompletableFuture<Boolean> for success/failure
     */
    CompletableFuture<Boolean> storeOtpAsync(String email, String otpCode, String purpose);
    
    /**
     * Get OTP from cache
     */
    Optional<String> getOtp(String email, String purpose);
    
    /**
     * Remove OTP from cache
     */
    void removeOtp(String email, String purpose);
} 