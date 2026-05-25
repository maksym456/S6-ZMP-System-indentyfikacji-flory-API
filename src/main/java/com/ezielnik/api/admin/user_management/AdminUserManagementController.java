package com.ezielnik.api.admin.user_management;

import com.ezielnik.api.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/users")
public class AdminUserManagementController {

    private final AdminService adminService;

    public AdminUserManagementController(AdminService adminService) {
        this.adminService = adminService;
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
        return adminService.banUser(adminUserId, userId);
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
        return adminService.makeAdmin(adminUserId, userId);
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
        return adminService.sendAdminWarning(adminUserId, userId, request);
    }
    @Operation(
            summary = "List users",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @GetMapping()
    public List<AdminUserResponse> listUsers(@AuthenticationPrincipal Jwt jwt) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return adminService.listUsers(adminUserId);
    }

    @Operation(
            summary = "Unban/reactivate user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User unbanned successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot unban deleted account"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/unban")
    public String unbanUser(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable UUID userId) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return adminService.unbanUser(adminUserId, userId);
    }
    @Operation(
            summary = "Remove admin role from user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin role removed successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot remove your own admin role"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/remove-admin")
    public String removeAdmin(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID userId) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return adminService.removeAdmin(adminUserId, userId);
    }

    @Operation(
            summary = "Permanently delete a user account",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete your own account"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required or account inactive/unverified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{userId}")
    public String deleteUser(@AuthenticationPrincipal Jwt jwt,
                             @PathVariable UUID userId) {
        return adminService.adminDeleteUser(UUID.fromString(jwt.getSubject()), userId);
    }
}