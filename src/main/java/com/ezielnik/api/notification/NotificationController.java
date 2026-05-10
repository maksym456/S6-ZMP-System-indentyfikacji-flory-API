package com.ezielnik.api.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Get my notifications",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.getMyNotifications(userId);
    }

    @Operation(
            summary = "Get my unread notifications",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread notifications returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/unread")
    public List<NotificationResponse> getMyUnreadNotifications(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.getMyUnreadNotifications(userId);
    }

    @Operation(
            summary = "Mark notification as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "You cannot access this notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PatchMapping("/{notificationId}/read")
    public String markAsRead(@AuthenticationPrincipal Jwt jwt,
                             @PathVariable UUID notificationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.markAsRead(userId, notificationId);
    }

    @Operation(
            summary = "Mark all notifications as read",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/read-all")
    public String markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.markAllAsRead(userId);
    }
}