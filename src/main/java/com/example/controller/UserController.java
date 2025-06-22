package com.example.controller;

import com.example.common.ApiResponse;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
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
    public ResponseEntity<ApiResponse<Void>> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Email is required"));
        }
        
        ApiResponse<Void> response = userService.sendOtpForEmailVerification(email.trim());
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otpCode = request.get("otpCode");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Email is required"));
        }
        
        if (otpCode == null || otpCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "OTP code is required"));
        }
        
        ApiResponse<Void> response = userService.verifyOtpAndActivateUser(email.trim(), otpCode.trim());
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
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> sendOtpAsync(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(ApiResponse.error(400, "Email is required"))
            );
        }
        
        return userService.sendOtpForEmailVerificationAsync(email.trim())
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @PostMapping("/async/verify-otp")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> verifyOtpAsync(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otpCode = request.get("otpCode");
        
        if (email == null || email.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(ApiResponse.error(400, "Email is required"))
            );
        }
        
        if (otpCode == null || otpCode.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(ApiResponse.error(400, "OTP code is required"))
            );
        }
        
        return userService.verifyOtpAndActivateUserAsync(email.trim(), otpCode.trim())
                .thenApply(response -> ResponseEntity.status(response.getCode()).body(response));
    }

    @GetMapping("/async/{userId}")
    public CompletableFuture<ResponseEntity<ApiResponse<UserDTO>>> getUserByIdAsync(@PathVariable String userId) {
        return userService.getUserByIdAsync(userId)
                .thenApply(user -> ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user)))
                .exceptionally(throwable -> ResponseEntity.notFound().build());
    }
} 