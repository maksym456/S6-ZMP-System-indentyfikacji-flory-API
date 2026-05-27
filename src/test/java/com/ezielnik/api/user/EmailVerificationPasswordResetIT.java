package com.ezielnik.api.user;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.auth.ForgotPasswordRequest;
import com.ezielnik.api.auth.LoginRequest;
import com.ezielnik.api.auth.LoginResponse;
import com.ezielnik.api.auth.RegisterResponse;
import com.ezielnik.api.auth.ResetPasswordRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class EmailVerificationPasswordResetIT extends IntegrationTestBase {

    @Test
    void verifyEmail_validToken_setsVerifiedTrue() {
        RegisterResponse reg = register("alice", "alice@example.com", "Password1!");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq("alice@example.com"), tokenCaptor.capture());
        String verificationToken = tokenCaptor.getValue();

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/users/verify?token=" + verificationToken, String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        User user = userRepository.findById(reg.getId()).orElseThrow();
        assertThat(user.isVerified()).isTrue();
    }

    @Test
    void verifyEmail_invalidToken_returns400() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/users/verify?token=invalid.token.here", String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void verifyEmail_alreadyVerified_returnsAlreadyVerifiedMessage() {
        register("alice", "alice@example.com", "Password1!");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(anyString(), tokenCaptor.capture());
        String token = tokenCaptor.getValue();

        restTemplate.getForEntity("/users/verify?token=" + token, String.class);
        ResponseEntity<String> second = restTemplate.getForEntity("/users/verify?token=" + token, String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).contains("already verified");
    }

    @Test
    void resendVerification_existingUnverifiedAccount_sendsEmail() {
        register("alice", "alice@example.com", "Password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/users/resend-verification?email=alice@example.com", null, String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(emailService, org.mockito.Mockito.atLeast(2))
                .sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerification_nonexistentEmail_returnsGenericMessage() {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/users/resend-verification?email=ghost@example.com", null, String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgotPassword_validEmail_sendsResetEmail() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("alice@example.com");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/forgot-password", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(emailService).sendPasswordResetEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void forgotPassword_unknownEmail_returnsGenericMessage() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("ghost@example.com");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/forgot-password", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgotPassword_missingEmail_returns400() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/forgot-password", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPassword_validToken_changesPassword() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("alice@example.com");
        restTemplate.postForEntity("/users/forgot-password", forgotReq, String.class);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(anyString(), tokenCaptor.capture());
        String resetToken = tokenCaptor.getValue();

        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setToken(resetToken);
        resetReq.setNewPassword("NewPassword2@");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/reset-password", resetReq, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setLogin("alice");
        loginReq.setPassword("NewPassword2@");
        ResponseEntity<LoginResponse> loginResp = restTemplate.postForEntity("/users/login", loginReq, LoginResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void resetPassword_samePassword_returns400() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("alice@example.com");
        restTemplate.postForEntity("/users/forgot-password", forgotReq, String.class);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(anyString(), tokenCaptor.capture());

        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setToken(tokenCaptor.getValue());
        resetReq.setNewPassword("Password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/reset-password", resetReq, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPassword_invalidToken_returns400() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("invalid.token.here");
        req.setNewPassword("NewPassword2@");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/reset-password", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPassword_tokenInvalidatedAfterPasswordChange() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("alice@example.com");
        restTemplate.postForEntity("/users/forgot-password", forgotReq, String.class);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(anyString(), tokenCaptor.capture());
        String token = tokenCaptor.getValue();

        ResetPasswordRequest firstReset = new ResetPasswordRequest();
        firstReset.setToken(token);
        firstReset.setNewPassword("NewPassword2@");
        restTemplate.postForEntity("/users/reset-password", firstReset, String.class);

        // Using the same token again should fail because password changed
        ResetPasswordRequest secondReset = new ResetPasswordRequest();
        secondReset.setToken(token);
        secondReset.setNewPassword("AnotherPass3#");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/reset-password", secondReset, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void showResetPasswordForm_returnsHtml() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/users/reset-password?token=sometoken", String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("<form");
    }
}
