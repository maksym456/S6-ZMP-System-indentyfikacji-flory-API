package com.ezielnik.api.plant;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.herbarium.HerbariumRequest;
import com.ezielnik.api.herbarium.HerbariumResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class PlantIT extends IntegrationTestBase {

    private String aliceToken;
    private UUID herbariumId;

    @BeforeEach
    void setUpAliceAndHerbarium() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        aliceToken = loginAndGetToken("alice", "Password1!");
        herbariumId = createHerbarium(aliceToken).getId();
    }

    private HerbariumResponse createHerbarium(String token) {
        HerbariumRequest req = new HerbariumRequest();
        req.setName("My Garden");
        req.setPublic(false);
        ResponseEntity<HerbariumResponse> resp = restTemplate.exchange(
                "/herbaria", HttpMethod.POST, withAuth(req, token), HerbariumResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        HerbariumResponse body = resp.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    private ResponseEntity<PlantIdentificationChoice> addPlant(String token, UUID herbId, byte[] imageBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "test-photo.jpg";
            }
        });

        return restTemplate.exchange(
                "/herbaria/" + herbId + "/plants/add",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                PlantIdentificationChoice.class
        );
    }

    private ResponseEntity<PlantIdentificationChoice> addPlant(String token, UUID herbId) {
        return addPlant(token, herbId, new byte[]{1, 2, 3});
    }

    @Test
    void addPlant_unrecognized_returnsPendingChoiceWithId() {
        ResponseEntity<PlantIdentificationChoice> resp = addPlant(aliceToken, herbariumId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PlantIdentificationChoice choice = resp.getBody();
        assertThat(choice).isNotNull();
        assertThat(choice.isResolved()).isFalse();
        assertThat(choice.getStatus()).isEqualTo("unrecognized");
        assertThat(choice.getPendingPhotoId()).isNotBlank();
    }

    @Test
    void addPlant_recognized_newSpecies_resolvesDirectly() {
        Mockito.when(plantIdentificationService.identify(any()))
                .thenReturn(new PlantIdentificationService.IdentificationResult(
                        "Rosa canina", 0.95, "gbif-123", "Rosaceae", "Rosa", "Dog rose"
                ));

        ResponseEntity<PlantIdentificationChoice> resp = addPlant(aliceToken, herbariumId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PlantIdentificationChoice choice = resp.getBody();
        assertThat(choice).isNotNull();
        assertThat(choice.isResolved()).isTrue();
        assertThat(choice.getPlant()).isNotNull();
        assertThat(choice.getPlant().getDetectedSpecies()).isEqualTo("Rosa canina");
    }

    @Test
    void addPlant_recognized_sameSpeciesAlreadyExists_addPhotoToExisting() {
        Mockito.when(plantIdentificationService.identify(any()))
                .thenReturn(new PlantIdentificationService.IdentificationResult(
                        "Rosa canina", 0.95, "gbif-123", "Rosaceae", "Rosa", "Dog rose"
                ));

        addPlant(aliceToken, herbariumId);
        ResponseEntity<PlantIdentificationChoice> secondResp = addPlant(aliceToken, herbariumId);

        PlantIdentificationChoice choice = secondResp.getBody();
        assertThat(choice).isNotNull();
        assertThat(choice.isResolved()).isTrue();
        assertThat(choice.getPlant().getPhotos()).hasSize(2);
    }

    @Test
    void confirmPlant_new_unrecognized_createsMissingPlant() {
        ResponseEntity<PlantIdentificationChoice> addResp = addPlant(aliceToken, herbariumId);
        String pendingId = Objects.requireNonNull(addResp.getBody()).getPendingPhotoId();

        PlantConfirmRequest req = new PlantConfirmRequest();
        req.setPendingPhotoId(pendingId);
        req.setDecisionType("new");

        ResponseEntity<PlantResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/confirm",
                HttpMethod.POST, withAuth(req, aliceToken), PlantResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PlantResponse plant = resp.getBody();
        assertThat(plant).isNotNull();
        assertThat(plant.getName()).startsWith("Unrecognized Plant #");
        assertThat(plant.getPhotos()).hasSize(1);
    }

    @Test
    void confirmPlant_existing_attachesPhotoToExistingPlant() {
        // Create a recognized plant first
        Mockito.when(plantIdentificationService.identify(any()))
                .thenReturn(new PlantIdentificationService.IdentificationResult(
                        "Rosa canina", 0.95, "gbif-123", "Rosaceae", "Rosa", "Dog rose"
                ));
        ResponseEntity<PlantIdentificationChoice> firstAdd = addPlant(aliceToken, herbariumId);
        UUID existingPlantId = Objects.requireNonNull(firstAdd.getBody()).getPlant().getId();

        // Now add an unrecognized photo
        Mockito.when(plantIdentificationService.identify(any()))
                .thenReturn(PlantIdentificationService.IdentificationResult.empty());
        ResponseEntity<PlantIdentificationChoice> addResp = addPlant(aliceToken, herbariumId);
        String pendingId = Objects.requireNonNull(addResp.getBody()).getPendingPhotoId();

        // Confirm: attach to existing plant
        PlantConfirmRequest req = new PlantConfirmRequest();
        req.setPendingPhotoId(pendingId);
        req.setDecisionType("existing");
        req.setExistingPlantId(existingPlantId);

        ResponseEntity<PlantResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/confirm",
                HttpMethod.POST, withAuth(req, aliceToken), PlantResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var attachBody = resp.getBody();
        assertThat(attachBody).isNotNull();
        assertThat(attachBody.getPhotos()).hasSize(2);
    }

    @Test
    void getPlants_returnsAllPlantsInHerbarium() {
        // Create two plants
        ResponseEntity<PlantIdentificationChoice> add1 = addPlant(aliceToken, herbariumId);
        confirmPlantNew(Objects.requireNonNull(add1.getBody()).getPendingPhotoId());
        ResponseEntity<PlantIdentificationChoice> add2 = addPlant(aliceToken, herbariumId);
        confirmPlantNew(Objects.requireNonNull(add2.getBody()).getPendingPhotoId());

        ResponseEntity<List<PlantResponse>> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants", HttpMethod.GET,
                withAuth(aliceToken), new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void getPlant_validPlant_returnsPlant() {
        ResponseEntity<PlantIdentificationChoice> add = addPlant(aliceToken, herbariumId);
        PlantResponse created = confirmPlantNew(Objects.requireNonNull(add.getBody()).getPendingPhotoId());

        ResponseEntity<PlantResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + created.getId(),
                HttpMethod.GET, withAuth(aliceToken), PlantResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var getPlantBody = resp.getBody();
        assertThat(getPlantBody).isNotNull();
        assertThat(getPlantBody.getId()).isEqualTo(created.getId());
    }

    @Test
    void updatePlantName_changesName() {
        ResponseEntity<PlantIdentificationChoice> add = addPlant(aliceToken, herbariumId);
        PlantResponse plant = confirmPlantNew(Objects.requireNonNull(add.getBody()).getPendingPhotoId());

        PlantUpdateRequest updateReq = new PlantUpdateRequest();
        updateReq.setName("My Rose");

        ResponseEntity<PlantResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plant.getId(),
                HttpMethod.PATCH, withAuth(updateReq, aliceToken), PlantResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var updatedPlant = resp.getBody();
        assertThat(updatedPlant).isNotNull();
        assertThat(updatedPlant.getName()).isEqualTo("My Rose");
    }

    @Test
    void updatePlantName_emptyName_returns400() {
        ResponseEntity<PlantIdentificationChoice> add = addPlant(aliceToken, herbariumId);
        PlantResponse plant = confirmPlantNew(Objects.requireNonNull(add.getBody()).getPendingPhotoId());

        PlantUpdateRequest updateReq = new PlantUpdateRequest();
        updateReq.setName("");

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plant.getId(),
                HttpMethod.PATCH, withAuth(updateReq, aliceToken), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deletePlant_removesPlant() {
        ResponseEntity<PlantIdentificationChoice> add = addPlant(aliceToken, herbariumId);
        PlantResponse plant = confirmPlantNew(Objects.requireNonNull(add.getBody()).getPendingPhotoId());

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plant.getId(),
                HttpMethod.DELETE, withAuth(aliceToken), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> getResp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plant.getId(),
                HttpMethod.GET, withAuth(aliceToken), String.class
        );
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addPlant_otherUsersHerbarium_returns403() {
        registerAndVerify("bob", "bob@example.com", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bobToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() { return "test.jpg"; }
        });

        ResponseEntity<String> httpResp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/add",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
        assertThat(httpResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getPlants_withUpdatedSince_returnsOnlyUpdatedAfter() {
        ResponseEntity<PlantIdentificationChoice> add1 = addPlant(aliceToken, herbariumId);
        PlantResponse plant1 = confirmPlantNew(Objects.requireNonNull(add1.getBody()).getPendingPhotoId());
        ResponseEntity<PlantIdentificationChoice> add2 = addPlant(aliceToken, herbariumId);
        PlantResponse plant2 = confirmPlantNew(Objects.requireNonNull(add2.getBody()).getPendingPhotoId());

        jdbcTemplate.update("UPDATE plants SET updated_at = '2020-01-01 00:00:00+00' WHERE id = ?",
                plant1.getId());

        ResponseEntity<List<PlantResponse>> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants?updatedSince=2021-01-01T00:00:00Z",
                HttpMethod.GET, withAuth(aliceToken), new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<PlantResponse> body = resp.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.getFirst().getId()).isEqualTo(plant2.getId());
    }

    @Test
    void getPlants_withUpdatedSince_noMatch_returnsEmpty() {
        ResponseEntity<PlantIdentificationChoice> add = addPlant(aliceToken, herbariumId);
        PlantResponse plant = confirmPlantNew(Objects.requireNonNull(add.getBody()).getPendingPhotoId());

        jdbcTemplate.update("UPDATE plants SET updated_at = '2020-01-01 00:00:00+00' WHERE id = ?",
                plant.getId());

        ResponseEntity<List<PlantResponse>> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants?updatedSince=2021-01-01T00:00:00Z",
                HttpMethod.GET, withAuth(aliceToken), new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    void getPlants_publicHerbarium_accessibleWithoutAuth() {
        // Make herbarium public
        HerbariumRequest updateReq = new HerbariumRequest();
        updateReq.setName("My Garden");
        updateReq.setPublic(true);
        restTemplate.exchange("/herbaria/" + herbariumId, HttpMethod.PATCH,
                withAuth(updateReq, aliceToken), HerbariumResponse.class);

        ResponseEntity<List<PlantResponse>> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants", HttpMethod.GET,
                noAuth(), new ParameterizedTypeReference<>() {}
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private PlantResponse confirmPlantNew(String pendingPhotoId) {
        PlantConfirmRequest req = new PlantConfirmRequest();
        req.setPendingPhotoId(pendingPhotoId);
        req.setDecisionType("new");

        ResponseEntity<PlantResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/confirm",
                HttpMethod.POST, withAuth(req, aliceToken), PlantResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PlantResponse body = resp.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
