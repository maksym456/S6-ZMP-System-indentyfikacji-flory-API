package com.ezielnik.api.auth;

import com.ezielnik.api.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-must-be-at-least-32-characters-long";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(3600_000);
        jwtService = new JwtService(props);

        user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("somehash");
        user.prePersist();
    }

    @Test
    void generateToken_containsSubjectAndClaims() {
        String token = jwtService.generateToken(user);
        String userId = jwtService.extractUserId(token);
        assertThat(userId).isEqualTo(user.getId().toString());
    }

    @Test
    void generateToken_isNotPreAuth() {
        String token = jwtService.generateToken(user);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertThat(claims.get("purpose")).isNull();
    }

    @Test
    void generatePreAuthToken_hasPurposeClaim() {
        String token = jwtService.generatePreAuthToken(user);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertThat(claims.get("purpose")).isEqualTo("pre_auth");
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    }

    @Test
    void extractEmailVerificationUserId_validToken_returnsId() {
        String token = jwtService.generateEmailVerificationToken(user);
        UUID id = jwtService.extractEmailVerificationUserId(token);
        assertThat(id).isEqualTo(user.getId());
    }

    @Test
    void extractEmailVerificationUserId_wrongPurpose_throws400() {
        String token = jwtService.generateToken(user);
        assertThatThrownBy(() -> jwtService.extractEmailVerificationUserId(token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void extractEmailVerificationUserId_expiredToken_throws400() {
        String expired = buildExpiredToken(user, "email_verification");
        assertThatThrownBy(() -> jwtService.extractEmailVerificationUserId(expired))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void extractEmailVerificationUserId_gibberish_throws400() {
        assertThatThrownBy(() -> jwtService.extractEmailVerificationUserId("not.a.jwt"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void extractPasswordResetUserId_validToken_returnsId() {
        String token = jwtService.generatePasswordResetToken(user);
        UUID id = jwtService.extractPasswordResetUserId(token);
        assertThat(id).isEqualTo(user.getId());
    }

    @Test
    void extractPasswordResetUserId_wrongPurpose_throws400() {
        String token = jwtService.generateToken(user);
        assertThatThrownBy(() -> jwtService.extractPasswordResetUserId(token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void extractPasswordResetUserId_expiredToken_throws400() {
        String expired = buildExpiredToken(user, "password_reset");
        assertThatThrownBy(() -> jwtService.extractPasswordResetUserId(expired))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validatePasswordResetTokenForUser_validToken_doesNotThrow() {
        String token = jwtService.generatePasswordResetToken(user);
        jwtService.validatePasswordResetTokenForUser(token, user);
    }

    @Test
    void validatePasswordResetTokenForUser_changedPassword_throws400() {
        String token = jwtService.generatePasswordResetToken(user);
        user.setPasswordHash("newhashafterchange");
        assertThatThrownBy(() -> jwtService.validatePasswordResetTokenForUser(token, user))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void validatePasswordResetTokenForUser_expiredToken_throws400() {
        String expired = buildExpiredToken(user, "password_reset");
        assertThatThrownBy(() -> jwtService.validatePasswordResetTokenForUser(expired, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expired");
    }

    private String buildExpiredToken(User u, String purpose) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long past = System.currentTimeMillis() - 10_000;
        return Jwts.builder()
                .subject(u.getId().toString())
                .claim("purpose", purpose)
                .claim("email", u.getEmail())
                .issuedAt(new Date(past))
                .expiration(new Date(past - 1))
                .signWith(key)
                .compact();
    }
}
