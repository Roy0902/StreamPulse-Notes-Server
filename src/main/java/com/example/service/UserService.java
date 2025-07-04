package com.example.service;

import com.example.common.ApiResponse;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.SignupRequestDTO;
import com.example.dto.UserDTO;

import java.util.concurrent.CompletableFuture;

public interface UserService {
    // Read operations (synchronous)
    CompletableFuture<ApiResponse<LoginResponseDTO>> login(LoginRequestDTO loginRequest);
    UserDTO getUserById(String userId);
    
    // Write operations
    CompletableFuture<ApiResponse<Void>> sendOtpForEmailVerification(String email);
    CompletableFuture<ApiResponse<Void>> signUp(SignupRequestDTO signupRequest);
    CompletableFuture<ApiResponse<Void>> verifyOtpAndActivateUser(String email, String otpCode);
    CompletableFuture<UserDTO> updateUser(String userId, UserDTO userDTO);
} 