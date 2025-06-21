package com.example.service;

import com.example.common.ApiResponse;
import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.UserDTO;

public interface UserService {
    ApiResponse<LoginResponseDTO> login(LoginRequestDTO loginRequest);
    ApiResponse<Void> register(UserDTO userDTO) throws InterruptedException;
    UserDTO getUserById(String userId);
    UserDTO updateUser(String userId, UserDTO userDTO);
} 