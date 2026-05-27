package com.ezielnik.api.auth.refresh_token;

import com.ezielnik.api.auth.JwtProperties;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService service;

    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-unit-tests-only-must-be-at-least-32-characters-long");
        props.setRefreshExpirationDays(30);

        service = new RefreshTokenService(refreshTokenRepository, userRepository, props);

        user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.prePersist();
    }

    @Test
    void generate_savesTokenAndReturnsRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.generate(user);

        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(raw).startsWith(user.getId().toString() + ":");
        assertThat(saved.getTokenHash()).isNotNull();
        assertThat(saved.getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    void generate_hashIsNotPlaintextToken() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        String raw = service.generate(user);
        verify(refreshTokenRepository).save(captor.capture());

        String randomPart = raw.split(":", 2)[1];
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(randomPart);
    }

    @Test
    void validateAndRotate_validToken_returnsNewPairAndDeletesOld() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String raw = service.generate(user);

        RefreshToken stored = captureLastSaved();
        assertThat(stored).isNotNull();

        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.TokenPair pair = service.validateAndRotate(raw);

        verify(refreshTokenRepository).delete(stored);
        assertThat(pair.user()).isEqualTo(user);
        assertThat(pair.refreshToken()).startsWith(user.getId().toString() + ":");
        assertThat(pair.refreshToken()).isNotEqualTo(raw);
    }

    @Test
    void validateAndRotate_nullToken_throws401() {
        assertThatThrownBy(() -> service.validateAndRotate(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void validateAndRotate_malformedToken_throws401() {
        assertThatThrownBy(() -> service.validateAndRotate("notavalidtoken"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void validateAndRotate_unknownHash_existingUser_revokesAllSessions() {
        UUID userId = user.getId();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(userRepository.existsById(userId)).thenReturn(true);

        String fakeRaw = userId + ":someRandomPart";

        assertThatThrownBy(() -> service.validateAndRotate(fakeRaw))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid or expired refresh token");

        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    void validateAndRotate_unknownHash_unknownUser_throws401WithoutRevocation() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(userRepository.existsById(userId)).thenReturn(false);

        String fakeRaw = userId + ":someRandomPart";

        assertThatThrownBy(() -> service.validateAndRotate(fakeRaw))
                .isInstanceOf(ResponseStatusException.class);

        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void validateAndRotate_expiredToken_throws401() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String raw = service.generate(user);

        RefreshToken stored = captureLastSaved();
        // Simulate expired token by setting expiry in the past
        // We need to intercept the stored object and set expiry via reflection or create directly
        RefreshToken expired = new RefreshToken(user, stored.getTokenHash(), OffsetDateTime.now().minusDays(1));
        expired.prePersist();

        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.validateAndRotate(raw))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Refresh token expired");

        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void validateAndRotate_wrongUserId_throws401() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String raw = service.generate(user);

        RefreshToken stored = captureLastSaved();

        // Build a raw token with a different userId prefix but same hash
        User otherUser = new User();
        otherUser.setUsername("bob");
        otherUser.setEmail("bob@example.com");
        otherUser.setPasswordHash("hash");
        otherUser.prePersist();

        RefreshToken tokenForOther = new RefreshToken(otherUser, stored.getTokenHash(), OffsetDateTime.now().plusDays(30));
        tokenForOther.prePersist();

        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(tokenForOther));

        assertThatThrownBy(() -> service.validateAndRotate(raw))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void revoke_validToken_deletesRecord() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String raw = service.generate(user);
        RefreshToken stored = captureLastSaved();

        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        service.revoke(raw);

        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void revoke_nullToken_doesNothing() {
        service.revoke(null);
        verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void revokeAll_deletesAllTokensForUser() {
        UUID userId = user.getId();
        service.revokeAll(userId);
        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    private RefreshToken captureLastSaved() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
