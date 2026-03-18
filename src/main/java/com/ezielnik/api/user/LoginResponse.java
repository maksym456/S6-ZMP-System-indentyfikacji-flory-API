package com.ezielnik.api.user;

import java.util.UUID;

public class LoginResponse {

    private UUID id;
    private String username;
    private String email;
    private String token;

    public LoginResponse(User user, String token) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.token = token;
    }
    public LoginResponse() {
    }

    public String getToken() {
        return token;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}