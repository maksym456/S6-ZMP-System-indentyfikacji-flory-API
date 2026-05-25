package com.ezielnik.api.auth.two_factor_auth;

public class TwoFactorVerifyRequest {
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
