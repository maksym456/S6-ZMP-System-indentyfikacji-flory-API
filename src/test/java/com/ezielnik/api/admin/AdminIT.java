package com.ezielnik.api.admin;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.admin.content_management.AdminHerbariumDetailResponse;
import com.ezielnik.api.admin.content_management.AdminHerbariumListItemResponse;
import com.ezielnik.api.admin.user_management.AdminUserFriendsResponse;
import com.ezielnik.api.admin.user_management.AdminWarningRequest;
import com.ezielnik.api.auth.RegisterResponse;
import com.ezielnik.api.herbarium.HerbariumRequest;
import com.ezielnik.api.herbarium.HerbariumResponse;
import com.ezielnik.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class AdminIT extends IntegrationTestBase {

    private String adminToken;
    private UUID adminUserId;
    private UUID regularUserId;
    private String regularToken;

    @BeforeEach
    void setUpAdmin() {
        RegisterResponse adminReg = register("adminUser", "admin@example.com", "Password1!");
        adminUserId = adminReg.getId();
        verifyUser(adminUserId);

        // Promote to admin directly
        User admin = userRepository.findById(adminUserId).orElseThrow();
        admin.setAdmin(true);
        userRepository.save(admin);
        adminToken = loginAndGetToken("adminUser", "Password1!");

        RegisterResponse reg = register("bob", "bob@example.com", "Password1!");
        regularUserId = reg.getId();
        verifyUser(regularUserId);
        regularToken = loginAndGetToken("bob", "Password1!");
    }

    @Test
    void banUser_asAdmin_deactivatesUser() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId + "/ban", HttpMethod.PATCH,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User user = userRepository.findById(regularUserId).orElseThrow();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void banUser_asNonAdmin_returns403() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + adminUserId + "/ban", HttpMethod.PATCH,
                withAuth(regularToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void banUser_selfBan_returns400() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + adminUserId + "/ban", HttpMethod.PATCH,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unbanUser_asAdmin_reactivatesUser() {
        // Ban first
        restTemplate.exchange("/users/" + regularUserId + "/ban", HttpMethod.PATCH,
                withAuth(adminToken), String.class);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId + "/unban", HttpMethod.PATCH,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User user = userRepository.findById(regularUserId).orElseThrow();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void makeAdmin_asAdmin_promotesUser() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId + "/make-admin", HttpMethod.PATCH,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User user = userRepository.findById(regularUserId).orElseThrow();
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void removeAdmin_asAdmin_demotesUser() {
        User bob = userRepository.findById(regularUserId).orElseThrow();
        bob.setAdmin(true);
        userRepository.save(bob);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId + "/remove-admin", HttpMethod.PATCH,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User updated = userRepository.findById(regularUserId).orElseThrow();
        assertThat(updated.isAdmin()).isFalse();
    }

    @Test
    void removeAdmin_selfRemove_returns400() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + adminUserId + "/remove-admin", HttpMethod.PATCH,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sendAdminWarning_sendsEmailAndCreatesNotification() {
        AdminWarningRequest req = new AdminWarningRequest();
        req.setSubject("Policy Violation");
        req.setMessage("Your content violates our terms.");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId + "/warning", HttpMethod.POST,
                withAuth(req, adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(emailService).sendAdminWarningEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendAdminWarning_missingSubject_returns400() {
        AdminWarningRequest req = new AdminWarningRequest();
        req.setMessage("Some message");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId + "/warning", HttpMethod.POST,
                withAuth(req, adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteUser_asAdmin_anonymizesUser() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId, HttpMethod.DELETE,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User deletedUser = userRepository.findById(regularUserId).orElseThrow();
        assertThat(deletedUser.getEmail()).endsWith("@deleted.local");
        assertThat(deletedUser.getUsername()).startsWith("deleted-user-");
    }

    @Test
    void deleteUser_selfDelete_returns400() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + adminUserId, HttpMethod.DELETE,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getOverviewStats_asAdmin_returnsStats() {
        ResponseEntity<AdminOverviewStatsResponse> resp = restTemplate.exchange(
                "/stats/overview", HttpMethod.GET, withAuth(adminToken),
                AdminOverviewStatsResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    void getOverviewStats_asNonAdmin_returns403() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/stats/overview", HttpMethod.GET, withAuth(regularToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listUsers_asAdmin_returnsAllUsers() {
        ResponseEntity<com.ezielnik.api.admin.user_management.AdminUserResponse[]> resp = restTemplate.exchange(
                "/stats/users", HttpMethod.GET, withAuth(adminToken),
                com.ezielnik.api.admin.user_management.AdminUserResponse[].class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void makeAdmin_inactiveUser_returns400() {
        User bob = userRepository.findById(regularUserId).orElseThrow();
        bob.setActive(false);
        userRepository.save(bob);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + regularUserId + "/make-admin", HttpMethod.PATCH,
                withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getUserDetail_asAdmin_returnsUserDetails() {
        ResponseEntity<com.ezielnik.api.admin.user_management.AdminUserDetailResponse> resp = restTemplate.exchange(
                "/stats/users/" + regularUserId, HttpMethod.GET, withAuth(adminToken),
                com.ezielnik.api.admin.user_management.AdminUserDetailResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    void listHerbaria_asAdmin_returnsAll() {
        HerbariumRequest herbReq = new HerbariumRequest();
        herbReq.setName("Bob's Herb");
        herbReq.setPublic(true);
        restTemplate.exchange("/herbaria", HttpMethod.POST, withAuth(herbReq, regularToken), HerbariumResponse.class);

        ResponseEntity<AdminHerbariumListItemResponse[]> resp = restTemplate.exchange(
                "/stats/herbaria", HttpMethod.GET, withAuth(adminToken),
                AdminHerbariumListItemResponse[].class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void getHerbariumDetail_asAdmin_returnsDetail() {
        HerbariumRequest herbReq = new HerbariumRequest();
        herbReq.setName("Bob's Secret Herb");
        herbReq.setPublic(false);
        HerbariumResponse herb = restTemplate.exchange(
                "/herbaria", HttpMethod.POST, withAuth(herbReq, regularToken), HerbariumResponse.class
        ).getBody();
        assertThat(herb).isNotNull();

        ResponseEntity<AdminHerbariumDetailResponse> resp = restTemplate.exchange(
                "/stats/herbaria/" + herb.getId(), HttpMethod.GET, withAuth(adminToken),
                AdminHerbariumDetailResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    void getUserFriends_asAdmin_returnsStructure() {
        ResponseEntity<AdminUserFriendsResponse> resp = restTemplate.exchange(
                "/stats/users/" + regularUserId + "/friends", HttpMethod.GET, withAuth(adminToken),
                AdminUserFriendsResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var friends = resp.getBody();
        assertThat(friends).isNotNull();
        assertThat(friends.accepted()).isNotNull();
        assertThat(friends.incoming()).isNotNull();
        assertThat(friends.sent()).isNotNull();
    }

    @Test
    void getUserFriends_unknownUser_returns404() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/stats/users/" + UUID.randomUUID() + "/friends", HttpMethod.GET, withAuth(adminToken),
                String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
