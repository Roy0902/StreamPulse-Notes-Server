package com.example.config.scheduler;

import com.example.service.OtpCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {
    
    private final OtpCacheService otpCacheService;
    
    /**
     * Clean up expired OTPs from cache every 12 minutes
     */
    @Scheduled(fixedRate = 720000) // 12 minutes in milliseconds
    public void cleanupExpiredOtpsFromCache() {
        try {
            otpCacheService.cleanupExpiredOtps();
            log.info("OTP cache cleanup completed");
        } catch (Exception e) {
            log.error("Error cleaning up expired OTPs from cache: {}", e.getMessage(), e);
        }
    }
} 