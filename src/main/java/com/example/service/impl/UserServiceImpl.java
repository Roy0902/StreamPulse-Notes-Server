package com.example.service.impl;

import com.example.common.ApiResponse;
import com.example.common.JwtUtil;
import com.example.common.MessageConstants;
import com.example.config.concurrency.ThreadPoolFactory;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final OtpCacheService otpCacheService;
    private final ThreadPoolFactory threadPoolFactory;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final IdGenerator snowflake = IdGenerators.newSnowflakeIdGenerator();

    @Override
    public ApiResponse<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
        try {
            User user = userRepository.findByEmail(loginRequest.getEmail());

            if (user == null) {
                return ApiResponse.error(401, MessageConstants.INVALID_EMAIL_OR_PASSWORD);
            }
            
            // Check if email is verified
            if (user.getStatus() == User.Status.unverified) {
                return ApiResponse.error(401, "Please verify your email address before logging in.");
            }
            
            // Check if account is revoked
            if (user.getStatus() == User.Status.revoked) {
                return ApiResponse.error(401, "Your account has been revoked. Please contact support.");
            }
            
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                // Increment failed login attempts
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                userRepository.save(user);
                
                return ApiResponse.error(401, MessageConstants.INVALID_EMAIL_OR_PASSWORD);
            }
            
            // Reset failed login attempts on successful login
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            
            // Generate JWT token
            LoginResponseDTO loginData = new LoginResponseDTO();
            loginData.setAccessToken(JwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getUserId()));
            loginData.setRefreshToken(JwtUtil.generateRefreshToken(user.getUsername(), user.getEmail(), user.getUserId(), loginRequest.isRememberMe()));
            
            String successMessage = loginRequest.isRememberMe() 
                ? MessageConstants.LOGIN_SUCCESSFUL_REMEMBER_ME 
                : MessageConstants.LOGIN_SUCCESSFUL;
            
            return ApiResponse.success(successMessage, loginData);
            
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] Login system error - Email: {}, Error: {}", 
                        timestamp, maskEmail(loginRequest.getEmail()), e.getMessage(), e);
            return ApiResponse.error(500, MessageConstants.SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public CompletableFuture<ApiResponse<Void>> signUp(SignupRequestDTO signupRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check database health before proceeding
                if (!isDatabaseHealthy()) {
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.error("[{}] Registration failed - Database unhealthy - Email: {}", 
                                timestamp, maskEmail(signupRequest.getEmail()));
                    return ApiResponse.error(503, "Service temporarily unavailable. Please try again later.");
                }
                
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

                // Save with retry logic for transient failures
                User savedUser = saveUserWithRetry(user);
                
                // Verify the user was actually saved
                if (savedUser == null || !verifyUserPersisted(savedUser.getUserId())) {
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.error("[{}] Registration verification failed - UserID: {}, Email: {}", 
                                timestamp, userId, maskEmail(signupRequest.getEmail()));
                    return ApiResponse.error(500, "Registration completed but verification failed. Please contact support.");
                }
                
                String timestamp = LocalDateTime.now().format(formatter);
                logger.info("[{}] User registered successfully - UserID: {}, Email: {}", 
                            timestamp, userId, maskEmail(signupRequest.getEmail()));
                
                return ApiResponse.success("Account created successfully. Please verify your email address to activate your account.");

            } catch (Exception e) {
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] Registration system error - Email: {}, Error: {}", 
                            timestamp, maskEmail(signupRequest.getEmail()), e.getMessage(), e);
                return ApiResponse.error(500, MessageConstants.REGISTRATION_FAILED);
            }
        }, threadPoolFactory.getUserRegistrationThreadPool());
    }
    
    /**
     * Save user with retry logic for transient database failures
     */
    private User saveUserWithRetry(User user) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                return userRepository.save(user);
            } catch (Exception e) {
                retryCount++;
                String timestamp = LocalDateTime.now().format(formatter);
                logger.warn("[{}] User save attempt {} failed - UserID: {}, Error: {}", 
                           timestamp, retryCount, user.getUserId(), e.getMessage());
                
                if (retryCount >= maxRetries) {
                    throw e;
                }
                
                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(100 * retryCount * 2);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Save operation interrupted", ie);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Verify that the user was actually persisted in the database
     */
    private boolean verifyUserPersisted(String userId) {
        try {
            // Force a fresh database query to verify persistence
            User persistedUser = userRepository.findById(userId).orElse(null);
            if (persistedUser == null) {
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] User persistence verification failed - UserID: {} not found in database", 
                            timestamp, userId);
                return false;
            }
            
            // Additional verification: check if we can read the user by email
            User userByEmail = userRepository.findByEmail(persistedUser.getEmail());
            if (userByEmail == null || !userByEmail.getUserId().equals(userId)) {
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] User email verification failed - UserID: {}, Email: {}", 
                            timestamp, userId, maskEmail(persistedUser.getEmail()));
                return false;
            }
            
            return true;
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] User verification system error - UserID: {}, Error: {}", 
                        timestamp, userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check database connectivity and health
     */
    private boolean isDatabaseHealthy() {
        try {
            // Simple query to test database connectivity
            userRepository.count();
            return true;
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] Database health check failed - Error: {}", 
                        timestamp, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public UserDTO getUserById(String userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException(String.format(MessageConstants.USER_NOT_FOUND, userId)));
            return convertToDTO(user);
        } catch (RuntimeException e) {
            // Re-throw business logic exceptions without logging
            throw e;
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] Get user by ID system error - UserID: {}, Error: {}", 
                        timestamp, userId, e.getMessage(), e);
            throw new RuntimeException(MessageConstants.SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional
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
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] Update user system error - UserID: {}, Error: {}", 
                            timestamp, userId, e.getMessage(), e);
                throw new RuntimeException(MessageConstants.SERVER_ERROR, e);
            }
        }, threadPoolFactory.getUserOperationsThreadPool());
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

    // OTP Implementation Methods
    @Override
    @Transactional
    public CompletableFuture<ApiResponse<Void>> sendOtpForEmailVerification(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if user exists
                User user = userRepository.findByEmail(email);
                if (user == null) {
                    return ApiResponse.error(404, "User not found with this email address. Please sign up first.");
                }
                
                // Check if user is already verified
                if (user.getStatus() == User.Status.normal) {
                    return ApiResponse.error(400, "Email is already verified.");
                }
                
                // Check if user is revoked
                if (user.getStatus() == User.Status.revoked) {
                    return ApiResponse.error(400, "Account has been revoked. Please contact support.");
                }
                
                // Generate 6-digit OTP
                String otpCode = generateOtp();
                
                // Store OTP in Valkey cache only (10 minutes TTL)
                otpCacheService.storeOtp(email, otpCode, "EMAIL_VERIFICATION", 600);
                
                // Send OTP email asynchronously
                emailService.sendOtpEmailAsync(email, user.getUsername(), otpCode)
                        .exceptionally(throwable -> {
                            String timestamp = LocalDateTime.now().format(formatter);
                            logger.error("[{}] Email sending failed - Email: {}, Error: {}", 
                                        timestamp, maskEmail(email), throwable.getMessage());
                            return null;
                        });
                
                String timestamp = LocalDateTime.now().format(formatter);
                logger.info("[{}] OTP sent successfully - Email: {}", timestamp, maskEmail(email));
                
                return ApiResponse.success("OTP sent successfully. Please check your email.");
                
            } catch (Exception e) {
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] OTP send failed - Email: {}, Error: {}", 
                            timestamp, maskEmail(email), e.getMessage(), e);
                return ApiResponse.error(500, "Failed to send OTP. Please try again.");
            }
        }, threadPoolFactory.getOtpThreadPool());
    }

    @Override
    @Transactional
    public CompletableFuture<ApiResponse<Void>> verifyOtpAndActivateUser(String email, String otpCode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get OTP from Valkey cache only
                var cachedOtp = otpCacheService.getOtp(email, "EMAIL_VERIFICATION");
                
                if (cachedOtp.isPresent() && cachedOtp.get().equals(otpCode)) {
                    // OTP found in cache and matches
                    otpCacheService.removeOtp(email, "EMAIL_VERIFICATION");
                    
                    // Update user status to normal
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
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] OTP verification failed - Email: {}, Error: {}", 
                            timestamp, maskEmail(email), e.getMessage(), e);
                return ApiResponse.error(500, "Failed to verify OTP. Please try again.");
            }
        }, threadPoolFactory.getOtpThreadPool());
    }
    
    /**
     * Generate a 6-digit OTP
     */
    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
} 