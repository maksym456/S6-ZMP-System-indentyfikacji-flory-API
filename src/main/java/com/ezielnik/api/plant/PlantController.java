package com.ezielnik.api.plant;

import com.ezielnik.api.photo.MovePhotoRequest;
import com.ezielnik.api.photo.PhotoUpdateRequest;
import com.ezielnik.api.photo.PlantPhotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/herbaria/{herbariumId}/plants")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @Operation(summary = "Add a plant photo to a herbarium", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlantResponse addPlant(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID herbariumId,
                                  @RequestPart("photo") MultipartFile photo,
                                  @RequestParam(required = false) String photoDescription) {
        return plantService.addPlant(UUID.fromString(jwt.getSubject()), herbariumId, photo, photoDescription);
    }

    @Operation(summary = "Get all plants in a herbarium", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public List<PlantResponse> getPlants(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID herbariumId) {
        return plantService.getPlantsForHerbarium(UUID.fromString(jwt.getSubject()), herbariumId);
    }

    @Operation(summary = "Get a single plant", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{plantId}")
    public PlantResponse getPlant(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID herbariumId,
                                  @PathVariable UUID plantId) {
        return plantService.getPlant(UUID.fromString(jwt.getSubject()), herbariumId, plantId);
    }

    @Operation(summary = "Update a recognized plant's name", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{plantId}")
    public PlantResponse updatePlantName(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID herbariumId,
                                         @PathVariable UUID plantId,
                                         @RequestBody PlantUpdateRequest request) {
        return plantService.updatePlantName(UUID.fromString(jwt.getSubject()), herbariumId, plantId, request.getName());
    }

    @Operation(summary = "Delete a plant and all its photos", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{plantId}")
    public String deletePlant(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID herbariumId,
                              @PathVariable UUID plantId) {
        return plantService.deletePlant(UUID.fromString(jwt.getSubject()), herbariumId, plantId);
    }

    @Operation(summary = "Update a photo's description", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{plantId}/photos/{photoId}")
    public PlantPhotoResponse updatePhotoDescription(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID herbariumId,
                                                     @PathVariable UUID plantId,
                                                     @PathVariable UUID photoId,
                                                     @RequestBody PhotoUpdateRequest request) {
        return plantService.updatePhotoDescription(
                UUID.fromString(jwt.getSubject()), herbariumId, plantId, photoId, request.getDescription());
    }

    @Operation(summary = "Delete a photo (auto-deletes plant if it was the last photo)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{plantId}/photos/{photoId}")
    public String deletePhoto(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID herbariumId,
                              @PathVariable UUID plantId,
                              @PathVariable UUID photoId) {
        return plantService.deletePhoto(UUID.fromString(jwt.getSubject()), herbariumId, plantId, photoId);
    }

    @Operation(summary = "Move a photo between unrecognized plants", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{plantId}/photos/{photoId}/move")
    public PlantResponse movePhoto(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID herbariumId,
                                   @PathVariable UUID plantId,
                                   @PathVariable UUID photoId,
                                   @RequestBody MovePhotoRequest request) {
        return plantService.movePhoto(
                UUID.fromString(jwt.getSubject()), herbariumId, plantId, photoId, request.getTargetPlantId());
    }
}