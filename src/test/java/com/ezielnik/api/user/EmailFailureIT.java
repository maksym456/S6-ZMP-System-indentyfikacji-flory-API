package com.ezielnik.api.user;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.auth.ForgotPasswordRequest;
import com.ezielnik.api.auth.RegisterRequest;
import com.ezielnik.api.auth.RegisterResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

class EmailFailureIT extends IntegrationTestBase {

    @Test
    void register_emailServiceDown_stillReturns201WithFallbackMessage() {
        Mockito.doThrow(new RuntimeException("Brevo unreachable"))
                .when(emailService).sendVerificationEmail(anyString(), anyString());

        ResponseEntity<RegisterResponse> resp = restTemplate.postForEntity(
                "/users/register",
                buildRegisterRequest(),
                RegisterResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).contains("could not send the verification email");
    }

    @Test
    void resendVerification_emailServiceDown_stillReturns200() {
        register("alice", "alice@example.com", "Password1!"); // not verified — so resend actually tries to send
        Mockito.doThrow(new RuntimeException("Brevo unreachable"))
                .when(emailService).sendVerificationEmail(anyString(), anyString());

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/users/resend-verification?email=alice@example.com",
                null,
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgotPassword_emailServiceDown_stillReturns200() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        Mockito.doThrow(new RuntimeException("Brevo unreachable"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString());

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("alice@example.com");

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/users/forgot-password",
                req,
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password1!");
        return req;
    }
}
