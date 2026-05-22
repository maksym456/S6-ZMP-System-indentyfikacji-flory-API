package com.ezielnik.api.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private boolean verified;
    private boolean active;
    private boolean admin;
    private boolean emailTwoFactorEnabled;
    private String warning;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.verified = user.isVerified();
        this.active = user.isActive();
        this.admin = user.isAdmin();
        this.emailTwoFactorEnabled = user.isEmailTwoFactorEnabled();

        if (!user.isVerified()) {
            this.warning = "Your email is not verified. Please verify your email to unlock all features.";
        }
    }

    public UserResponse() {
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

    public boolean isVerified() {
        return verified;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isEmailTwoFactorEnabled() {
        return emailTwoFactorEnabled;
    }

    public String getWarning() {
        return warning;
    }
}