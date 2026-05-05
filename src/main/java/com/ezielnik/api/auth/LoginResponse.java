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
    private boolean verified;
    private boolean admin;
    private String token;

    public LoginResponse(String message, User user, String token) {
        this.message = message;
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.verified = user.isVerified();
        this.token = token;

        if (!user.isVerified()) {
            this.warning = "Email not verified";
        }
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

    public boolean isVerified() {
        return verified;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getToken() {
        return token;
    }
}