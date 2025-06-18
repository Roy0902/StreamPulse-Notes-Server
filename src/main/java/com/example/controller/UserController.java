package com.example.controller;

import com.example.common.ApiResponse;
import com.example.common.MessageConstants;
import com.example.dto.UserDTO;
import com.example.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    @GetMapping
    public ApiResponse<List<UserDTO>> getAllUsers() {
        try {
            List<UserDTO> users = userService.getAllUsers();
            return ApiResponse.withData(200, MessageConstants.SUCCESS, users);
        } catch (Exception e) {
            return ApiResponse.withoutData(500, String.format(MessageConstants.USER_RETRIEVE_ERROR, e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserDTO> getUserById(@PathVariable String userId) {
        try {
            UserDTO user = userService.getUserById(userId);
            return ApiResponse.withData(200, MessageConstants.SUCCESS, user);
        } catch (RuntimeException e) {
            return ApiResponse.withoutData(404, String.format(MessageConstants.USER_NOT_FOUND, userId));
        }
    }

    @PostMapping
    public ApiResponse<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        try {
            UserDTO createdUser = userService.createUser(userDTO);
            return ApiResponse.withData(201, MessageConstants.USER_CREATED, createdUser);
        } catch (Exception e) {
            return ApiResponse.withoutData(400, String.format(MessageConstants.USER_CREATE_ERROR, e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserDTO> updateUser(@PathVariable String userId, @RequestBody UserDTO userDTO) {
        try {
            UserDTO updatedUser = userService.updateUser(userId, userDTO);
            return ApiResponse.withData(200, MessageConstants.USER_UPDATED, updatedUser);
        } catch (RuntimeException e) {
            return ApiResponse.withoutData(404, String.format(MessageConstants.USER_NOT_FOUND, userId));
        }
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return ApiResponse.withoutData(200, MessageConstants.USER_DELETED);
        } catch (RuntimeException e) {
            return ApiResponse.withoutData(404, String.format(MessageConstants.USER_NOT_FOUND, userId));
        }
    }
} 