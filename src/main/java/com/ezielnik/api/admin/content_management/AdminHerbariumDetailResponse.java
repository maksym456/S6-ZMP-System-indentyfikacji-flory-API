package com.ezielnik.api.admin.content_management;

import com.ezielnik.api.herbarium.Herbarium;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminHerbariumDetailResponse {

    private UUID id;
    private UUID ownerId;
    private String ownerUsername;
    private String ownerEmail;
    private String name;
    private String description;
    private boolean isPublic;
    private long plantCount;
    private long photoCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

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

    @JsonCreator
    public AdminHerbariumDetailResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public long getPlantCount() { return plantCount; }
    public void setPlantCount(long plantCount) { this.plantCount = plantCount; }

    public long getPhotoCount() { return photoCount; }
    public void setPhotoCount(long photoCount) { this.photoCount = photoCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
