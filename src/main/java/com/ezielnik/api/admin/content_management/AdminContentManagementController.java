package com.ezielnik.api.admin.content_management;

import com.ezielnik.api.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/{userId}")
public class AdminContentManagementController {

    private final AdminService adminService;

    public AdminContentManagementController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Delete a user's herbarium and all its contents",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbarium deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified"),
            @ApiResponse(responseCode = "404", description = "User or herbarium not found")
    })
    @DeleteMapping("/herbaria/{herbariumId}")
    public String deleteHerbarium(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID userId,
                                  @PathVariable UUID herbariumId) {
        return adminService.adminDeleteHerbarium(UUID.fromString(jwt.getSubject()), userId, herbariumId);
    }

    @Operation(summary = "Delete a plant and all its photos from a user's herbarium",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plant deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified"),
            @ApiResponse(responseCode = "404", description = "Herbarium or plant not found")
    })
    @DeleteMapping("/herbaria/{herbariumId}/plants/{plantId}")
    public String deletePlant(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID userId,
                              @PathVariable UUID herbariumId,
                              @PathVariable UUID plantId) {
        return adminService.adminDeletePlant(UUID.fromString(jwt.getSubject()), herbariumId, plantId);
    }

    @Operation(summary = "Delete a single photo from a plant (auto-deletes plant if it was the last photo)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified"),
            @ApiResponse(responseCode = "404", description = "Herbarium, plant, or photo not found")
    })
    @DeleteMapping("/herbaria/{herbariumId}/plants/{plantId}/photos/{photoId}")
    public String deletePhoto(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID userId,
                              @PathVariable UUID herbariumId,
                              @PathVariable UUID plantId,
                              @PathVariable UUID photoId) {
        return adminService.adminDeletePhoto(UUID.fromString(jwt.getSubject()), herbariumId, plantId, photoId);
    }
}
