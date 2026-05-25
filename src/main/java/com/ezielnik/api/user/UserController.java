package com.ezielnik.api.user;

import com.ezielnik.api.auth.*;
import com.ezielnik.api.auth.refresh_token.RefreshRequest;
import com.ezielnik.api.auth.refresh_token.RefreshResponse;
import com.ezielnik.api.auth.refresh_token.RefreshTokenService;
import com.ezielnik.api.auth.two_factor_auth.TwoFactorService;
import com.ezielnik.api.auth.two_factor_auth.TwoFactorVerifyRequest;
import com.ezielnik.api.fcm.DeviceTokenRequest;
import com.ezielnik.api.fcm.FcmService;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/users")
public class UserController {

    private final JwtService jwtService;
    private final UserService userService;
    private final TwoFactorService twoFactorService;
    private final FcmService fcmService;
    private final RefreshTokenService refreshTokenService;

    public UserController(JwtService jwtService, UserService userService, TwoFactorService twoFactorService, FcmService fcmService, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.twoFactorService = twoFactorService;
        this.fcmService = fcmService;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Email or username already exists")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @Operation(summary = "Login and get JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, or 2FA required (requiresTwoFactor=true, preAuthToken returned instead of token)"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Invalid login or password"),
            @ApiResponse(responseCode = "403", description = "User account is inactive")
    })
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = userService.login(request);
        if (user.isEmailTwoFactorEnabled()) {
            String preAuthToken = jwtService.generatePreAuthToken(user);
            twoFactorService.sendEmailCode(user.getId());
            return LoginResponse.twoFactorRequired(preAuthToken);
        }
        return new LoginResponse("Logged in successfully", user, jwtService.generateToken(user), refreshTokenService.generate(user));
    }

    @Operation(summary = "Complete login with 2FA email code — requires pre-auth token from /login, not a regular JWT", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, full JWT returned"),
            @ApiResponse(responseCode = "400", description = "Code is required or invalid/expired"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid pre-auth token"),
            @ApiResponse(responseCode = "403", description = "Token is not a pre-auth token")
    })
    @PostMapping("/verify-2fa")
    public LoginResponse verifyTwoFactor(@AuthenticationPrincipal Jwt jwt,
                                         @RequestBody TwoFactorVerifyRequest request) {
        if (!"pre_auth".equals(jwt.getClaimAsString("purpose"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token for 2FA verification");
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = twoFactorService.verifyEmailCode(userId, request.getCode());
        return new LoginResponse("Logged in successfully", user, jwtService.generateToken(user), refreshTokenService.generate(user));
    }

    @Operation(summary = "Enable email 2FA", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email 2FA enabled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/2fa/email/enable")
    public ResponseEntity<String> enableEmailTwoFactor(@AuthenticationPrincipal Jwt jwt) {
        userService.enableEmailTwoFactor(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok("Email two-factor authentication enabled");
    }

    @Operation(summary = "Disable email 2FA", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email 2FA disabled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/2fa/disable")
    public ResponseEntity<String> disableEmailTwoFactor(@AuthenticationPrincipal Jwt jwt) {
        userService.disableEmailTwoFactor(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok("Email two-factor authentication disabled");
    }

    @Operation(summary = "Resend 2FA email code — requires pre-auth token from /login", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification code sent"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid pre-auth token"),
            @ApiResponse(responseCode = "403", description = "Token is not a pre-auth token")
    })
    @PostMapping("/2fa/send-email-code")
    public ResponseEntity<String> resendEmailCode(@AuthenticationPrincipal Jwt jwt) {
        if (!"pre_auth".equals(jwt.getClaimAsString("purpose"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token for 2FA operation");
        }
        twoFactorService.sendEmailCode(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok("Verification code sent");
    }

    @Operation(summary = "Get current user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.me(UUID.fromString(jwt.getSubject()));
        return new UserResponse(user);
    }

    @Operation(summary = "Delete current user account", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/me")
    public String deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        return userService.deleteMyAccount(UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Verify user email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token) {
        return userService.verifyEmail(token);
    }

    @Operation(summary = "Resend verification email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification email resent if account exists"),
            @ApiResponse(responseCode = "400", description = "Email is required")
    })
    @PostMapping("/resend-verification")
    public String resendVerificationEmail(@RequestParam String email) {
        return userService.resendVerificationEmail(email);
    }

    @Operation(summary = "Request password reset email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset email sent if account exists"),
            @ApiResponse(responseCode = "400", description = "Email is required")
    })
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return userService.forgotPassword(request);
    }

    @Operation(summary = "Show password reset form")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTML reset form returned"),
            @ApiResponse(responseCode = "400", description = "Token is required")
    })
    @GetMapping("/reset-password")
    public ResponseEntity<String> showResetPasswordForm(@RequestParam String token) {
        String escapedToken = HtmlUtils.htmlEscape(token);

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Reset password</title>
                <meta charset="UTF-8">
                <style>
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-family: Arial, sans-serif;
                        background: #f5f5f5;
                    }

                    .card {
                        width: 100%;
                        max-width: 400px;
                        padding: 32px;
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
                        text-align: center;
                    }

                    h2 {
                        margin-top: 0;
                        margin-bottom: 24px;
                    }

                    label {
                        display: block;
                        margin-bottom: 8px;
                        text-align: left;
                    }

                    input {
                        width: 100%;
                        box-sizing: border-box;
                        padding: 10px;
                        margin-bottom: 20px;
                        border: 1px solid #ccc;
                        border-radius: 6px;
                    }

                    button {
                        width: 100%;
                        padding: 12px;
                        border: none;
                        border-radius: 6px;
                        cursor: pointer;
                        font-size: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Reset your password</h2>
                    <form method="post" action="/users/reset-password">
                        <input type="hidden" name="token" value="__TOKEN__" />
                        <label for="newPassword">New password</label>
                        <input type="password" id="newPassword" name="newPassword" required />
                        <button type="submit">Reset password</button>
                    </form>
                </div>
            </body>
            </html>
            """.replace("__TOKEN__", escapedToken);

        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @Operation(summary = "Reset password using JSON body")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset token"),
            @ApiResponse(responseCode = "403", description = "User account is inactive"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String resetPassword(@RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request);
    }

    @Operation(summary = "Reset password using form")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset token"),
            @ApiResponse(responseCode = "403", description = "User account is inactive"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String resetPasswordForm(@RequestParam String token, @RequestParam String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword(newPassword);

        return userService.resetPassword(request);
    }

    @Operation(summary = "Register FCM device token for push notifications", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device token registered"),
            @ApiResponse(responseCode = "400", description = "Token is required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/me/fcm-token")
    public ResponseEntity<String> registerFcmToken(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestBody DeviceTokenRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userService.me(userId);
        fcmService.registerToken(userId, request.getToken(), user);
        return ResponseEntity.ok("Device token registered");
    }

    @Operation(summary = "Unregister FCM device token", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device token removed"),
            @ApiResponse(responseCode = "400", description = "Token is required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/me/fcm-token")
    public ResponseEntity<String> unregisterFcmToken(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam String token) {
        fcmService.unregisterToken(UUID.fromString(jwt.getSubject()), token);
        return ResponseEntity.ok("Device token removed");
    }

    @Operation(summary = "Refresh access token using a refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token and refresh token returned"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public RefreshResponse refresh(@RequestBody RefreshRequest request) {
        RefreshTokenService.TokenPair pair = refreshTokenService.validateAndRotate(request.getRefreshToken());
        return new RefreshResponse(jwtService.generateToken(pair.user()), pair.refreshToken());
    }

    @Operation(summary = "Logout and invalidate refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }
}