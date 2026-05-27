package com.ezielnik.api.photo;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PlantPhotoResponse {

    private UUID id;
    private UUID plantId;
    private String url;
    private String description;
    private Double confidence;
    private OffsetDateTime createdAt;

    public PlantPhotoResponse(PlantPhoto photo) {
        this.id = photo.getId();
        this.plantId = photo.getPlant().getId();
        this.url = photo.getUrl();
        this.description = photo.getDescription();
        this.confidence = photo.getConfidence();
        this.createdAt = photo.getCreatedAt();
    }

    @JsonCreator
    public PlantPhotoResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
