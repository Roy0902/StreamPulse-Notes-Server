package com.example.service.impl;

import com.example.service.OtpCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpCacheServiceImpl implements OtpCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${otp.cache.ttl:600}")
    private long defaultTtl;
    
    private static final String OTP_KEY_PREFIX = "otp:";
    
    @Override
    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackStoreOtp")
    public void storeOtp(String email, String otpCode, String purpose, long ttlSeconds) {
        try {
            String key = buildOtpKey(email, purpose);
            
            // Store OTP with expiration
            redisTemplate.opsForValue().set(key, otpCode, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("OTP stored in cache for email: {}, purpose: {}, TTL: {}s", 
                     maskEmail(email), purpose, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to store OTP in cache for email: {}, purpose: {}, Error: {}", 
                     maskEmail(email), purpose, e.getMessage(), e);
            throw new RuntimeException("Failed to store OTP in cache", e);
        }
    }
    
    public void fallbackStoreOtp(String email, String otpCode, String purpose, long ttlSeconds, Throwable t) {
        log.error("Fallback: Could not store OTP for email: {}, purpose: {}. Reason: {}", maskEmail(email), purpose, t.getMessage());
        // Optionally, you can notify or queue for later retry
    }
    
    @Override
    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackGetOtp")
    public Optional<String> getOtp(String email, String purpose) {
        try {
            String key = buildOtpKey(email, purpose);
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                log.debug("OTP retrieved from cache for email: {}, purpose: {}", 
                         maskEmail(email), purpose);
                return Optional.of(value.toString());
            }
            
            log.debug("OTP not found in cache for email: {}, purpose: {}", 
                     maskEmail(email), purpose);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get OTP from cache for email: {}, purpose: {}, Error: {}", 
                     maskEmail(email), purpose, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    public Optional<String> fallbackGetOtp(String email, String purpose, Throwable t) {
        log.error("Fallback: Could not get OTP for email: {}, purpose: {}. Reason: {}", maskEmail(email), purpose, t.getMessage());
        return Optional.empty();
    }
    
    @Override
    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackRemoveOtp")
    public void removeOtp(String email, String purpose) {
        try {
            String key = buildOtpKey(email, purpose);
            
            redisTemplate.delete(key);
            
            log.debug("OTP removed from cache for email: {}, purpose: {}", 
                     maskEmail(email), purpose);
        } catch (Exception e) {
            log.error("Failed to remove OTP from cache for email: {}, purpose: {}, Error: {}", 
                     maskEmail(email), purpose, e.getMessage(), e);
        }
    }
    
    public void fallbackRemoveOtp(String email, String purpose, Throwable t) {
        log.error("Fallback: Could not remove OTP for email: {}, purpose: {}. Reason: {}", maskEmail(email), purpose, t.getMessage());
        // Optionally, you can queue for later retry
    }
    
    private String buildOtpKey(String email, String purpose) {
        return OTP_KEY_PREFIX + email + ":" + purpose;
    }
    
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***@" + (atIndex > 0 ? email.substring(atIndex + 1) : "");
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }
} 