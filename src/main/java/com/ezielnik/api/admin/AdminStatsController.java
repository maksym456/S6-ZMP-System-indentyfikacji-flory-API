package com.ezielnik.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/stats")
public class AdminStatsController {

    private final AdminService adminService;

    public AdminStatsController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Get system overview statistics", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview statistics returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified")
    })
    @GetMapping("/overview")
    public AdminOverviewStatsResponse getOverviewStats(@AuthenticationPrincipal Jwt jwt) {
        return adminService.getOverviewStats(UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "List all users with basic info", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified")
    })
    @GetMapping("/users")
    public List<AdminUserResponse> listUsers(@AuthenticationPrincipal Jwt jwt) {
        return adminService.listUsers(UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Get detailed info about a specific user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User detail returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{userId}")
    public AdminUserDetailResponse getUserDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {
        return adminService.getUserDetail(UUID.fromString(jwt.getSubject()), userId);
    }

    @Operation(summary = "Get friends, pending and sent requests for a user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User friends returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{userId}/friends")
    public AdminUserFriendsResponse getUserFriends(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId) {
        return adminService.getUserFriends(UUID.fromString(jwt.getSubject()), userId);
    }

    @Operation(summary = "List all herbaria with owner info", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbarium list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified")
    })
    @GetMapping("/herbaria")
    public List<AdminHerbariumListItemResponse> listHerbaria(@AuthenticationPrincipal Jwt jwt) {
        return adminService.listHerbariaWithOwners(UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Get detailed info about a specific herbarium", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Herbarium detail returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified"),
            @ApiResponse(responseCode = "404", description = "Herbarium not found")
    })
    @GetMapping("/herbaria/{herbariumId}")
    public AdminHerbariumDetailResponse getHerbariumDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID herbariumId) {
        return adminService.getHerbariumDetail(UUID.fromString(jwt.getSubject()), herbariumId);
    }
}
