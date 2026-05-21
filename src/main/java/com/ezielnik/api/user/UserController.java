package com.ezielnik.api.user;

import com.ezielnik.api.auth.*;
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
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/users")
public class UserController {

    private final JwtService jwtService;
    private final UserService userService;

    public UserController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
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
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Invalid login or password")
    })
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = userService.login(request);
        String token = jwtService.generateToken(user);
        return new LoginResponse("Logged in successfully", user, token);
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
}