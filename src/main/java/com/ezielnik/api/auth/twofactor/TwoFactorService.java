package com.ezielnik.api.auth.twofactor;

import com.ezielnik.api.auth.EmailService;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TwoFactorService {

    private final TwoFactorCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public TwoFactorService(TwoFactorCodeRepository codeRepository,
                            UserRepository userRepository,
                            EmailService emailService,
                            PasswordEncoder passwordEncoder) {
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendEmailCode(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        codeRepository.invalidateAllForUser(userId);

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        TwoFactorCode record = new TwoFactorCode();
        record.setUser(user);
        record.setCodeHash(passwordEncoder.encode(code));
        record.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        codeRepository.save(record);

        emailService.sendTwoFactorCode(user.getEmail(), code);
    }

    @Transactional
    public User verifyEmailCode(UUID userId, String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is required");
        }

        TwoFactorCode record = codeRepository
                .findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByExpiresAtDesc(userId, OffsetDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code has expired or was already used"));

        if (!passwordEncoder.matches(code, record.getCodeHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
        }

        record.setUsed(true);
        codeRepository.save(record);

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
