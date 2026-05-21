package com.ezielnik.api.admin;

import com.ezielnik.api.herbarium.Herbarium;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminHerbariumListItemResponse {

    private final UUID id;
    private final UUID ownerId;
    private final String ownerUsername;
    private final String name;
    private final String description;
    private final boolean isPublic;
    private final long plantCount;
    private final OffsetDateTime createdAt;

    public AdminHerbariumListItemResponse(Herbarium herbarium, long plantCount) {
        this.id = herbarium.getId();
        this.ownerId = herbarium.getUserId();
        this.ownerUsername = herbarium.getUser().getUsername();
        this.name = herbarium.getName();
        this.description = herbarium.getDescription();
        this.isPublic = herbarium.isPublic();
        this.plantCount = plantCount;
        this.createdAt = herbarium.getCreatedAt();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isPublic() { return isPublic; }
    public long getPlantCount() { return plantCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
