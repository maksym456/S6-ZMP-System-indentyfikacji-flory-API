package com.ezielnik.api.photo;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PlantPhotoResponse {

    private final UUID id;
    private final UUID plantId;
    private final String url;
    private final String description;
    private final Double confidence;
    private final OffsetDateTime createdAt;

    public PlantPhotoResponse(PlantPhoto photo) {
        this.id = photo.getId();
        this.plantId = photo.getPlant().getId();
        this.url = photo.getUrl();
        this.description = photo.getDescription();
        this.confidence = photo.getConfidence();
        this.createdAt = photo.getCreatedAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public Double getConfidence() {
        return confidence;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
