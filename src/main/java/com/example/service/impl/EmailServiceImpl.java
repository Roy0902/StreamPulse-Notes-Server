package com.example.service.impl;

import com.example.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final ExecutorService emailExecutor;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public CompletableFuture<Void> sendOtpEmailAsync(String to, String username, String otp) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                sendOtpEmail(to, username, otp);
                log.info("OTP email sent successfully to: {}", maskEmail(to));
            } catch (Exception e) {
                log.error("Failed to send OTP email to: {}, Error: {}", maskEmail(to), e.getMessage(), e);
                throw new RuntimeException("Email sending failed", e);
            }
        }, emailExecutor);
    }

    private void sendOtpEmail(String to, String username, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>"
                           + "<h1 style='color: #333; text-align: center;'>Welcome, " + username + "!</h1>"
                           + "<p style='color: #666; font-size: 16px;'>Thank you for signing up. Please use the following OTP to verify your account:</p>"
                           + "<div style='background-color: #f8f9fa; border: 2px solid #007bff; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0;'>"
                           + "<h2 style='color: #007bff; font-size: 32px; letter-spacing: 8px; margin: 0;'>" + otp + "</h2>"
                           + "</div>"
                           + "<p style='color: #666; font-size: 14px;'>This OTP will expire in 10 minutes for security reasons.</p>"
                           + "<p style='color: #999; font-size: 12px;'>If you didn't request this verification, please ignore this email.</p>"
                           + "</div>";

        helper.setTo(to);
        helper.setSubject("Verify Your Account - OTP");
        helper.setFrom(fromEmail);
        helper.setText(htmlContent, true);

        mailSender.send(message);
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
