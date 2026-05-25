package com.ezielnik.api.auth;

public record RefreshResponse(String token, String refreshToken) {
}
