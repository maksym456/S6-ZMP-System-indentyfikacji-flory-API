package com.ezielnik.api.user;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.auth.LoginResponse;
import com.ezielnik.api.auth.refresh_token.RefreshRequest;
import com.ezielnik.api.auth.refresh_token.RefreshResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenIT extends IntegrationTestBase {

    @Test
    void refresh_validToken_returnsNewTokenPair() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        LoginResponse loginResp = login("alice", "Password1!");
        String oldRefreshToken = loginResp.getRefreshToken();
        String oldAccessToken = loginResp.getToken();

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(oldRefreshToken);

        ResponseEntity<RefreshResponse> resp = restTemplate.postForEntity("/users/refresh", req, RefreshResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        RefreshResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.token()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.token()).isNotEqualTo(oldAccessToken);
        assertThat(body.refreshToken()).isNotEqualTo(oldRefreshToken);
    }

    @Test
    void refresh_oldTokenCannotBeReusedAfterRotation() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        LoginResponse loginResp = login("alice", "Password1!");
        String oldRefreshToken = loginResp.getRefreshToken();

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(oldRefreshToken);
        restTemplate.postForEntity("/users/refresh", req, RefreshResponse.class);

        ResponseEntity<String> reuseResp = restTemplate.postForEntity("/users/refresh", req, String.class);
        assertThat(reuseResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_reuseDetected_revokesAllSessions() {
        registerAndVerify("alice", "alice@example.com", "Password1!");

        // Login on two devices
        LoginResponse device1 = login("alice", "Password1!");
        LoginResponse device2 = login("alice", "Password1!");

        // Rotate device1's token
        RefreshRequest req1 = new RefreshRequest();
        req1.setRefreshToken(device1.getRefreshToken());
        ResponseEntity<RefreshResponse> rotated = restTemplate.postForEntity("/users/refresh", req1, RefreshResponse.class);
        assertThat(rotated.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Replay the original device1 token (theft detection)
        restTemplate.postForEntity("/users/refresh", req1, String.class);

        // Device2's valid token should now also be revoked
        RefreshRequest req2 = new RefreshRequest();
        req2.setRefreshToken(device2.getRefreshToken());
        ResponseEntity<String> device2Resp = restTemplate.postForEntity("/users/refresh", req2, String.class);
        assertThat(device2Resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_invalidToken_returns401() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("completely-invalid-token");

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/refresh", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_nullToken_returns401() {
        RefreshRequest req = new RefreshRequest();

        ResponseEntity<String> resp = restTemplate.postForEntity("/users/refresh", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_validRefreshToken_invalidatesIt() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        LoginResponse loginResp = login("alice", "Password1!");
        String refreshToken = loginResp.getRefreshToken();

        RefreshRequest logoutReq = new RefreshRequest();
        logoutReq.setRefreshToken(refreshToken);

        ResponseEntity<String> logoutResp = restTemplate.postForEntity("/users/logout", logoutReq, String.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        RefreshRequest refreshReq = new RefreshRequest();
        refreshReq.setRefreshToken(refreshToken);
        ResponseEntity<String> refreshResp = restTemplate.postForEntity("/users/refresh", refreshReq, String.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_onlyInvalidatesOneDeviceSession() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        LoginResponse device1 = login("alice", "Password1!");
        LoginResponse device2 = login("alice", "Password1!");

        // Logout device1
        RefreshRequest logoutReq = new RefreshRequest();
        logoutReq.setRefreshToken(device1.getRefreshToken());
        restTemplate.postForEntity("/users/logout", logoutReq, String.class);

        // Device2 should still work
        RefreshRequest refreshReq = new RefreshRequest();
        refreshReq.setRefreshToken(device2.getRefreshToken());
        ResponseEntity<RefreshResponse> device2Resp = restTemplate.postForEntity("/users/refresh", refreshReq, RefreshResponse.class);
        assertThat(device2Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_withNullToken_returns200() {
        RefreshRequest req = new RefreshRequest();
        ResponseEntity<String> resp = restTemplate.postForEntity("/users/logout", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void refresh_doesNotRequireAuthentication() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        LoginResponse loginResp = login("alice", "Password1!");

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(loginResp.getRefreshToken());

        // No Authorization header needed
        ResponseEntity<RefreshResponse> resp = restTemplate.postForEntity("/users/refresh", req, RefreshResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
