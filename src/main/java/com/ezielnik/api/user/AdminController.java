package com.ezielnik.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Ban/deactivate user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User banned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/ban")
    public String banUser(@AuthenticationPrincipal Jwt jwt,
                          @PathVariable UUID userId) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return userService.banUser(adminUserId, userId);
    }

    @Operation(
            summary = "Promote user to admin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User promoted to admin successfully"),
            @ApiResponse(responseCode = "400", description = "User must be active and verified"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/make-admin")
    public String makeAdmin(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable UUID userId) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return userService.makeAdmin(adminUserId, userId);
    }

    @Operation(
            summary = "Send warning to user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warning sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{userId}/warning")
    public String sendAdminWarning(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID userId,
                                   @RequestBody AdminWarningRequest request) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return userService.sendAdminWarning(adminUserId, userId, request);
    }
}