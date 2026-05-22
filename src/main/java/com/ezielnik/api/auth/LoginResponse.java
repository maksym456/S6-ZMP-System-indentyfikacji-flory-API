package com.ezielnik.api.auth;

import com.ezielnik.api.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String message;
    private String warning;
    private UUID id;
    private String username;
    private String email;
    private Boolean verified;
    private Boolean admin;
    private String token;
    private Boolean requiresTwoFactor;
    private String preAuthToken;

    public LoginResponse(String message, User user, String token) {
        this.message = message;
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.verified = user.isVerified();
        this.token = token;
        this.admin = user.isAdmin();

        if (!user.isVerified()) {
            this.warning = "Email not verified";
        }
    }

    public static LoginResponse twoFactorRequired(String preAuthToken) {
        LoginResponse r = new LoginResponse();
        r.message = "Two-factor authentication required";
        r.requiresTwoFactor = true;
        r.preAuthToken = preAuthToken;
        return r;
    }

    public LoginResponse() {
    }

    public String getMessage() {
        return message;
    }

    public String getWarning() {
        return warning;
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

    public Boolean isVerified() {
        return verified;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public String getToken() {
        return token;
    }

    public Boolean getRequiresTwoFactor() {
        return requiresTwoFactor;
    }

    public String getPreAuthToken() {
        return preAuthToken;
    }
}