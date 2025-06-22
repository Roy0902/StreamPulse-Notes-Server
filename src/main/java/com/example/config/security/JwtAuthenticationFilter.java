package com.example.config.security;

import com.example.common.JwtUtil;
import com.example.common.MessageConstants;
import com.example.entity.User;
import com.example.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        final String userId;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        
        try {
            username = jwtUtil.extractUsername(jwt);
            userId = jwtUtil.extractUserId(jwt);
            
            if (username != null && userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.warn("[{}] JWT authentication failed - UserID: {}, Username: {}, Reason: User not found, IP: {}", 
                               timestamp, userId, username, getClientIP(request));
                    sendErrorResponse(response, 401, MessageConstants.USER_NOT_FOUND_AUTH);
                    return;
                }
                
                // Verify username matches (additional security check)
                if (!user.getUsername().equals(username)) {
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.warn("[{}] JWT authentication failed - UserID: {}, Username mismatch: {} vs {}, IP: {}", 
                               timestamp, userId, username, user.getUsername(), getClientIP(request));
                    sendErrorResponse(response, 401, MessageConstants.INVALID_TOKEN);
                    return;
                }
                
                if (jwtUtil.validateToken(jwt, username)) {
                    
                    UserDetails userDetails = createUserDetails(user);
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        userDetails.getAuthorities()
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    String timestamp = LocalDateTime.now().format(formatter);
                    logger.warn("[{}] JWT authentication failed - UserID: {}, Username: {}, Reason: Invalid token, IP: {}", 
                               timestamp, userId, username, getClientIP(request));
                    sendErrorResponse(response, 401, MessageConstants.INVALID_TOKEN);
                    return;
                }
            }
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(formatter);
            logger.error("[{}] JWT token validation system error - IP: {}, Error: {}", 
                        timestamp, getClientIP(request), e.getMessage(), e);
            sendErrorResponse(response, 401, MessageConstants.MALFORMED_TOKEN);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String errorJson = String.format(
            "{\"code\":%d,\"message\":\"%s\",\"data\":null}",
            status,
            message
        );
        
        response.getWriter().write(errorJson);
    }
    
    private UserDetails createUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .authorities(new ArrayList<>()) 
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
} 