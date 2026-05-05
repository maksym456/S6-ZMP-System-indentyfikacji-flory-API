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
import org.springframework.web.bind.annotation.ResponseStatus;

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
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.me(UUID.fromString(jwt.getSubject()));
        return new UserResponse(user);
    }

    @Operation(summary = "Verify user email", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token) {
        return userService.verifyEmail(token);
    }

    @Operation(summary = "Resend verification email", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification email resent if account exists"),
            @ApiResponse(responseCode = "400", description = "Email is required")
    })
    @PostMapping("/resend-verification")
    public String resendVerificationEmail(@RequestParam String email) {
        return userService.resendVerificationEmail(email);
    }
}