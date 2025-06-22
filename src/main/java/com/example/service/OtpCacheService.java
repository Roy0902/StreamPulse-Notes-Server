package com.example.service;

import java.util.Optional;

public interface OtpCacheService {
    
    /**
     * Store OTP in cache with expiration
     */
    void storeOtp(String email, String otpCode, String purpose, long ttlSeconds);
    
    /**
     * Get OTP from cache
     */
    Optional<String> getOtp(String email, String purpose);
    
    /**
     * Remove OTP from cache
     */
    void removeOtp(String email, String purpose);
    
    /**
     * Clean up expired OTPs
     */
    void cleanupExpiredOtps();
} 