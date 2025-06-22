package com.example.service;

import com.example.common.ApiResponse;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.SignupRequestDTO;

import java.util.concurrent.CompletableFuture;

public interface UserService {
    // Synchronous methods
    ApiResponse<LoginResponseDTO> login(LoginRequestDTO loginRequest);
    ApiResponse<Void> signUp(SignupRequestDTO userDTO) throws InterruptedException;
    
    // OTP Methods
    ApiResponse<Void> sendOtpForEmailVerification(String email);
    ApiResponse<Void> verifyOtpAndActivateUser(String email, String otpCode);
    
    // Async methods for better performance
    CompletableFuture<ApiResponse<LoginResponseDTO>> loginAsync(LoginRequestDTO loginRequest);
    CompletableFuture<ApiResponse<Void>> signUpAsync(UserDTO userDTO);
    CompletableFuture<ApiResponse<Void>> sendOtpForEmailVerificationAsync(String email);
    CompletableFuture<ApiResponse<Void>> verifyOtpAndActivateUserAsync(String email, String otpCode);
} 