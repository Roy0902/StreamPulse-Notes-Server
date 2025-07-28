package com.example.exception;

public class LoginException extends RuntimeException {
    private final int statusCode;
    
    public LoginException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public LoginException(String message) {
        super(message);
        this.statusCode = 500;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
} 