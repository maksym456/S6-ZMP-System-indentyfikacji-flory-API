package com.ezielnik.api.auth;

import com.ezielnik.api.user.User;
import com.fasterxml.jackson.annotation.JsonCreator;
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
    private String refreshToken;
    private Boolean requiresTwoFactor;
    private String preAuthToken;

    public LoginResponse(String message, User user, String token, String refreshToken) {
        this.message = message;
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.verified = user.isVerified();
        this.token = token;
        this.refreshToken = refreshToken;
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

    @JsonCreator
    public LoginResponse() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
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

    public Boolean isVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Boolean getRequiresTwoFactor() {
        return requiresTwoFactor;
    }

    public void setRequiresTwoFactor(Boolean requiresTwoFactor) {
        this.requiresTwoFactor = requiresTwoFactor;
    }

    public String getPreAuthToken() {
        return preAuthToken;
    }

    public void setPreAuthToken(String preAuthToken) {
        this.preAuthToken = preAuthToken;
    }
}
