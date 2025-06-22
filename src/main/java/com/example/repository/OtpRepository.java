package com.example.repository;

import com.example.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {
    
    // Find the most recent valid OTP for email verification
    Optional<Otp> findFirstByEmailAndPurposeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, String purpose, LocalDateTime now);
    
    // Find all OTPs for an email (for cleanup)
    List<Otp> findByEmailAndPurpose(String email, String purpose);
    
    // Mark OTP as used
    @Modifying
    @Query("UPDATE Otp o SET o.isUsed = true WHERE o.otpId = :otpId")
    void markAsUsed(@Param("otpId") String otpId);
    
    // Clean up expired OTPs
    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
} 