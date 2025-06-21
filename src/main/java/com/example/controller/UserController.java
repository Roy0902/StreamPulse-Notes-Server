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

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = userService.login(loginRequest);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@Valid @RequestBody UserDTO userDTO) {
        try {
            UserDTO registeredUser = userService.register(userDTO);
            if (registeredUser != null) {
                return ResponseEntity.ok(ApiResponse.success("User registered successfully", registeredUser));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.badRequest("Email already exists"));
            }
        } catch (InterruptedException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.serverError("Registration failed"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
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

    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody UserDTO userDTO) throws InterruptedException {
        UserDTO createdUser = userService.createUser(userDTO);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", createdUser));
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

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 