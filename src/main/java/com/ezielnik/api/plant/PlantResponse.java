package com.ezielnik.api.plant;

import com.ezielnik.api.photo.PlantPhoto;
import com.ezielnik.api.photo.PlantPhotoResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class PlantResponse {

    private UUID id;
    private UUID herbariumId;
    private String name;
    private String detectedSpecies;
    private String speciesId;
    private String family;
    private String genus;
    private String commonNames;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<PlantPhotoResponse> photos;

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

    @JsonCreator
    public PlantResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getHerbariumId() {
        return herbariumId;
    }

    public void setHerbariumId(UUID herbariumId) {
        this.herbariumId = herbariumId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetectedSpecies() {
        return detectedSpecies;
    }

    public void setDetectedSpecies(String detectedSpecies) {
        this.detectedSpecies = detectedSpecies;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(String speciesId) {
        this.speciesId = speciesId;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getCommonNames() {
        return commonNames;
    }

    public void setCommonNames(String commonNames) {
        this.commonNames = commonNames;
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

    public List<PlantPhotoResponse> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PlantPhotoResponse> photos) {
        this.photos = photos;
    }
}
