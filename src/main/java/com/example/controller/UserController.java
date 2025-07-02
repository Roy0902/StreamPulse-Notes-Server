package com.example.controller;

import com.example.common.ApiResponse;
import com.example.common.JwtContextUtil;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.OtpRequestDTO;
import com.example.dto.OtpVerificationDTO;
import com.example.dto.SignupRequestDTO;
import com.example.dto.UserDTO;
import com.example.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtContextUtil jwtContextUtil;

    // Synchronous endpoints
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        CompletableFuture<ApiResponse<LoginResponseDTO>> response = userService.login(loginRequest);
        return ResponseEntity.status(response.getNow(null).getCode()).body(response.getNow(null));
    }

    @PostMapping("/sign-up")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> signup(@Valid @RequestBody SignupRequestDTO signupRequest) {
        return userService.signUp(signupRequest)
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @PostMapping("/send-otp")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> sendOtp(@Valid @RequestBody OtpRequestDTO otpRequest) {
        return userService.sendOtpForEmailVerification(otpRequest.getEmail())
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @PostMapping("/verify-otp")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> verifyOtp(@Valid @RequestBody OtpVerificationDTO otpVerification) {
        return userService.verifyOtpAndActivateUser(otpVerification.getEmail(), otpVerification.getOtpCode())
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentUser() {
        String userId = jwtContextUtil.getCurrentUserId();
        String username = jwtContextUtil.getCurrentUsername();
        
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "User not authenticated"));
        }
        
        Map<String, String> userInfo = Map.of(
            "userId", userId,
            "username", username != null ? username : "Unknown"
        );
        
        return ResponseEntity.ok(ApiResponse.success("Current user info retrieved", userInfo));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable String userId) {
        try {
            UserDTO user = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
} 