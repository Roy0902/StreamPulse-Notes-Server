package com.example.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    // Secure secret key - at least 256 bits (32 bytes) for HS256
    private static final String SECRET_KEY = "StreamPulse2024!@#$%^&*()_+QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890";
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    
    private static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_TOKEN_VALIDITY = 12 * 60 * 60 * 1000; // 12 hours
    private static final long REMEMBER_ME_REFRESH_TOKEN_VALIDITY = 5 * 24 * 60 * 60 * 1000; // 5 days

    public static String generateToken(String username, String email) {
        return generateToken(username, email, ACCESS_TOKEN_VALIDITY);
    }

    public static String generateRefreshToken(String username, String email) {
        return generateToken(username, email, REFRESH_TOKEN_VALIDITY);
    }

    public static String generateRefreshToken(String username, String email, boolean rememberMe) {
        long validity = rememberMe ? REMEMBER_ME_REFRESH_TOKEN_VALIDITY : REFRESH_TOKEN_VALIDITY;
        return generateToken(username, email, validity);
    }

    private static String generateToken(String username, String email, long validity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("type", validity == ACCESS_TOKEN_VALIDITY ? "access" : "refresh");
        claims.put("iat", new Date());
        return createToken(claims, username, validity);
    }

    private static String createToken(Map<String, Object> claims, String subject, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (username.equals(tokenUsername) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
} 