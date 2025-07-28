package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private boolean success;
    private T data;
    private String errorMessage;
    private int statusCode;

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null, 200);
    }

    public static <T> Result<T> success(T data, int statusCode) {
        return new Result<>(true, data, null, statusCode);
    }

    public static <T> Result<T> error(String message, int code) {
        return new Result<>(false, null, message, code);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, null, message, 500);
    }
} 