package com.example.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private String refreshToken;
} 