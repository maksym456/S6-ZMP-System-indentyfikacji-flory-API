package com.ezielnik.api.plant;

import java.util.List;

public class PlantIdentificationChoice {

    private final boolean resolved;
    private final PlantResponse plant;
    private final String pendingPhotoId;
    private final String status;
    private final IdentificationInfo identification;
    private final List<PlantResponse> recommendedPlants;

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

    public PlantResponse getPlant() {
        return plant;
    }

    public String getPendingPhotoId() {
        return pendingPhotoId;
    }

    public String getStatus() {
        return status;
    }

    public IdentificationInfo getIdentification() {
        return identification;
    }

    public List<PlantResponse> getRecommendedPlants() {
        return recommendedPlants;
    }

    public record IdentificationInfo(String detectedSpecies, Double confidence, String speciesId, String family,
                                     String genus, String commonNames) {

    }
}
