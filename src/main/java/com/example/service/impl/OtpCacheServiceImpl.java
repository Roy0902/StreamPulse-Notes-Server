package com.example.service.impl;

import com.example.service.OtpCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;
import io.netty.util.concurrent.Future;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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
    
    private final @Qualifier("redisServiceThreadPool") ExecutorService redisServiceThreadPool;
    
    
    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackStoreOtpWithRetry")
    private void storeOtpWithRetry(String key, String otpCode, long ttlSeconds, String email, String purpose) {
        redisTemplate.opsForValue().set(key, otpCode, ttlSeconds, TimeUnit.SECONDS);
    }
    
    public void fallbackStoreOtpWithRetry(String key, String otpCode, long ttlSeconds, String email, String purpose, Throwable t) {
        log.error("Fallback: Could not store OTP for email: {}, purpose: {}. Reason: {}", maskEmail(email), purpose, t.getMessage());
        throw new RuntimeException("Failed to store OTP in Redis", t);
    }
    
    @Override
    public Optional<String> getOtp(String email, String purpose) {
        String key = buildOtpKey(email, purpose);
        return getOtpWithRetry(key, email, purpose);
    }
    
    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackGetOtpWithRetry")
    private Optional<String> getOtpWithRetry(String key, String email, String purpose) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            log.debug("OTP retrieved from cache for email: {}, purpose: {}", maskEmail(email), purpose);
            return Optional.of(value.toString());
        }
        log.debug("OTP not found in cache for email: {}, purpose: {}", maskEmail(email), purpose);
        return Optional.empty();
    }
    
    public Optional<String> fallbackGetOtpWithRetry(String key, String email, String purpose, Throwable t) {
        log.error("Fallback: Could not get OTP for email: {}, purpose: {}. Reason: {}", maskEmail(email), purpose, t.getMessage());
        return Optional.empty();
    }
    
    @Override
    public void removeOtp(String email, String purpose) {
        String key = buildOtpKey(email, purpose);
        CompletableFuture.runAsync(() -> {
            removeOtpWithRetry(key, email, purpose);
        }, redisServiceThreadPool).exceptionally(t -> {
            fallbackRemoveOtpWithRetry(key, email, purpose, t);
            return null;
        });
    }
    
    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackRemoveOtpWithRetry")
    private void removeOtpWithRetry(String key, String email, String purpose) {
        redisTemplate.delete(key);
        log.debug("OTP removed from cache for email: {}, purpose: {}", maskEmail(email), purpose);
    }
    
    public void fallbackRemoveOtpWithRetry(String key, String email, String purpose, Throwable t) {
        log.error("Fallback: Could not remove OTP for email: {}, purpose: {}. Reason: {}", maskEmail(email), purpose, t.getMessage());
    }
    
    @Override
    public CompletableFuture<Boolean> storeOtpAsync(String email, String otpCode, String purpose) {
        String key = buildOtpKey(email, purpose);
        return CompletableFuture.supplyAsync(() -> {
            try {
                storeOtpWithRetry(key, otpCode, defaultTtl, email, purpose);
                log.info("OTP stored successfully for email: {}, purpose: {}", maskEmail(email), purpose);
                return true;
            } catch (Exception e) {
                log.error("Failed to store OTP for email: {}, purpose: {}. Reason: {}", maskEmail(email), purpose, e.getMessage(), e);
                return false;
            }
        }, redisServiceThreadPool);
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