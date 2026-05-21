package com.ezielnik.api.herbarium;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/herbaria")
public class HerbariumController {

    private final HerbariumService herbariumService;

    public HerbariumController(HerbariumService herbariumService) {
        this.herbariumService = herbariumService;
    }

    @Operation(
            summary = "Create herbarium",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Herbarium created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified"),
            @ApiResponse(responseCode = "409", description = "A herbarium with this name already exists")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public HerbariumResponse createHerbarium(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody HerbariumRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return herbariumService.createHerbarium(userId, request);
    }

    @Operation(
            summary = "Get my herbaria",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbaria returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified")
    })
    @GetMapping("/me")
    public List<HerbariumResponse> getMyHerbaria(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return herbariumService.getMyHerbaria(userId);
    }

    @Operation(summary = "Get public herbaria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public herbaria returned successfully")
    })
    @GetMapping("/public")
    public List<HerbariumResponse> getPublicHerbaria() {
        return herbariumService.getPublicHerbaria();
    }

    @Operation(summary = "Get another user's herbaria (public ones, or all if friends)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbaria returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified")
    })
    @GetMapping("/user/{userId}")
    public List<HerbariumResponse> getUserHerbaria(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID userId) {
        return herbariumService.getUserHerbaria(UUID.fromString(jwt.getSubject()), userId);
    }

    @Operation(
            summary = "Get herbarium by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbarium returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Private herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium not found")
    })
    @GetMapping("/{herbariumId}")
    public HerbariumResponse getHerbarium(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID herbariumId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return herbariumService.getHerbarium(userId, herbariumId);
    }

    @Operation(
            summary = "Update herbarium",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbarium updated successfully"),
            @ApiResponse(responseCode = "400", description = "Name is required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium not found"),
            @ApiResponse(responseCode = "409", description = "A herbarium with this name already exists")
    })
    @PatchMapping("/{herbariumId}")
    public HerbariumResponse updateHerbarium(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID herbariumId,
                                             @RequestBody HerbariumRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return herbariumService.updateHerbarium(userId, herbariumId, request);
    }

    @Operation(
            summary = "Delete herbarium",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbarium deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not your herbarium, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Herbarium not found")
    })
    @DeleteMapping("/{herbariumId}")
    public String deleteHerbarium(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID herbariumId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return herbariumService.deleteHerbarium(userId, herbariumId);
    }
}