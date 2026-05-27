package com.ezielnik.api.plant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

public class PlantIdentificationChoice {

    private boolean resolved;
    private PlantResponse plant;
    private String pendingPhotoId;
    private String status;
    private IdentificationInfo identification;
    private List<PlantResponse> recommendedPlants;

    private PlantIdentificationChoice(boolean resolved, PlantResponse plant, String pendingPhotoId,
                                      String status, IdentificationInfo identification,
                                      List<PlantResponse> recommendedPlants) {
        this.resolved = resolved;
        this.plant = plant;
        this.pendingPhotoId = pendingPhotoId;
        this.status = status;
        this.identification = identification;
        this.recommendedPlants = recommendedPlants;
    }

    @JsonCreator
    public PlantIdentificationChoice() {
    }

    public static PlantIdentificationChoice resolved(PlantResponse plant) {
        return new PlantIdentificationChoice(true, plant, null, null, null, null);
    }

    public static PlantIdentificationChoice recognized(String pendingPhotoId,
                                                       IdentificationInfo identification,
                                                       List<PlantResponse> recommendedPlants) {
        return new PlantIdentificationChoice(false, null, pendingPhotoId, "recognized", identification, recommendedPlants);
    }

    public static PlantIdentificationChoice unrecognized(String pendingPhotoId) {
        return new PlantIdentificationChoice(false, null, pendingPhotoId, "unrecognized", null, List.of());
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public PlantResponse getPlant() {
        return plant;
    }

    public void setPlant(PlantResponse plant) {
        this.plant = plant;
    }

    public String getPendingPhotoId() {
        return pendingPhotoId;
    }

    public void setPendingPhotoId(String pendingPhotoId) {
        this.pendingPhotoId = pendingPhotoId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public IdentificationInfo getIdentification() {
        return identification;
    }

    public void setIdentification(IdentificationInfo identification) {
        this.identification = identification;
    }

    public List<PlantResponse> getRecommendedPlants() {
        return recommendedPlants;
    }

    public void setRecommendedPlants(List<PlantResponse> recommendedPlants) {
        this.recommendedPlants = recommendedPlants;
    }

    public record IdentificationInfo(String detectedSpecies, Double confidence, String speciesId, String family,
                                     String genus, String commonNames) {

    }
}
