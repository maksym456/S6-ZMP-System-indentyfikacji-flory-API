package com.ezielnik.api.user;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.auth.LoginRequest;
import com.ezielnik.api.auth.LoginResponse;
import com.ezielnik.api.auth.RegisterRequest;
import com.ezielnik.api.auth.RegisterResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class RegistrationLoginIT extends IntegrationTestBase {

    @Test
    void register_validRequest_returns201AndSendsVerificationEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password1!");

        ResponseEntity<RegisterResponse> resp = restTemplate.postForEntity("/users/register", req, RegisterResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RegisterResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUsername()).isEqualTo("alice");
        assertThat(body.getEmail()).isEqualTo("alice@example.com");
        assertThat(body.getId()).isNotNull();

        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void register_duplicateEmail_returns409() {
        register("alice", "alice@example.com", "Password1!");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice2");
        req.setEmail("alice@example.com");
        req.setPassword("Password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/register", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_duplicateUsername_returns409() {
        register("alice", "alice@example.com", "Password1!");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice2@example.com");
        req.setPassword("Password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/register", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_weakPassword_noUppercase_returns400() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@example.com");
        req.setPassword("password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/register", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_weakPassword_noNumber_returns400() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@example.com");
        req.setPassword("Password!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/register", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_weakPassword_noSpecialChar_returns400() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@example.com");
        req.setPassword("Password1");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/register", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_weakPassword_tooShort_returns400() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@example.com");
        req.setPassword("P1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/register", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_missingUsername_returns400() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("bob@example.com");
        req.setPassword("Password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/register", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_validCredentials_returnsTokenAndRefreshToken() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        LoginRequest req = new LoginRequest();
        req.setLogin("alice");
        req.setPassword("Password1!");

        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity("/users/login", req, LoginResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getToken()).isNotBlank();
        assertThat(body.getRefreshToken()).isNotBlank();
        assertThat(body.getRefreshToken()).contains(":");
    }

    @Test
    void login_byEmail_works() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        LoginRequest req = new LoginRequest();
        req.setLogin("alice@example.com");
        req.setPassword("Password1!");

        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity("/users/login", req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var loginByEmail = resp.getBody();
        assertThat(loginByEmail).isNotNull();
        assertThat(loginByEmail.getToken()).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        LoginRequest req = new LoginRequest();
        req.setLogin("alice");
        req.setPassword("WrongPassword1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_unknownUser_returns401() {
        LoginRequest req = new LoginRequest();
        req.setLogin("ghost");
        req.setPassword("Password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_bannedUser_returns403() {
        RegisterResponse reg = register("alice", "alice@example.com", "Password1!");
        User user = userRepository.findById(reg.getId()).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        LoginRequest req = new LoginRequest();
        req.setLogin("alice");
        req.setPassword("Password1!");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/login", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void me_withValidToken_returnsCurrentUser() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        ResponseEntity<UserResponse> resp = restTemplate.exchange(
                "/users/me", HttpMethod.GET, withAuth(token), UserResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var meBody = resp.getBody();
        assertThat(meBody).isNotNull();
        assertThat(meBody.getUsername()).isEqualTo("alice");
    }

    @Test
    void me_withoutToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/users/me", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_unverifiedUser_returnsTokenWithWarning() {
        register("alice", "alice@example.com", "Password1!");

        LoginRequest req = new LoginRequest();
        req.setLogin("alice");
        req.setPassword("Password1!");

        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity("/users/login", req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var unverifiedLogin = resp.getBody();
        assertThat(unverifiedLogin).isNotNull();
        assertThat(unverifiedLogin.isVerified()).isFalse();
        assertThat(unverifiedLogin.getWarning()).isNotBlank();
    }

    @Test
    void deleteMyAccount_anonymizesUser() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me", HttpMethod.DELETE, withAuth(token), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User deleted = userRepository.findByEmailOrUsername("alice@example.com", "alice").orElse(null);
        assertThat(deleted).isNull();
    }

    @Test
    void register_emailStoredLowercase() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("Alice@EXAMPLE.COM");
        req.setPassword("Password1!");

        ResponseEntity<RegisterResponse> resp = restTemplate.postForEntity("/users/register", req, RegisterResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var regBody = resp.getBody();
        assertThat(regBody).isNotNull();
        assertThat(regBody.getEmail()).isEqualTo("alice@example.com");
    }
}
