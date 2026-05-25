package com.ezielnik.api.admin.content_management;

import com.ezielnik.api.herbarium.Herbarium;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminHerbariumDetailResponse {

    private final UUID id;
    private final UUID ownerId;
    private final String ownerUsername;
    private final String ownerEmail;
    private final String name;
    private final String description;
    private final boolean isPublic;
    private final long plantCount;
    private final long photoCount;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public AdminHerbariumDetailResponse(Herbarium herbarium, long plantCount, long photoCount) {
        this.id = herbarium.getId();
        this.ownerId = herbarium.getUserId();
        this.ownerUsername = herbarium.getUser().getUsername();
        this.ownerEmail = herbarium.getUser().getEmail();
        this.name = herbarium.getName();
        this.description = herbarium.getDescription();
        this.isPublic = herbarium.isPublic();
        this.plantCount = plantCount;
        this.photoCount = photoCount;
        this.createdAt = herbarium.getCreatedAt();
        this.updatedAt = herbarium.getUpdatedAt();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isPublic() { return isPublic; }
    public long getPlantCount() { return plantCount; }
    public long getPhotoCount() { return photoCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
