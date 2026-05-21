package com.ezielnik.api.friend;

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
@RequestMapping("/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @Operation(summary = "Send a friend request by username", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Friend request sent"),
            @ApiResponse(responseCode = "400", description = "Missing username or sending to yourself"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Already friends or request already exists")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/request")
    public FriendResponse sendRequest(@AuthenticationPrincipal Jwt jwt,
                                      @RequestBody FriendRequestBody body) {
        return friendshipService.sendRequest(UUID.fromString(jwt.getSubject()), body.getUsername());
    }

    @Operation(summary = "Accept a friend request", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friend request accepted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not the addressee of this request, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Friend request not found"),
            @ApiResponse(responseCode = "409", description = "Request is no longer pending")
    })
    @PostMapping("/{friendshipId}/accept")
    public FriendResponse acceptRequest(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID friendshipId) {
        return friendshipService.acceptRequest(UUID.fromString(jwt.getSubject()), friendshipId);
    }

    @Operation(summary = "Remove a friend or cancel/reject a request", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friendship removed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not part of this friendship, account inactive, or email not verified"),
            @ApiResponse(responseCode = "404", description = "Friendship not found")
    })
    @DeleteMapping("/{friendshipId}")
    public String removeFriendship(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID friendshipId) {
        return friendshipService.removeFriendship(UUID.fromString(jwt.getSubject()), friendshipId);
    }

    @Operation(summary = "Get accepted friends", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friends list returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified")
    })
    @GetMapping
    public List<FriendResponse> getFriends(@AuthenticationPrincipal Jwt jwt) {
        return friendshipService.getFriends(UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Get incoming friend requests", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incoming requests returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified")
    })
    @GetMapping("/requests")
    public List<FriendResponse> getIncomingRequests(@AuthenticationPrincipal Jwt jwt) {
        return friendshipService.getIncomingRequests(UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Get sent friend requests", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sent requests returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Account inactive or email not verified")
    })
    @GetMapping("/requests/sent")
    public List<FriendResponse> getSentRequests(@AuthenticationPrincipal Jwt jwt) {
        return friendshipService.getSentRequests(UUID.fromString(jwt.getSubject()));
    }
}
