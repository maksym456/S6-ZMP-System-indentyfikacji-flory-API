package com.ezielnik.api.friend;

import com.ezielnik.api.user.User;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;

public class FriendResponse {

    private UUID friendshipId;
    private UUID userId;
    private String username;
    private FriendshipStatus status;
    private String direction;
    private OffsetDateTime createdAt;

    public FriendResponse(Friendship friendship, UUID viewerId) {
        this.friendshipId = friendship.getId();
        User other = friendship.getOtherUser(viewerId);
        this.userId = other.getId();
        this.username = other.getUsername();
        this.status = friendship.getStatus();
        this.direction = friendship.getRequester().getId().equals(viewerId) ? "SENT" : "RECEIVED";
        this.createdAt = friendship.getCreatedAt();
    }

    @JsonCreator
    public FriendResponse() {
    }

    public UUID getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(UUID friendshipId) {
        this.friendshipId = friendshipId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
