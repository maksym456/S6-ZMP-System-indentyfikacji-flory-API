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

    public static class IdentificationInfo {

        private final String detectedSpecies;
        private final Double confidence;
        private final String speciesId;
        private final String family;
        private final String genus;
        private final String commonNames;

        public IdentificationInfo(String detectedSpecies, Double confidence, String speciesId,
                                  String family, String genus, String commonNames) {
            this.detectedSpecies = detectedSpecies;
            this.confidence = confidence;
            this.speciesId = speciesId;
            this.family = family;
            this.genus = genus;
            this.commonNames = commonNames;
        }

        public String getDetectedSpecies() {
            return detectedSpecies;
        }

        public Double getConfidence() {
            return confidence;
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
    }
}
