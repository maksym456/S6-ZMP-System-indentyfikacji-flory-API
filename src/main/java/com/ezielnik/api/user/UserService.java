package com.ezielnik.api.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        return new RegisterResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
    }
    public User login(LoginRequest request) {
        User user = userRepository
                .findByEmailOrUsername(request.getLogin(), request.getLogin())
                .orElseThrow(() -> new RuntimeException("Invalid login or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid login or password");
        }

        return user;
    }
}