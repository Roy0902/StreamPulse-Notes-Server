package com.example.service.impl;

import com.example.common.ApiResponse;
import com.example.common.JwtUtil;
import com.example.common.MessageConstants;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.UserDTO;
import com.example.entity.User;
import com.example.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final IdGenerator snowflake = IdGenerators.newSnowflakeIdGenerator();

    @Override
    public ApiResponse<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
        try {
            User user = userRepository.findByEmail(loginRequest.getEmail());

            if (user == null) {
                return ApiResponse.error(401, MessageConstants.INVALID_EMAIL_OR_PASSWORD);
            }
            
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                return ApiResponse.error(401, MessageConstants.INVALID_EMAIL_OR_PASSWORD);
            }
            
            LoginResponseDTO loginData = new LoginResponseDTO();
            loginData.setToken(JwtUtil.generateToken(user.getUsername(), user.getEmail()));
            loginData.setRefreshToken(JwtUtil.generateRefreshToken(user.getUsername(), user.getEmail(), loginRequest.isRememberMe()));
            
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
    public ApiResponse<Void> register(UserDTO userDTO) throws InterruptedException {
        try {
            // Check database health before proceeding
            if (!isDatabaseHealthy()) {
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] Registration failed - Database unhealthy - Email: {}", 
                            timestamp, maskEmail(userDTO.getEmail()));
                return ApiResponse.error(503, "Service temporarily unavailable. Please try again later.");
            }
            
            User existingUser = userRepository.findByEmail(userDTO.getEmail());
            if (existingUser != null) {
                return ApiResponse.error(400, MessageConstants.EMAIL_ALREADY_EXISTS);
            }
            
            User user = new User();
            String userId = snowflake.generateId(1000).asString();
            user.setUserId(userId);
            user.setEmail(userDTO.getEmail());
            user.setUsername(userDTO.getUsername());
            user.setPasswordHash(passwordEncoder.encode(userDTO.getPasswordHash()));
            user.setProvider(userDTO.getProvider());
            user.setProviderId(userDTO.getProviderId());
            user.setStatus(User.Status.normal);

            // Save with retry logic for transient failures
            User savedUser = saveUserWithRetry(user);
            
            // Verify the user was actually saved
            if (savedUser == null || !verifyUserPersisted(savedUser.getUserId())) {
                String timestamp = LocalDateTime.now().format(formatter);
                logger.error("[{}] Registration verification failed - UserID: {}, Email: {}", 
                            timestamp, userId, maskEmail(userDTO.getEmail()));
                return ApiResponse.error(500, "Registration completed but verification failed. Please contact support.");
            }
            
            return ApiResponse.success(MessageConstants.REGISTRATION_SUCCESSFUL);

        } catch (InterruptedException e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] Registration interrupted - Email: {}, Error: {}", 
                        timestamp, maskEmail(userDTO.getEmail()), e.getMessage(), e);
            throw new InterruptedException(MessageConstants.REGISTRATION_FAILED + ": " + e.getMessage());
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] Registration system error - Email: {}, Error: {}", 
                        timestamp, maskEmail(userDTO.getEmail()), e.getMessage(), e);
            return ApiResponse.error(500, MessageConstants.REGISTRATION_FAILED);
        }
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
                    Thread.sleep(100 * retryCount);
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
    public UserDTO updateUser(String userId, UserDTO userDTO) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException(String.format(MessageConstants.USER_NOT_FOUND, userId)));
            
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            if (userDTO.getPasswordHash() != null && !userDTO.getPasswordHash().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(userDTO.getPasswordHash()));
            }
            user.setProvider(userDTO.getProvider());
            user.setProviderId(userDTO.getProviderId());
            
            return convertToDTO(userRepository.save(user));
        } catch (RuntimeException e) {

            throw e;
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] Update user system error - UserID: {}, Error: {}", 
                        timestamp, userId, e.getMessage(), e);
            throw new RuntimeException(MessageConstants.SERVER_ERROR, e);
        }
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPasswordHash(user.getPasswordHash());
        dto.setProvider(user.getProvider());
        dto.setProviderId(user.getProviderId());
        dto.setStatus(user.getStatus().name());
        return dto;
    }
    
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "null";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***@" + (atIndex > 0 ? email.substring(atIndex + 1) : "");
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }
} 