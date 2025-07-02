package com.example.service.impl;

import com.example.common.ApiResponse;
import com.example.common.JwtUtil;
import com.example.common.MessageConstants;
import com.example.dto.LoginRequestDTO;
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
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Supplier;

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
    private final RetryRegistry retryRegistry;

    /*
     * LOGIN FUNCTION
     */
    @Async("userServiceThreadPool")
    @Override
    @Retry(name = "database")
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackLogin")
    public CompletableFuture<ApiResponse<LoginResponseDTO>> login(LoginRequestDTO loginRequest) {
        return CompletableFuture.supplyAsync(() -> { 
            try {
                User user = userRepository.findByEmail(loginRequest.getEmail());

                if (user == null) {
                    return ApiResponse.error(404, MessageConstants.INVALID_EMAIL_OR_PASSWORD,null);
                }
                
                if (user.getStatus() == User.Status.unverified) {
                    return ApiResponse.error(401, MessageConstants.EMAIL_NOT_VERIFIED,null);
                }
                
                if (user.getStatus() == User.Status.revoked) {
                    return ApiResponse.error(403, MessageConstants.ACCOUNT_REVOKED,null);
                }
                
                if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                    int failedAttempts = incrementFailedLoginAttemptsAsync(user.getEmail()).join(); 
                    if(failedAttempts > MAX_FAILED_LOGIN_ATTEMPTS) {                
                        user.setStatus(User.Status.revoked);   
                        return ApiResponse.error(403, MessageConstants.ACCOUNT_REVOKED,null);
                    }
                    return ApiResponse.error(404, MessageConstants.INVALID_EMAIL_OR_PASSWORD,null);
                }
                
                resetFailedLoginAttemptsAsync(user.getEmail());
                
                LoginResponseDTO loginData = new LoginResponseDTO();
                loginData.setAccessToken(JwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getUserId()));
                loginData.setRefreshToken(JwtUtil.generateRefreshToken(user.getUsername(), user.getEmail(), user.getUserId(), loginRequest.isRememberMe()));
                
                String successMessage = loginRequest.isRememberMe() 
                    ? MessageConstants.LOGIN_SUCCESSFUL_REMEMBER_ME 
                    : MessageConstants.LOGIN_SUCCESSFUL;
                
                return ApiResponse.success(successMessage, loginData);
                
            } catch (Exception e) {
                LogManager.logSystemError(MessageConstants.SERVER_ERROR, e.getMessage(), e);
                return ApiResponse.error(500, MessageConstants.SERVER_ERROR, new LoginResponseDTO());
            }
        }, userServiceThreadPool);
    }

    private CompletableFuture<Integer> incrementFailedLoginAttemptsAsync(String userIdentifier) {
        return CompletableFuture.supplyAsync(() -> {
            int failedAttempts = incrementFailedLoginAttempts(userIdentifier);
            
            return failedAttempts;
        }, userServiceThreadPool);
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
  
    @Retry(name = "redis")
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackResetFailedLoginAttempts")
    private void resetFailedLoginAttempts(String userIdentifier) {
        String key = buildFailedAttemptsKey(userIdentifier);
        Boolean result = redisTemplate.delete(key);
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
        return CompletableFuture.supplyAsync(() -> {
            try {             
                User existingUser = userRepository.findByEmail(signupRequest.getEmail());
                if (existingUser != null) {
                    return ApiResponse.error(400, MessageConstants.EMAIL_ALREADY_EXISTS);
                }
                
                User user = new User();
                String userId = snowflake.generateId(1000).asString();
                user.setUserId(userId);
                user.setEmail(signupRequest.getEmail());
                user.setUsername(signupRequest.getUsername());
                user.setPasswordHash(passwordEncoder.encode(signupRequest.getPassword()));
                user.setStatus(User.Status.unverified);

                CompletableFuture<Boolean> isSaved = saveUser(user);
                if (!isSaved.get()) {
                    LogManager.logSystemError("Registration verification failed", signupRequest.getEmail(), "UserID not found in database", null);
                    return ApiResponse.error(500, "Registration completed but verification failed. Please contact support.");
                }
                
                emailService.sendOtpEmailAsync(user.getEmail(), user.getUsername(), "<OTP_PLACEHOLDER>");
                
                String timestamp = LocalDateTime.now().format(formatter);
                logger.info("[{}] User registered successfully - UserID: {}, Email: {}", 
                            timestamp, userId, maskEmail(signupRequest.getEmail()));
                
                return ApiResponse.success("Account created successfully. Please verify your email address to activate your account.");

            } catch (Exception e) {
                LogManager.logSystemError("Registration system error", signupRequest.getEmail(), e.getMessage(), e);
                return ApiResponse.error(500, MessageConstants.REGISTRATION_FAILED);
            }
        }, userServiceThreadPool);
    }

    public CompletableFuture<Boolean> saveUser(User user){
        return CompletableFuture.supplyAsync(() -> {
            User savedUser = userRepository.save(user);
            return savedUser != null;
        }, userServiceThreadPool);
    }
    
    public CompletableFuture<ApiResponse<Void>> fallbackSignUp(SignupRequestDTO signupRequest, Throwable t) {
        LogManager.logSystemError("Fallback: Could not sign up for email", signupRequest.getEmail(), t.getMessage(), t);
        return CompletableFuture.completedFuture(ApiResponse.error(503, "Service temporarily unavailable. Please try again later."));
    }
    
    /**
     * Verify that the user was actually persisted in the database
     */
    private boolean verifyUserPersisted(String userId) {
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
    public ApiResponse<Void> sendOtpForEmailVerification(String email) {
        try {
            return userServiceThreadPool.<ApiResponse<Void>>submit(() -> {
                User user = userRepository.findByEmail(email);
                if (user == null) {
                    return ApiResponse.error(404, "User not found with this email address. Please sign up first.");
                }

                if (user.getStatus() == User.Status.normal) {
                    return ApiResponse.error(400, "Email is already verified.");
                }

                if (user.getStatus() == User.Status.revoked) {
                    return ApiResponse.error(400, "Account has been revoked. Please contact support.");
                }

                String otpCode = generateOtp();
                boolean otpStored = otpCacheService.storeOtpAsync(email, otpCode, "EMAIL_VERIFICATION", 600).join();
                if (!otpStored) {
                    return ApiResponse.error(500, "Failed to store OTP. Please try again.");
                }
                emailService.sendOtpEmailAsync(email, user.getUsername(), otpCode)
                    .exceptionally(throwable -> {
                        String timestamp = LocalDateTime.now().format(formatter);
                        logger.error("[{}] Email sending failed - Email: {}, Error: {}", timestamp, maskEmail(email), throwable.getMessage());
                        return null;
                    });
                String timestamp = LocalDateTime.now().format(formatter);
                logger.info("[{}] OTP sent successfully - Email: {}", timestamp, maskEmail(email));
                return ApiResponse.success("OTP sent successfully. Please check your email.");
            }).get();
        } catch (Exception e) {
            LogManager.logSystemError("OTP send failed", email, e.getMessage(), e);
            return ApiResponse.error(500, "Failed to send OTP. Please try again.");
        }
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
                        user.setFailedLoginAttempts(0); // Reset failed attempts
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
    
    private <T> CompletableFuture<T> withRetry(String retryPolicy, Supplier<T> operation) {
        io.github.resilience4j.retry.Retry retry = retryRegistry.retry(retryPolicy);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return retry.executeSupplier(operation);
            } catch (Exception e) {
                logger.error("Operation failed with retry policy: {}. Error: {}", retryPolicy, e.getMessage(), e);
                throw e;
            }
        }, userServiceThreadPool);
    }
    

    
    private void revokeUserAccount(String userIdentifier) {
        try {
            User user = userRepository.findByEmail(userIdentifier);
            if (user != null) {
                user.setStatus(User.Status.revoked);
                userRepository.save(user);
                logger.warn("User account revoked due to excessive failed login attempts: {}", maskUserIdentifier(userIdentifier));
            }
        } catch (Exception e) {
            logger.error("Failed to revoke user account: {}. Reason: {}", maskUserIdentifier(userIdentifier), e.getMessage(), e);
        }
    }
    
    private CompletableFuture<Void> resetFailedLoginAttemptsAsync(String userIdentifier) {
        return CompletableFuture.runAsync(() -> {
            try {
                resetFailedLoginAttempts(userIdentifier);
            } catch (Exception e) {
                logger.error("Failed to reset failed login attempts for user: {}. Reason: {}", maskUserIdentifier(userIdentifier), e.getMessage(), e);
            }
        }, userServiceThreadPool);
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