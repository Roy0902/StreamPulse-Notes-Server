package com.example.service.impl;

import com.example.service.OtpCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    private static final String OTP_PURPOSE_PREFIX = "purpose:";
    
    @Override
    public void storeOtp(String email, String otpCode, String purpose, long ttlSeconds) {
        try {
            String key = buildOtpKey(email, purpose);
            String purposeKey = buildPurposeKey(email, purpose);
            
            // Store OTP with expiration
            redisTemplate.opsForValue().set(key, otpCode, ttlSeconds, TimeUnit.SECONDS);
            
            // Store purpose mapping for cleanup
            redisTemplate.opsForValue().set(purposeKey, purpose, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("OTP stored in cache for email: {}, purpose: {}, TTL: {}s", 
                     maskEmail(email), purpose, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to store OTP in cache for email: {}, purpose: {}, Error: {}", 
                     maskEmail(email), purpose, e.getMessage(), e);
            throw new RuntimeException("Failed to store OTP in cache", e);
        }
    }
    
    @Override
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
    
    @Override
    public void removeOtp(String email, String purpose) {
        try {
            String key = buildOtpKey(email, purpose);
            String purposeKey = buildPurposeKey(email, purpose);
            
            redisTemplate.delete(key);
            redisTemplate.delete(purposeKey);
            
            log.debug("OTP removed from cache for email: {}, purpose: {}", 
                     maskEmail(email), purpose);
        } catch (Exception e) {
            log.error("Failed to remove OTP from cache for email: {}, purpose: {}, Error: {}", 
                     maskEmail(email), purpose, e.getMessage(), e);
        }
    }
    
    @Override
    public void cleanupExpiredOtps() {
        try {
            // This method is called by the scheduler
            // Redis automatically handles expiration, but we can log some metrics
            log.info("OTP cache cleanup completed - Redis handles automatic expiration");
        } catch (Exception e) {
            log.error("Failed to cleanup expired OTPs: {}", e.getMessage(), e);
        }
    }
    
    private String buildOtpKey(String email, String purpose) {
        return OTP_KEY_PREFIX + email + ":" + purpose;
    }
    
    private String buildPurposeKey(String email, String purpose) {
        return OTP_PURPOSE_PREFIX + email + ":" + purpose;
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