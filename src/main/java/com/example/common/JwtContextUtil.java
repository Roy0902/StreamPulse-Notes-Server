package com.example.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class JwtContextUtil {
    
    private final JwtUtil jwtUtil;
    
    public JwtContextUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Get current user ID from JWT token
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            // Extract from JWT token if available
            String token = getCurrentToken();
            if (token != null) {
                return jwtUtil.extractUserId(token);
            }
        }
        return null;
    }
    
    /**
     * Get current username from JWT token
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            return authentication.getName();
        }
        return null;
    }
    
    /**
     * Extract user ID from JWT token string
     */
    public String extractUserIdFromToken(String token) {
        return jwtUtil.extractUserId(token);
    }
    
    /**
     * Extract username from JWT token string
     */
    public String extractUsernameFromToken(String token) {
        return jwtUtil.extractUsername(token);
    }
    
    
    private String getCurrentToken() {
        // This would need to be implemented based on how you store the token
        // For now, returning null - you might want to store it in a ThreadLocal or similar
        return null;
    }
} 