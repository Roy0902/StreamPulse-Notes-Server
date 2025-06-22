package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id", length = 36)
    private String otpId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose; // e.g., "EMAIL_VERIFICATION", "PASSWORD_RESET"

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
} 