package com.ezielnik.api.admin;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.herbarium.HerbariumRequest;
import com.ezielnik.api.herbarium.HerbariumResponse;
import com.ezielnik.api.plant.PlantConfirmRequest;
import com.ezielnik.api.plant.PlantIdentificationChoice;
import com.ezielnik.api.plant.PlantResponse;
import com.ezielnik.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminContentManagementIT extends IntegrationTestBase {

    private String adminToken;
    private String bobToken;
    private UUID bobUserId;
    private UUID bobHerbariumId;

    @BeforeEach
    void setUp() {
        User admin = registerAndVerify("adminUser", "admin@example.com", "Password1!");
        admin.setAdmin(true);
        userRepository.save(admin);
        adminToken = loginAndGetToken("adminUser", "Password1!");

        User bob = registerAndVerify("bob", "bob@example.com", "Password1!");
        bobUserId = bob.getId();
        bobToken = loginAndGetToken("bob", "Password1!");

        HerbariumRequest herbReq = new HerbariumRequest();
        herbReq.setName("Bob's Garden");
        herbReq.setPublic(false);
        HerbariumResponse herb = restTemplate.exchange(
                "/herbaria", HttpMethod.POST, withAuth(herbReq, bobToken), HerbariumResponse.class
        ).getBody();
        assertThat(herb).isNotNull();
        bobHerbariumId = herb.getId();
    }

    private PlantResponse createPlantForBob() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bobToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override public String getFilename() { return "test.jpg"; }
        });
        PlantIdentificationChoice choice = restTemplate.exchange(
                "/herbaria/" + bobHerbariumId + "/plants/add",
                HttpMethod.POST, new HttpEntity<>(body, headers), PlantIdentificationChoice.class
        ).getBody();
        assertThat(choice).isNotNull();

        PlantConfirmRequest confirm = new PlantConfirmRequest();
        confirm.setPendingPhotoId(choice.getPendingPhotoId());
        confirm.setDecisionType("new");
        return restTemplate.exchange(
                "/herbaria/" + bobHerbariumId + "/plants/confirm",
                HttpMethod.POST, withAuth(confirm, bobToken), PlantResponse.class
        ).getBody();
    }

    @Test
    void adminDeleteHerbarium_asAdmin_deletesHerbarium() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + bobUserId + "/herbaria/" + bobHerbariumId,
                HttpMethod.DELETE, withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> check = restTemplate.exchange(
                "/herbaria/" + bobHerbariumId, HttpMethod.GET, withAuth(bobToken), String.class
        );
        assertThat(check.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminDeleteHerbarium_wrongUserId_returns404() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + UUID.randomUUID() + "/herbaria/" + bobHerbariumId,
                HttpMethod.DELETE, withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminDeleteHerbarium_asNonAdmin_returns403() {
        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + bobUserId + "/herbaria/" + bobHerbariumId,
                HttpMethod.DELETE, withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminDeletePlant_asAdmin_deletesPlant() {
        PlantResponse plant = createPlantForBob();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + bobUserId + "/herbaria/" + bobHerbariumId + "/plants/" + plant.getId(),
                HttpMethod.DELETE, withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> check = restTemplate.exchange(
                "/herbaria/" + bobHerbariumId + "/plants/" + plant.getId(),
                HttpMethod.GET, withAuth(bobToken), String.class
        );
        assertThat(check.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminDeletePlant_asNonAdmin_returns403() {
        PlantResponse plant = createPlantForBob();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + bobUserId + "/herbaria/" + bobHerbariumId + "/plants/" + plant.getId(),
                HttpMethod.DELETE, withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminDeletePhoto_lastPhoto_deletesPlantToo() {
        PlantResponse plant = createPlantForBob();
        UUID plantId = plant.getId();
        UUID photoId = plant.getPhotos().getFirst().getId();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + bobUserId + "/herbaria/" + bobHerbariumId
                        + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.DELETE, withAuth(adminToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> check = restTemplate.exchange(
                "/herbaria/" + bobHerbariumId + "/plants/" + plantId,
                HttpMethod.GET, withAuth(bobToken), String.class
        );
        assertThat(check.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminDeletePhoto_asNonAdmin_returns403() {
        PlantResponse plant = createPlantForBob();
        UUID plantId = plant.getId();
        UUID photoId = plant.getPhotos().getFirst().getId();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/users/" + bobUserId + "/herbaria/" + bobHerbariumId
                        + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.DELETE, withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
