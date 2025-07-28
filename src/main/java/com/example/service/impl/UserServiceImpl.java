package com.example.service.impl;

import com.example.common.ApiResponse;
import com.example.common.JwtUtil;
import com.example.common.MessageConstants;
import com.example.common.Result;
import com.example.dto.LoginRequestDTO;
import com.example.exception.LoginException;
import com.example.dto.LoginResponseDTO;
import com.example.dto.SignupRequestDTO;
import com.example.dto.UserDTO;
import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.service.EmailService;
import com.example.service.UserService;
import lombok.RequiredArgsConstructor;
import com.github.rholder.fauxflake.IdGenerators;
import com.github.rholder.fauxflake.api.IdGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.service.OtpCacheService;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.common.LogManager;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final OtpCacheService otpCacheService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final IdGenerator snowflake = IdGenerators.newSnowflakeIdGenerator();

    private final @Qualifier("userServiceThreadPool") ExecutorService userServiceThreadPool;
    
    private static final String FAILED_ATTEMPTS_KEY_PREFIX = "failed_attempts:";
    private static final long FAILED_ATTEMPTS_TTL = 86400; 
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    
    private final RedisTemplate<String, Object> redisTemplate;

    /*
     * LOGIN FUNCTION
     */
    @Override
    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackLogin")
    public CompletableFuture<ApiResponse<LoginResponseDTO>> login(LoginRequestDTO loginRequest) {
        return CompletableFuture.supplyAsync(() -> userRepository.findByEmail(loginRequest.getEmail()), userServiceThreadPool)
            .thenCompose(user -> {
                if (user == null) {
                    throw new LoginException(MessageConstants.INVALID_EMAIL_OR_PASSWORD, 404);
                }

                return validateUserStatus(user);
            })
            .thenCompose(result -> {
                if (!result.isSuccess()) {
                    throw new LoginException(result.getErrorMessage(), result.getStatusCode());
                }

                User user = result.getData();
                return validatePassword(loginRequest, user);
            })
            .thenCompose(result -> {
                if (!result.isSuccess()) {
                    throw new LoginException(result.getErrorMessage(), result.getStatusCode());
                }
                
                User user = result.getData();
                return handleSuccessfulLogin(user, loginRequest.isRememberMe());
            })
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof LoginException) {
                    LoginException loginException = (LoginException) throwable.getCause();
                    return ApiResponse.error(loginException.getStatusCode(), loginException.getMessage(), new LoginResponseDTO());
                }

                LogManager.logSystemError(MessageConstants.SERVER_ERROR, throwable.getMessage(), throwable);
                return ApiResponse.error(500, MessageConstants.SERVER_ERROR, new LoginResponseDTO());
            });
    }

    private CompletableFuture<Result<User>> validateUserStatus(User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (user.getStatus() == User.Status.unverified) {
                return Result.error(MessageConstants.EMAIL_NOT_VERIFIED, 401);
            }
            if (user.getStatus() == User.Status.revoked) {
                return Result.error(MessageConstants.ACCOUNT_REVOKED, 403);
            }
            return Result.success(user);
        }, userServiceThreadPool);
    }

    private CompletableFuture<Result<User>> validatePassword(LoginRequestDTO loginRequest, User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                handleFailedLogin(user.getEmail());
                return Result.error("Invalid password", 404);
            }
            resetFailedLoginAttempts(user.getEmail());
            return Result.success(user);
        }, userServiceThreadPool);
    }

    private CompletableFuture<Integer> handleFailedLogin(String email) {
        return incrementFailedLoginAttemptsAsync(email)
            .thenCompose(attempts -> {
                if (attempts > MAX_FAILED_LOGIN_ATTEMPTS) {
                    return revokeUserAccount(email)
                        .thenApply(v -> attempts);
                }
                return CompletableFuture.completedFuture(attempts);
            });
    }

    private CompletableFuture<ApiResponse<LoginResponseDTO>> handleSuccessfulLogin(User user, boolean rememberMe) {
        return CompletableFuture.supplyAsync(() -> {
            LoginResponseDTO loginData = new LoginResponseDTO();
            loginData.setAccessToken(JwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getUserId()));
            loginData.setRefreshToken(JwtUtil.generateRefreshToken(user.getUsername(), user.getEmail(), user.getUserId(), rememberMe));
            
            String successMessage = rememberMe 
                ? MessageConstants.LOGIN_SUCCESSFUL_REMEMBER_ME 
                : MessageConstants.LOGIN_SUCCESSFUL;
            
            return ApiResponse.success(successMessage, loginData);
        }, userServiceThreadPool);
    }

    private CompletableFuture<Void> revokeUserAccount(String email) {
        return CompletableFuture.runAsync(() -> {
            try {
                User user = userRepository.findByEmail(email);
                if (user != null) {
                    user.setStatus(User.Status.revoked);
                    userRepository.save(user);
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.warn("[{}] User account revoked due to excessive failed login attempts: {}", 
                                timestamp, maskEmail(email));
                }
            } catch (Exception e) {
                LogManager.logSystemError("Failed to revoke user account", email, e.getMessage(), e);
            }
        }, userServiceThreadPool);
    }

    private CompletableFuture<Integer> incrementFailedLoginAttemptsAsync(String userIdentifier) {
        return CompletableFuture.supplyAsync(() -> incrementFailedLoginAttempts(userIdentifier), userServiceThreadPool);
    }

    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackIncrementFailedLoginAttempts")
    private int incrementFailedLoginAttempts(String userIdentifier) {
        String key = buildFailedAttemptsKey(userIdentifier);
        Long result = redisTemplate.opsForValue().increment(key);
        if (result != null && result == 1) {
            redisTemplate.expire(key, FAILED_ATTEMPTS_TTL, TimeUnit.SECONDS);
        }
        LogManager.logSystemError(MessageConstants.HEADER_LOGIN_FAILED, userIdentifier, "Invalid password, attempt: " + result);
        return result != null ? result.intValue() : 0;
    }
    
    public int fallbackIncrementFailedLoginAttempts(String userIdentifier, Throwable t) {
        LogManager.logSystemError("REDIS_CONNECTION_FAILED", userIdentifier, 
                                 "Could not increment failed login attempts. Reason: " + t.getMessage(), t);
        
        return 0;
    }
  
    private CompletableFuture<Void> resetFailedLoginAttempts(String userIdentifier) {
        return CompletableFuture.runAsync(() -> resetFailedLoginAttemptsSync(userIdentifier), userServiceThreadPool);
    }

    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackResetFailedLoginAttempts")
    private void resetFailedLoginAttemptsSync(String userIdentifier) {
        String key = buildFailedAttemptsKey(userIdentifier);
        redisTemplate.delete(key);
    }

    public void fallbackResetFailedLoginAttempts(String userIdentifier, Throwable t) {
        LogManager.logSystemError("REDIS_CONNECTION_FAILED", userIdentifier, 
                                "Could not reset failed login attempts. Reason: " + t.getMessage(), t);
        
    }

    public ApiResponse<LoginResponseDTO> fallbackLogin(LoginRequestDTO loginRequest, Throwable t) {
        LogManager.logSystemError("Fallback: Could not login for email", loginRequest.getEmail(), t.getMessage(), t);
        return ApiResponse.error(503, "Service temporarily unavailable. Please try again later.");
    }

    private String buildFailedAttemptsKey(String userIdentifier) {
        return FAILED_ATTEMPTS_KEY_PREFIX + userIdentifier;
    }

    /*
     * SIGNUP FUNCTION
     */
    @Override
    @Transactional
    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackSignUp")
    public CompletableFuture<ApiResponse<Void>> signUp(SignupRequestDTO signupRequest) {
        return CompletableFuture.supplyAsync(() -> userRepository.findByEmail(signupRequest.getEmail()), userServiceThreadPool)
            .thenCompose(existingUser -> {
                if (existingUser != null) {
                    return CompletableFuture.completedFuture(ApiResponse.error(MessageConstants.EMAIL_ALREADY_EXISTS, 409));
                }

                return createUser(signupRequest);
            })
            .thenCompose(createResponse -> {
                if (createResponse.getStatusCode() != 200) {
                    return CompletableFuture.completedFuture(ApiResponse.error(createResponse.errorMessage(), createResponse.getStatusCode()));
                }

                return saveUser(createResponse.getData());
            })
            .thenCompose(saveResponse -> {
                if (saveResponse.getStatusCode() != 200) {
                    return CompletableFuture.completedFuture(ApiResponse.error(saveResponse.getErrorMessage(), saveResponse.getStatusCode()));
                }

                return verifyUserPersistence(saveResponse.getData());
            })
            .thenCompose(verifyResponse -> {
                if (verifyResponse.getStatusCode() != 200) {
                    return CompletableFuture.completedFuture(ApiResponse.error(verifyResponse.getErrorMessage(), verifyResponse.getStatusCode()));
                }

                return finalizeSignup(verifyResponse.getData());
            })
            .thenApply(result -> {
                if (result.getStatusCode() != 200) {
                    return CompletableFuture.completedFuture(ApiResponse.error(result.getErrorMessage(), result.getStatusCode()));
                }

                return ApiResponse.success("Account created successfully. Please verify your email address to activate your account.");
            })
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof LoginException) {
                    LoginException loginException = (LoginException) throwable.getCause();
                    return ApiResponse.error(userException.getStatusCode(), userException.getMessage(), new LoginResponseDTO());
                }
                LogManager.logSystemError(MessageConstants.SERVER_ERROR, throwable.getMessage(), throwable);
                return ApiResponse.error(500, MessageConstants.SERVER_ERROR, new LoginResponseDTO());
            });
    }
    
    public CompletableFuture<ApiResponse<Void>> fallbackSignUp(SignupRequestDTO signupRequest, Throwable t) {
        LogManager.logSystemError("Fallback: Could not sign up for email", signupRequest.getEmail(), t.getMessage(), t);
        return CompletableFuture.completedFuture(ApiResponse.error(503, "Service temporarily unavailable. Please try again later."));
    }

    private CompletableFuture<Result<User>> createUser(SignupRequestDTO signupRequest) {
        return CompletableFuture.supplyAsync(() -> {
            User user = new User();

            String userId = null;
            try {
                userId = snowflake.generateId(1000).asString();
            } catch (InterruptedException e) {
                String timestamp = LocalDateTime.now().format(formatter);
                logger.warn("[{}] Failed to create new user entity", timestamp);
                return Result.error("Failed to generate new user entity.");
            }

            user.setUserId(userId);
            user.setEmail(signupRequest.getEmail());
            user.setUsername(signupRequest.getUsername());
            user.setPasswordHash(passwordEncoder.encode(signupRequest.getPassword()));
            user.setStatus(User.Status.unverified);
            return Result.success(user);
        }, userServiceThreadPool);
    }

    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackSaveUser")
    private CompletableFuture<Result<User>> saveUser(User user) {
        return CompletableFuture.supplyAsync(() -> {
            User savedUser = userRepository.save(user);
            if (savedUser == null) {
                LogManager.logSystemError("Registration verification failed", user.getEmail(), "UserID not found in database", null);
                return Result.error("Service temporarily unavailable. Please try it later.", 503);
            }
            return Result.success(savedUser);
        }, userServiceThreadPool);
    }

    public CompletableFuture<ApiResponse<User>> fallbackSaveUser(User user, Throwable t) {
        LogManager.logSystemError("Fallback: Could not save user", user.getEmail(), t.getMessage(), t);
        return CompletableFuture.completedFuture(ApiResponse.error(503, "Service temporarily unavailable. Please try again later."));
    }

    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackVerifyUserPersistence")
    private CompletableFuture<Result<User>> verifyUserPersistence(User user) {
        return verifyUserPersistedAsync(user.getUserId())
            .thenCompose(isPersisted -> {
                if (!isPersisted) {
                    LogManager.logSystemError("User persistence verification failed", user.getUserId(), "UserID not found in database", null);
                    return CompletableFuture.completedFuture(Result.error("Registration completed but verification failed. Please contact support.", 503));
                }
                return CompletableFuture.completedFuture(Result.success(user));
            });
    }

    public CompletableFuture<ApiResponse<User>> fallbackVerifyUserPersistence(User user, Throwable t) {
        LogManager.logSystemError("Fallback: Could not verify user persistence", user.getUserId(), t.getMessage(), t);
        return CompletableFuture.completedFuture(ApiResponse.error(503, "Service temporarily unavailable. Please try again later."));
    }

    @Retry(name = "email")
    @CircuitBreaker(name = "email", fallbackMethod = "fallbackFinalizeSignup")
    private CompletableFuture<Result<Void>> finalizeSignup(User user) {
        return CompletableFuture.supplyAsync(() -> {
            emailService.sendOtpEmailAsync(user.getEmail(), user.getUsername(), "<OTP_PLACEHOLDER>");
            String timestamp = LocalDateTime.now().format(formatter);
            logger.info("[{}] User registered successfully - UserID: {}, Email: {}", timestamp, user.getUserId(), maskEmail(user.getEmail()));
            return Result.success(null);
        }, userServiceThreadPool);
    }

    public CompletableFuture<ApiResponse<Void>> fallbackFinalizeSignup(User user, Throwable t) {
        LogManager.logSystemError("Fallback: Could not finalize signup", user.getEmail(), t.getMessage(), t);
        // Even if email sending fails, the user is still created successfully
        String timestamp = LocalDateTime.now().format(formatter);
        logger.warn("[{}] User registered but email notification failed - UserID: {}, Email: {}", 
                    timestamp, user.getUserId(), maskEmail(user.getEmail()));
        return CompletableFuture.completedFuture(ApiResponse.success("Account created successfully. Please contact support for email verification."));
    }
    
    /**
     * Verify that the user was actually persisted in the database
     * Only for the sign-up process
     */
    private CompletableFuture<Boolean> verifyUserPersistedAsync(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User persistedUser = userRepository.findById(userId).orElse(null);
                if (persistedUser == null) {
                    LogManager.logSystemError("User persistence verification failed", userId, "UserID not found in database", null);
                    return false;
                }
                
                User userByEmail = userRepository.findByEmail(persistedUser.getEmail());
                if (userByEmail == null || !userByEmail.getUserId().equals(userId)) {
                    LogManager.logSystemError("User email verification failed", userId, "Email verification failed", null);
                    return false;
                }
                
                return true;
            } catch (Exception e) {
                LogManager.logSystemError("User verification system error", userId, e.getMessage(), e);
                return false;
            }
        }, userServiceThreadPool);
    }

    @Override
    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackGetUserById")
    public UserDTO getUserById(String userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException(String.format(MessageConstants.USER_NOT_FOUND, userId)));
            return convertToDTO(user);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogManager.logSystemError("Get user by ID system error", userId, e.getMessage(), e);
            throw new RuntimeException(MessageConstants.SERVER_ERROR, e);
        }
    }

    public UserDTO fallbackGetUserById(String userId, Throwable t) {
        LogManager.logSystemError("Fallback: Could not get user by ID", userId, t.getMessage(), t);
        throw new RuntimeException("Service temporarily unavailable. Please try again later.", t);
    }

    /*
     * UPDATE USER FUNCTION
     */
    @Override
    @Transactional
    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackUpdateUser")
    public CompletableFuture<UserDTO> updateUser(String userId, UserDTO userDTO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException(String.format(MessageConstants.USER_NOT_FOUND, userId)));
                
                user.setUsername(userDTO.getUsername());
                user.setEmail(userDTO.getEmail());
                if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                    user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
                }
                user.setProvider(userDTO.getProvider());
                user.setProviderId(userDTO.getProviderId());
                
                User savedUser = userRepository.save(user);
                return convertToDTO(savedUser);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                LogManager.logSystemError("Update user system error", userId, e.getMessage(), e);
                throw new RuntimeException(MessageConstants.SERVER_ERROR, e);
            }
        }, userServiceThreadPool);
    }

    public CompletableFuture<UserDTO> fallbackUpdateUser(String userId, UserDTO userDTO, Throwable t) {
        LogManager.logSystemError("Fallback: Could not update user", userId, t.getMessage(), t);
        return CompletableFuture.failedFuture(new RuntimeException("Service temporarily unavailable. Please try again later.", t));
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPassword(user.getPasswordHash());
        dto.setProvider(user.getProvider());
        dto.setProviderId(user.getProviderId());
        dto.setStatus(user.getStatus().name());
        return dto;
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

    @Override
    @Transactional
    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackSendOtpForEmailVerification")
    public CompletableFuture<ApiResponse<Void>> sendOtpForEmailVerification(String email) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                User user = userRepository.findByEmail(email);
                if (user == null) {
                    LogManager.logSystemError("Email verification failed", email, "User not found", null);
                    return ApiResponse.error(404, "User with email " + email + " not found");
                }

                if (user.getStatus() != User.Status.unverified) {
                    LogManager.logSystemError("Email verification failed", email, "User already verified", null);
                    return ApiResponse.error(403, "User with email " + email + " is already verified");
                }

                String otpCode = generateOtp();
                CompletableFuture<Boolean> isSuccess = otpCacheService.storeOtpAsync(email, otpCode, "EMAIL_VERIFICATION");

                String timestamp = LocalDateTime.now().format(formatter);
                logger.info("[{}] Email verification OTP sent - Email: {}", timestamp, maskEmail(email));
        
                isSuccess.thenAccept(success -> {
                    if (success) {
                        emailService.sendOtpEmailAsync(email, user.getUsername(), otpCode);
                    } else {
                        LogManager.logSystemError("OTP storage failed", email, "Failed to store OTP in cache", null);
                    }
                });

                return ApiResponse.success("OTP sent successfully. Please check your email to verify your account.");

            } catch (Exception e) {
                LogManager.logSystemError("Email verification system error", email, e.getMessage(), e);
                return ApiResponse.error(500, "Failed to send OTP. Please try again later.");
            }
        }, userServiceThreadPool);
    }

    @Override
    @Transactional
    public CompletableFuture<ApiResponse<Void>> verifyOtpAndActivateUser(String email, String otpCode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var cachedOtp = otpCacheService.getOtp(email, "EMAIL_VERIFICATION");
                
                if (cachedOtp.isPresent() && cachedOtp.get().equals(otpCode)) {
                    otpCacheService.removeOtp(email, "EMAIL_VERIFICATION");
                    
                    User user = userRepository.findByEmail(email);
                    if (user != null) {
                        user.setStatus(User.Status.normal);
                        userRepository.save(user);
                    }
                    
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.info("[{}] Email verified successfully - Email: {}", timestamp, maskEmail(email));
                    
                    return ApiResponse.success("Email verified successfully. You can now login.");
                } else {
                    return ApiResponse.error(400, "Invalid or expired OTP. Please request a new one.");
                }
                
            } catch (Exception e) {
                LogManager.logSystemError("OTP verification failed", email, e.getMessage(), e);
                return ApiResponse.error(500, "Failed to verify OTP. Please try again.");
            }
        }, userServiceThreadPool);
    }
    
    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    private String maskUserIdentifier(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isEmpty()) {
            return "***";
        }
        // If it looks like an email, mask it
        if (userIdentifier.contains("@")) {
            return maskEmail(userIdentifier);
        }
        // For userId or username, show first and last character
        if (userIdentifier.length() <= 2) {
            return "***";
        }
        return userIdentifier.charAt(0) + "***" + userIdentifier.charAt(userIdentifier.length() - 1);
    }
} 