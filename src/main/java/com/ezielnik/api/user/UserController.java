package com.ezielnik.api.user;

import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final JwtService jwtService;
    private final UserService userService;

    public UserController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = userService.login(request);
        String token = jwtService.generateToken(user);
        return new LoginResponse(user, token);
    }
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.me(UUID.fromString(jwt.getSubject()));
        return new UserResponse(user);
    }
}