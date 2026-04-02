package com.ezielnik.api.user;

import java.util.UUID;

public class RegisterResponse {

    private final String message;
    private UUID id;
    private String username;
    private String email;


    public RegisterResponse(String message, UUID id, String username, String email) {
        this.message = message;
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public String getMessage() { return message; }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}