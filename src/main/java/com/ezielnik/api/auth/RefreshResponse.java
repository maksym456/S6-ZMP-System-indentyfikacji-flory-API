package com.ezielnik.api.auth;

public class RefreshResponse {
    private final String token;
    private final String refreshToken;

    public RefreshResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
