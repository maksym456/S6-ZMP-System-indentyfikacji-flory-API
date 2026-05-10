package com.ezielnik.api.herbarium;

import java.time.OffsetDateTime;
import java.util.UUID;

public class HerbariumResponse {

    private final UUID id;
    private final UUID userId;
    private final String name;
    private final String description;
    private final boolean isPublic;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public HerbariumResponse(Herbarium herbarium) {
        this.id = herbarium.getId();
        this.userId = herbarium.getUserId();
        this.name = herbarium.getName();
        this.description = herbarium.getDescription();
        this.isPublic = herbarium.isPublic();
        this.createdAt = herbarium.getCreatedAt();
        this.updatedAt = herbarium.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}