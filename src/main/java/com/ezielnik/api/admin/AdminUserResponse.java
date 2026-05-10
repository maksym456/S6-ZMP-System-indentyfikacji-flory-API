package com.ezielnik.api.admin;

import com.ezielnik.api.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminUserResponse {

    private final UUID id;
    private final String email;
    private final String username;
    private final boolean active;
    private final boolean verified;
    private final boolean admin;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public AdminUserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.active = user.isActive();
        this.verified = user.isVerified();
        this.admin = user.isAdmin();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isAdmin() {
        return admin;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}