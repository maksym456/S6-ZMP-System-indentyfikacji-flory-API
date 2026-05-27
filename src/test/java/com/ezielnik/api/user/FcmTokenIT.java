package com.ezielnik.api.user;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.fcm.DeviceTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FcmTokenIT extends IntegrationTestBase {

    private String aliceToken;

    @BeforeEach
    void setUp() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        aliceToken = loginAndGetToken("alice", "Password1!");
    }

    @Test
    void registerFcmToken_storesToken() {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("device-token-abc123");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me/fcm-token", HttpMethod.POST,
                withAuth(req, aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void registerFcmToken_blankToken_returns400() {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me/fcm-token", HttpMethod.POST,
                withAuth(req, aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registerFcmToken_requiresAuth() {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("some-token");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me/fcm-token", HttpMethod.POST,
                new HttpEntity<>(req), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unregisterFcmToken_removesToken() {
        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("device-token-xyz");
        restTemplate.exchange("/users/me/fcm-token", HttpMethod.POST, withAuth(req, aliceToken), String.class);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me/fcm-token?token=device-token-xyz", HttpMethod.DELETE,
                withAuth(aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void registerFcmToken_sameTokenDifferentUser_reassigns() {
        registerAndVerify("bob", "bob@example.com", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        DeviceTokenRequest req = new DeviceTokenRequest();
        req.setToken("shared-device-token");

        restTemplate.exchange("/users/me/fcm-token", HttpMethod.POST, withAuth(req, aliceToken), String.class);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/me/fcm-token", HttpMethod.POST, withAuth(req, bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
