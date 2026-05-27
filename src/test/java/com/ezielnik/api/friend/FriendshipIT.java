package com.ezielnik.api.friend;

import com.ezielnik.api.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FriendshipIT extends IntegrationTestBase {

    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUpUsers() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        registerAndVerify("bob", "bob@example.com", "Password1!");
        aliceToken = loginAndGetToken("alice", "Password1!");
        bobToken = loginAndGetToken("bob", "Password1!");
    }

    private FriendResponse sendRequest(String fromToken) {
        FriendRequestBody body = new FriendRequestBody();
        body.setUsername("bob");
        ResponseEntity<FriendResponse> resp = restTemplate.exchange(
                "/friends/request", HttpMethod.POST,
                withAuth(body, fromToken), FriendResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    @Test
    void sendFriendRequest_createsRequest() {
        FriendResponse resp = sendRequest(aliceToken);

        assertThat(resp).isNotNull();
        assertThat(resp.getUsername()).isEqualTo("bob");
        assertThat(resp.getStatus()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(resp.getDirection()).isEqualTo("SENT");
    }

    @Test
    void sendFriendRequest_toSelf_returns400() {
        FriendRequestBody body = new FriendRequestBody();
        body.setUsername("alice");
        ResponseEntity<String> resp = restTemplate.exchange(
                "/friends/request", HttpMethod.POST, withAuth(body, aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sendFriendRequest_toNonexistentUser_returns404() {
        FriendRequestBody body = new FriendRequestBody();
        body.setUsername("ghost");
        ResponseEntity<String> resp = restTemplate.exchange(
                "/friends/request", HttpMethod.POST, withAuth(body, aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void sendFriendRequest_duplicate_returns409() {
        sendRequest(aliceToken);

        FriendRequestBody body = new FriendRequestBody();
        body.setUsername("bob");
        ResponseEntity<String> resp = restTemplate.exchange(
                "/friends/request", HttpMethod.POST, withAuth(body, aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void acceptFriendRequest_makesThemFriends() {
        FriendResponse request = sendRequest(aliceToken);
        UUID friendshipId = request.getFriendshipId();

        ResponseEntity<FriendResponse> resp = restTemplate.exchange(
                "/friends/" + friendshipId + "/accept", HttpMethod.POST,
                withAuth(bobToken), FriendResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var acceptBody = resp.getBody();
        assertThat(acceptBody).isNotNull();
        assertThat(acceptBody.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);

        ResponseEntity<List<FriendResponse>> friends = restTemplate.exchange(
                "/friends", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );
        var friendsBody = friends.getBody();
        assertThat(friendsBody).hasSize(1);
        assertThat(friendsBody.getFirst().getUsername()).isEqualTo("bob");
    }

    @Test
    void acceptFriendRequest_asRequester_returns403() {
        FriendResponse request = sendRequest(aliceToken);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/friends/" + request.getFriendshipId() + "/accept",
                HttpMethod.POST, withAuth(aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void removeFriendship_byRequester_removes() {
        FriendResponse request = sendRequest(aliceToken);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/friends/" + request.getFriendshipId(), HttpMethod.DELETE,
                withAuth(aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<List<FriendResponse>> friends = restTemplate.exchange(
                "/friends", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(friends.getBody()).isEmpty();
    }

    @Test
    void removeFriendship_byAddressee_removes() {
        FriendResponse request = sendRequest(aliceToken);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/friends/" + request.getFriendshipId(), HttpMethod.DELETE,
                withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void removeFriendship_byUnrelatedUser_returns403() {
        registerAndVerify("charlie", "charlie@example.com", "Password1!");
        String charlieToken = loginAndGetToken("charlie", "Password1!");

        FriendResponse request = sendRequest(aliceToken);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/friends/" + request.getFriendshipId(), HttpMethod.DELETE,
                withAuth(charlieToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getFriends_returnsOnlyAccepted() {
        FriendResponse request = sendRequest(aliceToken);

        // Before accept, friends list should be empty
        ResponseEntity<List<FriendResponse>> beforeAccept = restTemplate.exchange(
                "/friends", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(beforeAccept.getBody()).isEmpty();

        // Accept
        restTemplate.exchange(
                "/friends/" + request.getFriendshipId() + "/accept",
                HttpMethod.POST, withAuth(bobToken), FriendResponse.class
        );

        ResponseEntity<List<FriendResponse>> afterAccept = restTemplate.exchange(
                "/friends", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(afterAccept.getBody()).hasSize(1);
    }

    @Test
    void getIncomingRequests_showsPendingForAddressee() {
        sendRequest(aliceToken);

        ResponseEntity<List<FriendResponse>> resp = restTemplate.exchange(
                "/friends/requests", HttpMethod.GET, withAuth(bobToken),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var incomingBody = resp.getBody();
        assertThat(incomingBody).hasSize(1);
        assertThat(incomingBody.getFirst().getUsername()).isEqualTo("alice");
        assertThat(incomingBody.getFirst().getDirection()).isEqualTo("RECEIVED");
    }

    @Test
    void getSentRequests_showsPendingForRequester() {
        sendRequest(aliceToken);

        ResponseEntity<List<FriendResponse>> resp = restTemplate.exchange(
                "/friends/requests/sent", HttpMethod.GET, withAuth(aliceToken),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var sentBody = resp.getBody();
        assertThat(sentBody).hasSize(1);
        assertThat(sentBody.getFirst().getDirection()).isEqualTo("SENT");
    }

    @Test
    void friends_canSeeEachOthersPrivateHerbaria() {
        // After becoming friends, Alice should see Bob's private herbarium
        FriendResponse request = sendRequest(aliceToken);
        restTemplate.exchange(
                "/friends/" + request.getFriendshipId() + "/accept",
                HttpMethod.POST, withAuth(bobToken), FriendResponse.class
        );

        com.ezielnik.api.herbarium.HerbariumRequest herbReq = new com.ezielnik.api.herbarium.HerbariumRequest();
        herbReq.setName("Bob Private Herb");
        herbReq.setPublic(false);
        com.ezielnik.api.herbarium.HerbariumResponse bobHerbarium = restTemplate.exchange(
                "/herbaria", HttpMethod.POST,
                withAuth(herbReq, bobToken),
                com.ezielnik.api.herbarium.HerbariumResponse.class
        ).getBody();
        assertThat(bobHerbarium).isNotNull();

        ResponseEntity<com.ezielnik.api.herbarium.HerbariumResponse> resp = restTemplate.exchange(
                "/herbaria/" + bobHerbarium.getId(), HttpMethod.GET,
                withAuth(aliceToken), com.ezielnik.api.herbarium.HerbariumResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
