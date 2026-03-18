package com.ezielnik.api.user;

import java.util.UUID;

public class UserResponse {

    private final UUID id;
    private final String username;
    private final String email;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
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