package com.example.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.concurrent.RejectedExecutionException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<String> handleRejectedExecutionException(RejectedExecutionException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Server is overloaded. Please try again later.");
    }
    // Add other exception handlers as needed...
} 