package com.example.service.impl;

import com.example.common.JwtUtil;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.UserDTO;
import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import com.github.rholder.fauxflake.IdGenerators;
import com.github.rholder.fauxflake.api.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final IdGenerator snowflake = IdGenerators.newSnowflakeIdGenerator();

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {
            LoginResponseDTO response = new LoginResponseDTO();
            response.setStatusCode(401);
            response.setMessage("Invalid email or password");
            return response;
        }
        
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            LoginResponseDTO response = new LoginResponseDTO();
            response.setStatusCode(401);
            response.setMessage("Invalid email or password");
            return response;
        }
        
        LoginResponseDTO response = new LoginResponseDTO();
        response.setStatusCode(201);
        response.setToken(JwtUtil.generateToken(user.getUsername(), user.getEmail()));
        response.setRefreshToken(JwtUtil.generateRefreshToken(user.getUsername(), user.getEmail()));
        response.setMessage("Login successful");
        
        return response;
    }

    @Override
    @Transactional
    public UserDTO register(UserDTO userDTO) throws InterruptedException {
        try {
            User existingUser = userRepository.findByEmail(userDTO.getEmail());
            if (existingUser != null) {
                return null;
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
            User savedUser = userRepository.save(user);
            UserDTO response = new UserDTO();
            response.setUserId(savedUser.getUserId());
            response.setUsername(savedUser.getUsername());
            response.setEmail(savedUser.getEmail());
            response.setProvider(savedUser.getProvider());
            response.setProviderId(savedUser.getProviderId());
            response.setStatus(savedUser.getStatus().name());
            return response;
        } catch (InterruptedException e) {
            throw new InterruptedException("Registration failed: " + e.getMessage());
        }
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) throws InterruptedException {
        User user = new User();
        String userId = snowflake.generateId(1000).asString();
        user.setUserId(userId);
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPasswordHash(passwordEncoder.encode(userDTO.getPasswordHash()));
        user.setProvider(userDTO.getProvider());
        user.setProviderId(userDTO.getProviderId());
        user.setStatus(User.Status.normal);
        return convertToDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDTO updateUser(String userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        if (userDTO.getPasswordHash() != null && !userDTO.getPasswordHash().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(userDTO.getPasswordHash()));
        }
        user.setProvider(userDTO.getProvider());
        user.setProviderId(userDTO.getProviderId());
        
        return convertToDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
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