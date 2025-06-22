package com.example.service;

import com.example.common.ApiResponse;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.UserDTO;

import java.util.concurrent.CompletableFuture;

public interface UserService {
    // Synchronous methods
    ApiResponse<LoginResponseDTO> login(LoginRequestDTO loginRequest);
    ApiResponse<Void> signUp(UserDTO userDTO) throws InterruptedException;
    UserDTO getUserById(String userId);
    UserDTO updateUser(String userId, UserDTO userDTO);
    
    // OTP Methods
    ApiResponse<Void> sendOtpForEmailVerification(String email);
    ApiResponse<Void> verifyOtpAndActivateUser(String email, String otpCode);
    
    // Async methods for better performance
    CompletableFuture<ApiResponse<LoginResponseDTO>> loginAsync(LoginRequestDTO loginRequest);
    CompletableFuture<ApiResponse<Void>> signUpAsync(UserDTO userDTO);
    CompletableFuture<UserDTO> getUserByIdAsync(String userId);
    CompletableFuture<ApiResponse<Void>> sendOtpForEmailVerificationAsync(String email);
    CompletableFuture<ApiResponse<Void>> verifyOtpAndActivateUserAsync(String email, String otpCode);
} 