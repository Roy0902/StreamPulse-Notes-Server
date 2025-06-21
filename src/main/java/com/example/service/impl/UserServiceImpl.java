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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final IdGenerator snowflake = IdGenerators.newSnowflakeIdGenerator();

    @Override
    public ApiResponse<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
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
    }

    @Override
    @Transactional
    public ApiResponse<Void> register(UserDTO userDTO) throws InterruptedException {
        try {
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

            userRepository.save(user);
            return ApiResponse.success(MessageConstants.REGISTRATION_SUCCESSFUL);

        } catch (InterruptedException e) {
            throw new InterruptedException(MessageConstants.REGISTRATION_FAILED + ": " + e.getMessage());
        }
    }

    @Override
    public UserDTO getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(String.format(MessageConstants.USER_NOT_FOUND, userId)));
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(String userId, UserDTO userDTO) {
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
} 