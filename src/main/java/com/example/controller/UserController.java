package com.example.controller;

import com.example.common.ApiResponse;
import com.example.common.JwtContextUtil;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.OtpRequestDTO;
import com.example.dto.OtpVerificationDTO;
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
        ApiResponse<LoginResponseDTO> response = userService.login(loginRequest);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody UserDTO userDTO) {
        try {
            ApiResponse<Void> response = userService.signUp(userDTO);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (InterruptedException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("Registration failed"));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequestDTO otpRequest) {
        ApiResponse<Void> response = userService.sendOtpForEmailVerification(otpRequest.getEmail());
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OtpVerificationDTO otpVerification) {
        ApiResponse<Void> response = userService.verifyOtpAndActivateUser(otpVerification.getEmail(), otpVerification.getOtpCode());
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable String userId) {
        try {
            UserDTO user = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable String userId, @Valid @RequestBody UserDTO userDTO) {
        try {
            UserDTO updatedUser = userService.updateUser(userId, userDTO);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Asynchronous endpoints for better performance
    @PostMapping("/async/login")
    public CompletableFuture<ResponseEntity<ApiResponse<LoginResponseDTO>>> loginAsync(@Valid @RequestBody LoginRequestDTO loginRequest) {
        return userService.loginAsync(loginRequest)
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @PostMapping("/async/signup")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> signupAsync(@Valid @RequestBody UserDTO userDTO) {
        return userService.signUpAsync(userDTO)
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @PostMapping("/async/send-otp")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> sendOtpAsync(@Valid @RequestBody OtpRequestDTO otpRequest) {
        return userService.sendOtpForEmailVerificationAsync(otpRequest.getEmail())
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @PostMapping("/async/verify-otp")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> verifyOtpAsync(@Valid @RequestBody OtpVerificationDTO otpVerification) {
        return userService.verifyOtpAndActivateUserAsync(otpVerification.getEmail(), otpVerification.getOtpCode())
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @GetMapping("/async/{userId}")
    public CompletableFuture<ResponseEntity<ApiResponse<UserDTO>>> getUserByIdAsync(@PathVariable String userId) {
        return userService.getUserByIdAsync(userId)
                .thenApply(user -> ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user)))
                .exceptionally(throwable -> ResponseEntity.notFound().build());
    }
} 