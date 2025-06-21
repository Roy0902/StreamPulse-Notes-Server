package com.example.service;

import com.example.dto.LoginRequestDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.UserDTO;

import java.util.List;

public interface UserService {
    LoginResponseDTO login(LoginRequestDTO loginRequest);
    UserDTO register(UserDTO userDTO) throws InterruptedException;
    List<UserDTO> getAllUsers();
    UserDTO getUserById(String userId);
    UserDTO createUser(UserDTO userDTO) throws InterruptedException;
    UserDTO updateUser(String userId, UserDTO userDTO);
    void deleteUser(String userId);
} 