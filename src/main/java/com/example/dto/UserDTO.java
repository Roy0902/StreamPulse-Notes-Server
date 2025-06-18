package com.example.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String email;
    private String passwordHash;
} 