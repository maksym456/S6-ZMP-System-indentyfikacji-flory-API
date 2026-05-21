package com.ezielnik.api.friend;

import com.ezielnik.api.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public class FriendResponse {

    private final UUID friendshipId;
    private final UUID userId;
    private final String username;
    private final FriendshipStatus status;
    private final String direction;
    private final OffsetDateTime createdAt;

    public FriendResponse(Friendship friendship, UUID viewerId) {
        this.friendshipId = friendship.getId();
        User other = friendship.getOtherUser(viewerId);
        this.userId = other.getId();
        this.username = other.getUsername();
        this.status = friendship.getStatus();
        this.direction = friendship.getRequester().getId().equals(viewerId) ? "SENT" : "RECEIVED";
        this.createdAt = friendship.getCreatedAt();
    }

    public UUID getFriendshipId() {
        return friendshipId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public String getDirection() {
        return direction;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
