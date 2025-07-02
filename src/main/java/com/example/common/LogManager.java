package com.example.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private static final Logger logger = LoggerFactory.getLogger("AppLogger");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logInfo(String header, String body, Object... params) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[{}][{}][{}]", timestamp, header, body, params);
    }

    public static void logWarn(String header, String body, Object... params) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.warn("[{}][{}][{}]", timestamp, header, body, params);
    }

    public static void logError(String header, String body, Throwable t, Object... params) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][{}][{}]", timestamp, header, body, params, t);
    }

    public static void logError(String header, String body, Object... params) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][{}][{}]", timestamp, header, body, params);
    }

    public static void logDebug(String header, String body, Object... params) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.debug("[{}][{}][{}]", timestamp, header, body, params);
    }

    // User-specific logging methods
    public static void logFailedLoginAttempt(String email, String reason, Throwable t) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][FAILED_LOGIN_ATTEMPT][Email: {}, Reason: {}]", timestamp, maskEmail(email), reason, t);
    }

    public static void logFailedLoginAttempt(String email, String reason) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][FAILED_LOGIN_ATTEMPT][Email: {}, Reason: {}]", timestamp, maskEmail(email), reason);
    }

    public static void logSuccessfulLogin(String email, boolean rememberMe) {
        String timestamp = LocalDateTime.now().format(formatter);
        String rememberMeText = rememberMe ? "with remember me" : "without remember me";
        logger.info("[{}][SUCCESSFUL_LOGIN][Email: {}, Login: {}]", timestamp, maskEmail(email), rememberMeText);
    }

    public static void logAccountRevoked(String email, String reason) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.warn("[{}][ACCOUNT_REVOKED][Email: {}, Reason: {}]", timestamp, maskEmail(email), reason);
    }

    public static void logAccountRevoked(String email, String reason, Throwable t) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.warn("[{}][ACCOUNT_REVOKED][Email: {}, Reason: {}]", timestamp, maskEmail(email), reason, t);
    }

    public static void logUserRegistration(String email, String userId) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[{}][USER_REGISTRATION][Email: {}, UserID: {}]", timestamp, maskEmail(email), userId);
    }

    public static void logUserRegistrationFailed(String email, String reason, Throwable t) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][USER_REGISTRATION_FAILED][Email: {}, Reason: {}]", timestamp, maskEmail(email), reason, t);
    }

    public static void logEmailVerification(String email, boolean success) {
        String timestamp = LocalDateTime.now().format(formatter);
        String status = success ? "SUCCESS" : "FAILED";
        logger.info("[{}][EMAIL_VERIFICATION_{}][Email: {}]", timestamp, status, maskEmail(email));
    }

    public static void logOtpSent(String email) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[{}][OTP_SENT][Email: {}]", timestamp, maskEmail(email));
    }

    public static void logOtpSentFailed(String email, String reason, Throwable t) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][OTP_SENT_FAILED][Email: {}, Reason: {}]", timestamp, maskEmail(email), reason, t);
    }

    public static void logUserUpdate(String email, String userId, String reason) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[{}][USER_UPDATE][Email: {}, UserID: {}, Reason: {}]", timestamp, maskEmail(email), userId, reason);
    }

    public static void logUserUpdateFailed(String email, String userId, String reason, Throwable t) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][USER_UPDATE_FAILED][Email: {}, UserID: {}, Reason: {}]", timestamp, maskEmail(email), userId, reason, t);
    }

    // System error logging methods (for network, Java exceptions, etc.)
    public static void logSystemError(String header, String email, String body, Throwable t) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][{}][Email: {}, {}]", timestamp, header, maskEmail(email), body, t);
    }

    public static void logSystemError(String header, String body, Throwable t) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][{}][{}]", timestamp, header, body, t);
    }

    public static void logSystemError(String header, String email, String body) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][{}][Email: {}, {}]", timestamp, header, maskEmail(email), body);
    }

    public static void logSystemError(String header, String body) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.error("[{}][{}][{}]", timestamp, header, body);
    }

    private static String maskEmail(String email) {
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