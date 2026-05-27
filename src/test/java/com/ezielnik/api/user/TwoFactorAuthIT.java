package com.ezielnik.api.user;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.auth.LoginResponse;
import com.ezielnik.api.auth.two_factor_auth.TwoFactorVerifyRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class TwoFactorAuthIT extends IntegrationTestBase {

    @Test
    void enable2fa_withVerifiedUser_succeeds() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        User user = userRepository.findByEmailOrUsername("alice", "alice").orElseThrow();
        assertThat(user.isEmailTwoFactorEnabled()).isTrue();
    }

    @Test
    void enable2fa_withoutToken_returns401() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/2fa/email/enable", HttpMethod.POST,
                noAuth(), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void disable2fa_withVerifiedUserAnd2faEnabled_succeeds() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        restTemplate.exchange("/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/2fa/disable", HttpMethod.POST, withAuth(token), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        User user = userRepository.findByEmailOrUsername("alice", "alice").orElseThrow();
        assertThat(user.isEmailTwoFactorEnabled()).isFalse();
    }

    @Test
    void login_with2faEnabled_requiresTwoFactor() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        restTemplate.exchange("/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class);

        LoginResponse loginResp = login("alice", "Password1!");

        assertThat(loginResp.getRequiresTwoFactor()).isTrue();
        assertThat(loginResp.getPreAuthToken()).isNotBlank();
        assertThat(loginResp.getToken()).isNull();
        assertThat(loginResp.getRefreshToken()).isNull();
    }

    @Test
    void login_with2faEnabled_sendsEmailCode() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        restTemplate.exchange("/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class);

        login("alice", "Password1!");

        verify(emailService).sendTwoFactorCode(eq("alice@example.com"), anyString());
    }

    @Test
    void verify2fa_correctCode_returnsFullToken() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        restTemplate.exchange("/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class);

        LoginResponse preAuthResp = login("alice", "Password1!");
        String preAuthToken = preAuthResp.getPreAuthToken();

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendTwoFactorCode(anyString(), codeCaptor.capture());
        String code = codeCaptor.getValue();

        TwoFactorVerifyRequest verifyReq = new TwoFactorVerifyRequest();
        verifyReq.setCode(code);

        ResponseEntity<LoginResponse> verifyResp = restTemplate.exchange(
                "/users/verify-2fa", HttpMethod.POST, withAuth(verifyReq, preAuthToken), LoginResponse.class
        );

        assertThat(verifyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var verifyBody = verifyResp.getBody();
        assertThat(verifyBody).isNotNull();
        assertThat(verifyBody.getToken()).isNotBlank();
        assertThat(verifyBody.getRefreshToken()).isNotBlank();
    }

    @Test
    void verify2fa_wrongCode_returns401() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        restTemplate.exchange("/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class);

        LoginResponse preAuthResp = login("alice", "Password1!");
        String preAuthToken = preAuthResp.getPreAuthToken();

        TwoFactorVerifyRequest verifyReq = new TwoFactorVerifyRequest();
        verifyReq.setCode("000000");

        ResponseEntity<String> verifyResp = restTemplate.exchange(
                "/users/verify-2fa", HttpMethod.POST, withAuth(verifyReq, preAuthToken), String.class
        );

        assertThat(verifyResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void verify2fa_withRegularTokenNotPreAuth_returns403() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String regularToken = loginAndGetToken("alice", "Password1!");

        TwoFactorVerifyRequest verifyReq = new TwoFactorVerifyRequest();
        verifyReq.setCode("123456");

        ResponseEntity<String> verifyResp = restTemplate.exchange(
                "/users/verify-2fa", HttpMethod.POST, withAuth(verifyReq, regularToken), String.class
        );

        assertThat(verifyResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void resendEmailCode_withPreAuthToken_sendsNewCode() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        restTemplate.exchange("/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class);

        LoginResponse preAuthResp = login("alice", "Password1!");
        String preAuthToken = preAuthResp.getPreAuthToken();

        ResponseEntity<String> resendResp = restTemplate.exchange(
                "/users/2fa/send-email-code", HttpMethod.POST, withAuth(preAuthToken), String.class
        );

        assertThat(resendResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(emailService, org.mockito.Mockito.atLeast(2)).sendTwoFactorCode(anyString(), anyString());
    }

    @Test
    void resendEmailCode_withRegularToken_returns403() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String regularToken = loginAndGetToken("alice", "Password1!");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/2fa/send-email-code", HttpMethod.POST, withAuth(regularToken), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
