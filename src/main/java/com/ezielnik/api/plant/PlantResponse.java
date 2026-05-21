package com.ezielnik.api.plant;

import com.ezielnik.api.photo.PlantPhoto;
import com.ezielnik.api.photo.PlantPhotoResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class PlantResponse {

    private final UUID id;
    private final UUID herbariumId;
    private final String name;
    private final String detectedSpecies;
    private final String speciesId;
    private final String family;
    private final String genus;
    private final String commonNames;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final List<PlantPhotoResponse> photos;

    public PlantResponse(Plant plant, List<PlantPhoto> photos) {
        this.id = plant.getId();
        this.herbariumId = plant.getHerbariumId();
        this.name = plant.getName();
        this.detectedSpecies = plant.getDetectedSpecies();
        this.speciesId = plant.getSpeciesId();
        this.family = plant.getFamily();
        this.genus = plant.getGenus();
        this.commonNames = plant.getCommonNames();
        this.createdAt = plant.getCreatedAt();
        this.updatedAt = plant.getUpdatedAt();
        this.photos = photos.stream().map(PlantPhotoResponse::new).toList();
    }

    public UUID getId() {
        return id;
    }

    public UUID getHerbariumId() {
        return herbariumId;
    }

    public String getName() {
        return name;
    }

    public String getDetectedSpecies() {
        return detectedSpecies;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public String getFamily() {
        return family;
    }

    public String getGenus() {
        return genus;
    }

    public String getCommonNames() {
        return commonNames;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<PlantPhotoResponse> getPhotos() {
        return photos;
    }
}