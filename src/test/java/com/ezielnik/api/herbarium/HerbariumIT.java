package com.ezielnik.api.herbarium;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HerbariumIT extends IntegrationTestBase {

    private HerbariumRequest herbariumRequest(String name, boolean isPublic) {
        HerbariumRequest req = new HerbariumRequest();
        req.setName(name);
        req.setDescription("A test herbarium");
        req.setPublic(isPublic);
        return req;
    }

    private HerbariumResponse createHerbarium(String token, String name, boolean isPublic) {
        ResponseEntity<HerbariumResponse> resp = restTemplate.exchange(
                "/herbaria", HttpMethod.POST,
                withAuth(herbariumRequest(name, isPublic), token),
                HerbariumResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        HerbariumResponse body = resp.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    @Test
    void createHerbarium_returnsCreatedHerbarium() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        HerbariumResponse resp = createHerbarium(token, "My Garden", false);

        assertThat(resp).isNotNull();
        assertThat(resp.getName()).isEqualTo("My Garden");
        assertThat(resp.isPublic()).isFalse();
        assertThat(resp.getId()).isNotNull();
    }

    @Test
    void createHerbarium_duplicateName_returns409() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        createHerbarium(token, "My Garden", false);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria", HttpMethod.POST,
                withAuth(herbariumRequest("My Garden", false), token),
                String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createHerbarium_missingName_returns400() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");

        HerbariumRequest req = new HerbariumRequest();
        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria", HttpMethod.POST, withAuth(req, token), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getMyHerbaria_returnsOwnedHerbaria() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        createHerbarium(token, "Herbs", false);
        createHerbarium(token, "Flowers", true);

        ResponseEntity<List<HerbariumResponse>> resp = restTemplate.exchange(
                "/herbaria/me", HttpMethod.GET, withAuth(token),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void getHerbarium_ownHerbarium_succeeds() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        HerbariumResponse created = createHerbarium(token, "My Garden", false);

        ResponseEntity<HerbariumResponse> resp = restTemplate.exchange(
                "/herbaria/" + created.getId(), HttpMethod.GET, withAuth(token), HerbariumResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var herbariumBody = resp.getBody();
        assertThat(herbariumBody).isNotNull();
        assertThat(herbariumBody.getId()).isEqualTo(created.getId());
    }

    @Test
    void getHerbarium_otherUsersPrivate_returns403() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        registerAndVerify("bob", "bob@example.com", "Password1!");

        String aliceToken = loginAndGetToken("alice", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        HerbariumResponse aliceHerbarium = createHerbarium(aliceToken, "Alice Garden", false);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + aliceHerbarium.getId(), HttpMethod.GET,
                withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getHerbarium_publicHerbarium_accessibleByOtherUser() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        registerAndVerify("bob", "bob@example.com", "Password1!");

        String aliceToken = loginAndGetToken("alice", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        HerbariumResponse aliceHerbarium = createHerbarium(aliceToken, "Public Garden", true);

        ResponseEntity<HerbariumResponse> resp = restTemplate.exchange(
                "/herbaria/" + aliceHerbarium.getId(), HttpMethod.GET,
                withAuth(bobToken), HerbariumResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getPublicHerbaria_returnsOnlyPublic() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        createHerbarium(token, "Private", false);
        createHerbarium(token, "Public", true);

        ResponseEntity<List<HerbariumResponse>> resp = restTemplate.exchange(
                "/herbaria/public", HttpMethod.GET,
                noAuth(),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var publicHerbaria = resp.getBody();
        assertThat(publicHerbaria).hasSize(1);
        assertThat(publicHerbaria.getFirst().getName()).isEqualTo("Public");
    }

    @Test
    void updateHerbarium_changesName() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        HerbariumResponse created = createHerbarium(token, "Old Name", false);

        ResponseEntity<HerbariumResponse> resp = restTemplate.exchange(
                "/herbaria/" + created.getId(), HttpMethod.PATCH,
                withAuth(herbariumRequest("New Name", true), token),
                HerbariumResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var updatedHerbarium = resp.getBody();
        assertThat(updatedHerbarium).isNotNull();
        assertThat(updatedHerbarium.getName()).isEqualTo("New Name");
        assertThat(updatedHerbarium.isPublic()).isTrue();
    }

    @Test
    void updateHerbarium_otherUsersHerbarium_returns403() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        registerAndVerify("bob", "bob@example.com", "Password1!");

        String aliceToken = loginAndGetToken("alice", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        HerbariumResponse aliceHerbarium = createHerbarium(aliceToken, "Alice Garden", true);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + aliceHerbarium.getId(), HttpMethod.PATCH,
                withAuth(herbariumRequest("Stolen Name", false), bobToken),
                String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteHerbarium_ownHerbarium_succeeds() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        String token = loginAndGetToken("alice", "Password1!");
        HerbariumResponse created = createHerbarium(token, "To Delete", false);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + created.getId(), HttpMethod.DELETE, withAuth(token), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> getResp = restTemplate.exchange(
                "/herbaria/" + created.getId(), HttpMethod.GET, withAuth(token), String.class
        );
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteHerbarium_otherUsersHerbarium_returns403() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        registerAndVerify("bob", "bob@example.com", "Password1!");

        String aliceToken = loginAndGetToken("alice", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");
        HerbariumResponse aliceHerbarium = createHerbarium(aliceToken, "Alice Garden", true);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + aliceHerbarium.getId(), HttpMethod.DELETE,
                withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getUserHerbaria_showsOnlyPublicForNonFriends() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        registerAndVerify("bob", "bob@example.com", "Password1!");

        String aliceToken = loginAndGetToken("alice", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        createHerbarium(aliceToken, "Private", false);
        createHerbarium(aliceToken, "Public", true);

        User alice = userRepository.findByEmailOrUsername("alice", "alice").orElseThrow();

        ResponseEntity<List<HerbariumResponse>> resp = restTemplate.exchange(
                "/herbaria/user/" + alice.getId(), HttpMethod.GET, withAuth(bobToken),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var userHerbaria = resp.getBody();
        assertThat(userHerbaria).hasSize(1);
        assertThat(userHerbaria.getFirst().getName()).isEqualTo("Public");
    }
}
