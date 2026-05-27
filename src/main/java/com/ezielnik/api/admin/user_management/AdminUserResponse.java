package com.ezielnik.api.admin.user_management;

import com.ezielnik.api.user.User;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminUserResponse {

    private UUID id;
    private String email;
    private String username;
    private boolean active;
    private boolean verified;
    private boolean admin;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

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

    @JsonCreator
    public AdminUserResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
