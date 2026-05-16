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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class JwtService {
    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    public String generateToken(User user) {
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

    public String generatePasswordResetToken(User user) {
        long now = System.currentTimeMillis();
        long resetExpirationMs = 15 * 60 * 1000; // 15 minutes

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("purpose", "password_reset")
                .claim("email", user.getEmail())
                .claim("passwordVersion",
                        createPasswordVersion(user))
                .issuedAt(new Date(now))
                .expiration(new Date(now + resetExpirationMs))
                .signWith(key)
                .compact();
    }

    public UUID extractPasswordResetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get("purpose", String.class);

            if (!"password_reset".equals(purpose)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token");
            }

            return UUID.fromString(claims.getSubject());

        } catch (ExpiredJwtException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset link has expired");

        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token");
        }
    }
    public void validatePasswordResetTokenForUser(String token, User user) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get("purpose", String.class);

            if (!"password_reset".equals(purpose)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token");
            }

            String tokenPasswordVersion = claims.get("passwordVersion", String.class);
            String currentPasswordVersion = createPasswordVersion(user);

            if (tokenPasswordVersion == null || !tokenPasswordVersion.equals(currentPasswordVersion)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset link is no longer valid");
            }

        } catch (ExpiredJwtException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset link has expired");

        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token");
        }
    }
    private String createPasswordVersion(User user) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(keySpec);

            byte[] hash = mac.doFinal(user.getPasswordHash().getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create password reset token");
        }
    }
}