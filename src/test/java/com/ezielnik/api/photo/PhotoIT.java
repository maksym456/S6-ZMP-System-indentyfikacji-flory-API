package com.ezielnik.api.photo;

import com.ezielnik.api.IntegrationTestBase;
import com.ezielnik.api.herbarium.HerbariumRequest;
import com.ezielnik.api.herbarium.HerbariumResponse;
import com.ezielnik.api.plant.PlantConfirmRequest;
import com.ezielnik.api.plant.PlantIdentificationChoice;
import com.ezielnik.api.plant.PlantResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoIT extends IntegrationTestBase {

    private String aliceToken;
    private UUID herbariumId;

    @BeforeEach
    void setUpAlice() {
        registerAndVerify("alice", "alice@example.com", "Password1!");
        aliceToken = loginAndGetToken("alice", "Password1!");

        HerbariumRequest req = new HerbariumRequest();
        req.setName("My Garden");
        req.setPublic(false);
        HerbariumResponse herb = restTemplate.exchange(
                "/herbaria", HttpMethod.POST, withAuth(req, aliceToken), HerbariumResponse.class
        ).getBody();
        assertThat(herb).isNotNull();
        herbariumId = herb.getId();
    }

    private PlantResponse createPlantWithPhoto() {
        PlantIdentificationChoice choice = addPlant(aliceToken, herbariumId);
        return confirmNew(choice.getPendingPhotoId());
    }

    private PlantIdentificationChoice addPlant(String token, UUID herbId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override
            public String getFilename() { return "test.jpg"; }
        });

        PlantIdentificationChoice choice = restTemplate.exchange(
                "/herbaria/" + herbId + "/plants/add",
                HttpMethod.POST, new HttpEntity<>(body, headers),
                PlantIdentificationChoice.class
        ).getBody();
        assertThat(choice).isNotNull();
        return choice;
    }

    private PlantResponse confirmNew(String pendingPhotoId) {
        PlantConfirmRequest req = new PlantConfirmRequest();
        req.setPendingPhotoId(pendingPhotoId);
        req.setDecisionType("new");
        PlantResponse plant = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/confirm",
                HttpMethod.POST, withAuth(req, aliceToken), PlantResponse.class
        ).getBody();
        assertThat(plant).isNotNull();
        return plant;
    }

    @Test
    void getPhotoMetadata_returnsPhotoDetails() {
        PlantResponse plant = createPlantWithPhoto();
        UUID plantId = plant.getId();
        UUID photoId = plant.getPhotos().getFirst().getId();

        ResponseEntity<PlantPhotoResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.GET, withAuth(aliceToken), PlantPhotoResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var photoBody = resp.getBody();
        assertThat(photoBody).isNotNull();
        assertThat(photoBody.getId()).isEqualTo(photoId);
        assertThat(photoBody.getUrl()).isNotBlank();
    }

    @Test
    void updatePhotoDescription_setsDescription() {
        PlantResponse plant = createPlantWithPhoto();
        UUID plantId = plant.getId();
        UUID photoId = plant.getPhotos().getFirst().getId();

        PhotoUpdateRequest updateReq = new PhotoUpdateRequest();
        updateReq.setDescription("A beautiful flower");

        ResponseEntity<PlantPhotoResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.PATCH, withAuth(updateReq, aliceToken), PlantPhotoResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var setDescBody = resp.getBody();
        assertThat(setDescBody).isNotNull();
        assertThat(setDescBody.getDescription()).isEqualTo("A beautiful flower");
    }

    @Test
    void updatePhotoDescription_clearDescription() {
        PlantResponse plant = createPlantWithPhoto();
        UUID plantId = plant.getId();
        UUID photoId = plant.getPhotos().getFirst().getId();

        PhotoUpdateRequest setReq = new PhotoUpdateRequest();
        setReq.setDescription("Initial description");
        restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.PATCH, withAuth(setReq, aliceToken), PlantPhotoResponse.class
        );

        PhotoUpdateRequest clearReq = new PhotoUpdateRequest();
        ResponseEntity<PlantPhotoResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.PATCH, withAuth(clearReq, aliceToken), PlantPhotoResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var clearDescBody = resp.getBody();
        assertThat(clearDescBody).isNotNull();
        assertThat(clearDescBody.getDescription()).isNull();
    }

    @Test
    void deletePhoto_lastPhoto_deletesPlantToo() {
        PlantResponse plant = createPlantWithPhoto();
        UUID plantId = plant.getId();
        UUID photoId = plant.getPhotos().getFirst().getId();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.DELETE, withAuth(aliceToken), String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Plant should be auto-deleted since it had only one photo
        ResponseEntity<String> plantResp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId,
                HttpMethod.GET, withAuth(aliceToken), String.class
        );
        assertThat(plantResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletePhoto_notLastPhoto_keepPlant() {
        PlantResponse plant = createPlantWithPhoto();
        UUID plantId = plant.getId();

        // Add a second photo to the same plant
        PlantIdentificationChoice secondAdd = addPlant(aliceToken, herbariumId);
        PlantConfirmRequest req = new PlantConfirmRequest();
        req.setPendingPhotoId(secondAdd.getPendingPhotoId());
        req.setDecisionType("existing");
        req.setExistingPlantId(plantId);
        restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/confirm",
                HttpMethod.POST, withAuth(req, aliceToken), PlantResponse.class
        );

        UUID firstPhotoId = plant.getPhotos().getFirst().getId();

        restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId + "/photos/" + firstPhotoId,
                HttpMethod.DELETE, withAuth(aliceToken), String.class
        );

        ResponseEntity<PlantResponse> plantResp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId,
                HttpMethod.GET, withAuth(aliceToken), PlantResponse.class
        );
        assertThat(plantResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var keepPlantBody = plantResp.getBody();
        assertThat(keepPlantBody).isNotNull();
        assertThat(keepPlantBody.getPhotos()).hasSize(1);
    }

    @Test
    void movePhoto_movesToTargetPlant() {
        PlantResponse plant1 = createPlantWithPhoto();
        PlantResponse plant2 = createPlantWithPhoto();

        UUID photoId = plant1.getPhotos().getFirst().getId();

        MovePhotoRequest moveReq = new MovePhotoRequest();
        moveReq.setTargetPlantId(plant2.getId());

        ResponseEntity<PlantResponse> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plant1.getId() + "/photos/" + photoId + "/move",
                HttpMethod.POST, withAuth(moveReq, aliceToken), PlantResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var movedBody = resp.getBody();
        assertThat(movedBody).isNotNull();
        assertThat(movedBody.getPhotos()).hasSize(2);

        // Source plant should be auto-deleted (only had 1 photo)
        ResponseEntity<String> sourceResp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plant1.getId(),
                HttpMethod.GET, withAuth(aliceToken), String.class
        );
        assertThat(sourceResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPhotoMetadata_wrongPlantId_returns404() {
        PlantResponse plant = createPlantWithPhoto();
        UUID wrongPlantId = UUID.randomUUID();
        UUID photoId = plant.getPhotos().getFirst().getId();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + wrongPlantId + "/photos/" + photoId,
                HttpMethod.GET, withAuth(aliceToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPhotoMetadata_otherUsersPrivateHerbarium_returns403() {
        registerAndVerify("bob", "bob@example.com", "Password1!");
        String bobToken = loginAndGetToken("bob", "Password1!");

        PlantResponse plant = createPlantWithPhoto();
        UUID plantId = plant.getId();
        UUID photoId = plant.getPhotos().getFirst().getId();

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/" + plantId + "/photos/" + photoId,
                HttpMethod.GET, withAuth(bobToken), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void uploadPhoto_exceedsFileSizeLimit_returns413() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(aliceToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        byte[] oversizedFile = new byte[2048];
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new ByteArrayResource(oversizedFile) {
            @Override
            public String getFilename() { return "big.jpg"; }
        });

        ResponseEntity<String> resp = restTemplate.exchange(
                "/herbaria/" + herbariumId + "/plants/add",
                HttpMethod.POST, new HttpEntity<>(body, headers), String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    }
}
