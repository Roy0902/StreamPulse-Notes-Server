package com.example.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private int statusCode;
    private String message;
    private String token;
    private String refreshToken;
} 