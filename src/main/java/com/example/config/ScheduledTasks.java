package com.example.config;

import com.example.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {
    
    private final OtpRepository otpRepository;
    
    /**
     * Clean up expired OTPs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredOtps() {
        try {
            LocalDateTime now = LocalDateTime.now();
            otpRepository.deleteExpiredOtps(now);
            log.info("Expired OTPs cleaned up");
        } catch (Exception e) {
            log.error("Error cleaning up expired OTPs: {}", e.getMessage(), e);
        }
    }
} 