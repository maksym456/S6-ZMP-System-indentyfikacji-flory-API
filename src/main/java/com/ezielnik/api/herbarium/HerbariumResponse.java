package com.ezielnik.api.herbarium;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;

public class HerbariumResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private boolean isPublic;
    private long plantCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public HerbariumResponse(Herbarium herbarium, long plantCount) {
        this.id = herbarium.getId();
        this.userId = herbarium.getUserId();
        this.name = herbarium.getName();
        this.description = herbarium.getDescription();
        this.isPublic = herbarium.isPublic();
        this.plantCount = plantCount;
        this.createdAt = herbarium.getCreatedAt();
        this.updatedAt = herbarium.getUpdatedAt();
    }

    @JsonCreator
    public HerbariumResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public long getPlantCount() {
        return plantCount;
    }

    public void setPlantCount(long plantCount) {
        this.plantCount = plantCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
