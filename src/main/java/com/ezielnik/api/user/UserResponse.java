package com.ezielnik.api.user;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public UserResponse() {
    }

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

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isEmailTwoFactorEnabled() {
        return emailTwoFactorEnabled;
    }

    public void setEmailTwoFactorEnabled(boolean emailTwoFactorEnabled) {
        this.emailTwoFactorEnabled = emailTwoFactorEnabled;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
