package com.ezielnik.api.auth;

import com.ezielnik.api.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import java.util.UUID;

@Service
public class JwtService {
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String extractUserId(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    public String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getExpirationMs()))
                .signWith(key)
                .compact();
    }
    public String generateEmailVerificationToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );

        long now = System.currentTimeMillis();
        long verificationExpirationMs = 15 * 60 * 1000; // 15 minutes

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("purpose", "email_verification")
                .claim("email", user.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + verificationExpirationMs))
                .signWith(key)
                .compact();
    }

    public UUID extractEmailVerificationUserId(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get("purpose", String.class);

            if (!"email_verification".equals(purpose)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token");
            }

            return UUID.fromString(claims.getSubject());

        } catch (ExpiredJwtException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification link has expired");

        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token");
        }
    }
}