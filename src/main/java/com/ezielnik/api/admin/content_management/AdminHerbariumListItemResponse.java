package com.ezielnik.api.admin.content_management;

import com.ezielnik.api.herbarium.Herbarium;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminHerbariumListItemResponse {

    private UUID id;
    private UUID ownerId;
    private String ownerUsername;
    private String name;
    private String description;
    private boolean isPublic;
    private long plantCount;
    private OffsetDateTime createdAt;

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

    @JsonCreator
    public AdminHerbariumListItemResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public long getPlantCount() { return plantCount; }
    public void setPlantCount(long plantCount) { this.plantCount = plantCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
