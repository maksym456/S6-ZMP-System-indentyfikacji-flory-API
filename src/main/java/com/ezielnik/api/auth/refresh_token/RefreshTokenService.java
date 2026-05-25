package com.ezielnik.api.auth.refresh_token;

import com.ezielnik.api.auth.JwtProperties;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtProperties = jwtProperties;
    }

    public record TokenPair(User user, String refreshToken) {}

    @Transactional
    public String generate(User user) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawToken = user.getId().toString() + ":" + randomPart;
        String hash = sha256(randomPart);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(jwtProperties.getRefreshExpirationDays());
        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));
        return rawToken;
    }

    @Transactional
    public TokenPair validateAndRotate(String rawToken) {
        String[] parts = rawToken == null ? new String[0] : rawToken.split(":", 2);
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        UUID userId;
        try {
            userId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String hash = sha256(parts[1]);
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash);

        if (found.isEmpty()) {
            if (userRepository.existsById(userId)) {
                refreshTokenRepository.deleteByUserId(userId);
                log.warn("Refresh token reuse detected for user {} — all sessions revoked", userId);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        RefreshToken token = found.get();

        if (!token.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        User user = token.getUser();
        refreshTokenRepository.delete(token);
        String newRawToken = generate(user);
        return new TokenPair(user, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null) return;
        String[] parts = rawToken.split(":", 2);
        if (parts.length != 2) return;
        String hash = sha256(parts[1]);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
