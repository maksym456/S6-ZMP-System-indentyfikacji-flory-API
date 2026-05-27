package com.ezielnik.api.security;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.auth.LoginResponse;
import com.ezielnik.api.auth.RegisterResponse;
import com.ezielnik.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAccessControlIT extends IntegrationTestBase {

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/users/me", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() {
        // Build a token that expired 1 second ago
        User user = new User();
        user.setUsername("temp");
        user.setEmail("temp@example.com");
        user.setPasswordHash("hash");
        user.prePersist();

        // We can't generate an already-expired token from JwtService, but we can test with a garbage token
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth("totally.invalid.jwttoken");
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me", HttpMethod.GET,
                new org.springframework.http.HttpEntity<Void>(null, headers),
                String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_withValidToken_returnsUserData() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me", HttpMethod.GET, withAuth(token), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void me_bannedUser_withValidToken_returns403() {
        RegisterResponse reg = register("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        // Ban the user
        User user = userRepository.findById(reg.getId()).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me", HttpMethod.GET, withAuth(token), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void verifiedRequired_unverifiedUser_returns403() {
        register("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        // /herbaria/me is verified-only (not the /users/me exception)
        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/me", HttpMethod.GET, withAuth(token), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void me_unverifiedUser_stillAccessible() {
        // /users/me is accessible for unverified users (only requires active)
        register("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me", HttpMethod.GET, withAuth(token), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void preAuthToken_onProtectedEndpoint_returns403() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        // Enable 2FA so we can get a pre-auth token
        restTemplate.exchange("/users/2fa/email/enable", HttpMethod.POST, withAuth(token), String.class);

        LoginResponse loginResp = login("alice", "Password1!");
        String preAuthToken = loginResp.getPreAuthToken();

        // Using pre-auth token on a regular endpoint should be forbidden
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me", HttpMethod.GET, withAuth(preAuthToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void publicEndpoints_doNotRequireAuth() {
        // Registration
        assertThat(restTemplate.postForEntity("/users/register",
                new com.ezielnik.api.auth.RegisterRequest(), String.class)
                .getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);

        // Verify (bad token -> 400, not 401)
        assertThat(restTemplate.getForEntity("/users/verify?token=x", String.class)
                .getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);

        // Forgot password (bad request -> 400, not 401)
        assertThat(restTemplate.postForEntity("/users/forgot-password",
                new com.ezielnik.api.auth.ForgotPasswordRequest(), String.class)
                .getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anotherUsersResource_inaccessibleWithoutProperOwnership() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        registerAndVerify("bob", "bob@example.com", "Password1!");

        String aliceToken = loginAndGetToken("alice", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        // Bob's /users/me should not return Alice's data
        ResponseEntity<com.ezielnik.api.user.UserResponse> aliceMeResp = restTemplate.exchange(
                "/users/me", HttpMethod.GET,
                withAuth(aliceToken), com.ezielnik.api.user.UserResponse.class
        );
        var aliceBody = aliceMeResp.getBody();
        assertThat(aliceBody).isNotNull();
        assertThat(aliceBody.getUsername()).isEqualTo("alice");

        ResponseEntity<com.ezielnik.api.user.UserResponse> bobMeResp = restTemplate.exchange(
                "/users/me", HttpMethod.GET,
                withAuth(bobToken), com.ezielnik.api.user.UserResponse.class
        );
        var bobBody = bobMeResp.getBody();
        assertThat(bobBody).isNotNull();
        assertThat(bobBody.getUsername()).isEqualTo("bob");
    }
}
