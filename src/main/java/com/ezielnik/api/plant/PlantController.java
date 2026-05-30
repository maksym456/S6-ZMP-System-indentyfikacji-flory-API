package com.ezielnik.api.plant;

import com.ezielnik.api.photo.MovePhotoRequest;
import com.ezielnik.api.photo.PhotoUpdateRequest;
import com.ezielnik.api.photo.PlantPhotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/herbaria/{herbariumId}/plants")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @Operation(summary = "Add a plant to a herbarium", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plant added or pending identification choice returned"),
            @ApiResponse(responseCode = "400", description = "Photo is required or invalid format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium not found")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlantIdentificationChoice identifyPlant(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable UUID herbariumId,
                                                   @RequestPart("photo") MultipartFile photo,
                                                   @RequestParam(required = false) String photoDescription) {
        return plantService.identifyPlant(UUID.fromString(jwt.getSubject()), herbariumId, photo, photoDescription);
    }

    @Operation(summary = "Confirm a pending plant photo — attach to existing or new plant",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo confirmed and plant returned"),
            @ApiResponse(responseCode = "400", description = "Invalid decision type or missing existingPlantId"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium, pending photo, or target plant not found")
    })
    @PostMapping("/confirm")
    public PlantResponse confirmPlant(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID herbariumId,
                                      @RequestBody PlantConfirmRequest request) {
        return plantService.confirmPlant(UUID.fromString(jwt.getSubject()), herbariumId, request);
    }

    @Operation(summary = "Get all plants in a herbarium (public herbaria accessible without login)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plants returned"),
            @ApiResponse(responseCode = "401", description = "Herbarium is private and no credentials provided"),
            @ApiResponse(responseCode = "403", description = "Private herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium not found")
    })
    @GetMapping
    public List<PlantResponse> getPlants(Authentication authentication,
                                         @PathVariable UUID herbariumId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime updatedSince) {
        UUID userId = extractUserId(authentication);
        return plantService.getPlantsForHerbarium(userId, herbariumId, updatedSince);
    }

    @Operation(summary = "Get a single plant (public herbaria accessible without login)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plant returned"),
            @ApiResponse(responseCode = "401", description = "Herbarium is private and no credentials provided"),
            @ApiResponse(responseCode = "403", description = "Private herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium or plant not found")
    })
    @GetMapping("/{plantId}")
    public PlantResponse getPlant(Authentication authentication,
                                  @PathVariable UUID herbariumId,
                                  @PathVariable UUID plantId) {
        UUID userId = extractUserId(authentication);
        return plantService.getPlant(userId, herbariumId, plantId);
    }

    @Operation(summary = "Update a plant's name", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plant name updated"),
            @ApiResponse(responseCode = "400", description = "Name is required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium or plant not found")
    })
    @PatchMapping("/{plantId}")
    public PlantResponse updatePlantName(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable UUID herbariumId,
                                         @PathVariable UUID plantId,
                                         @RequestBody PlantUpdateRequest request) {
        return plantService.updatePlantName(UUID.fromString(jwt.getSubject()), herbariumId, plantId, request.getName());
    }

    @Operation(summary = "Delete a plant and all its photos", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plant deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium or plant not found")
    })
    @DeleteMapping("/{plantId}")
    public String deletePlant(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID herbariumId,
                              @PathVariable UUID plantId) {
        return plantService.deletePlant(UUID.fromString(jwt.getSubject()), herbariumId, plantId);
    }

    @Operation(summary = "Get a single photo's metadata (public herbaria accessible without login)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo returned"),
            @ApiResponse(responseCode = "401", description = "Herbarium is private and no credentials provided"),
            @ApiResponse(responseCode = "403", description = "Private herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium, plant, or photo not found")
    })
    @GetMapping("/{plantId}/photos/{photoId}")
    public PlantPhotoResponse getPhoto(Authentication authentication,
                                       @PathVariable UUID herbariumId,
                                       @PathVariable UUID plantId,
                                       @PathVariable UUID photoId) {
        UUID userId = extractUserId(authentication);
        return plantService.getPhoto(userId, herbariumId, plantId, photoId);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return UUID.fromString(jwtAuth.getToken().getSubject());
        }
        return null;
    }

    @Operation(summary = "Update a photo's description", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo description updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium, plant, or photo not found")
    })
    @PatchMapping("/{plantId}/photos/{photoId}")
    public PlantPhotoResponse updatePhotoDescription(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID herbariumId,
                                                     @PathVariable UUID plantId,
                                                     @PathVariable UUID photoId,
                                                     @RequestBody PhotoUpdateRequest request) {
        return plantService.updatePhotoDescription(
                UUID.fromString(jwt.getSubject()), herbariumId, plantId, photoId, request.getDescription());
    }

    @Operation(summary = "Delete a photo (auto-deletes plant if it was the last photo)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium, plant, or photo not found")
    })
    @DeleteMapping("/{plantId}/photos/{photoId}")
    public String deletePhoto(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID herbariumId,
                              @PathVariable UUID plantId,
                              @PathVariable UUID photoId) {
        return plantService.deletePhoto(UUID.fromString(jwt.getSubject()), herbariumId, plantId, photoId);
    }

    @Operation(summary = "Move a photo to another plant", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo moved to target plant"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium, plant, photo, or target plant not found")
    })
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
