package com.ezielnik.api.user;

import com.ezielnik.api.auth.EmailService;
import com.ezielnik.api.auth.JwtService;
import com.ezielnik.api.auth.LoginRequest;
import com.ezielnik.api.auth.RegisterRequest;
import com.ezielnik.api.auth.RegisterResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import com.ezielnik.api.auth.ForgotPasswordRequest;
import com.ezielnik.api.auth.ResetPasswordRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one capital letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one number");
        }

        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one special symbol");
        }
    }

    public RegisterResponse register(RegisterRequest request) {
        if (request.getUsername() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        if (request.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        validatePasswordStrength(request.getPassword());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        String verificationToken = jwtService.generateEmailVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        return new RegisterResponse(
                "User registered successfully. Please verify your email.",
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );
    }

    public User login(LoginRequest request) {
        if (request.getLogin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username or email is required");
        }

        if (request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        User user = userRepository
                .findByEmailOrUsername(request.getLogin(), request.getLogin())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid login or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid login or password");
        }

        return user;
    }

    public User me(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public String verifyEmail(String token) {
        UUID userId = jwtService.extractEmailVerificationUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isVerified()) {
            return "Email already verified";
        }

        user.setVerified(true);
        userRepository.save(user);

        return "Email verified successfully";
    }

    public String resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        User user = userRepository
                .findByEmailOrUsername(email.trim().toLowerCase(), email.trim().toLowerCase())
                .orElse(null);

        if (user == null) {
            return "If an account with this email exists and is not verified, a verification email has been sent.";
        }

        if (user.isVerified()) {
            return "Email is already verified.";
        }

        String verificationToken = jwtService.generateEmailVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return "Verification email sent.";
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository
                .findByEmailOrUsername(email, email)
                .orElse(null);

        if (user == null || !user.isActive()) {
            return "If an account with this email exists, a password reset email has been sent.";
        }

        String resetToken = jwtService.generatePasswordResetToken(user);
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        return "If an account with this email exists, a password reset email has been sent.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is required");
        }

        validatePasswordStrength(request.getNewPassword());

        UUID userId = jwtService.extractPasswordResetUserId(request.getToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        jwtService.validatePasswordResetTokenForUser(request.getToken(), user);

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be the same as the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password reset successfully";
    }
}