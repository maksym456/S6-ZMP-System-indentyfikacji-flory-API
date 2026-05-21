package com.ezielnik.api.plant;

import java.util.UUID;

public class PlantConfirmRequest {

    private String pendingPhotoId;
    private String decisionType;
    private UUID existingPlantId;

    public PlantConfirmRequest() {
    }

    public String getPendingPhotoId() {
        return pendingPhotoId;
    }

    public void setPendingPhotoId(String pendingPhotoId) {
        this.pendingPhotoId = pendingPhotoId;
    }

    public String getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(String decisionType) {
        this.decisionType = decisionType;
    }

    public UUID getExistingPlantId() {
        return existingPlantId;
    }

    public void setExistingPlantId(UUID existingPlantId) {
        this.existingPlantId = existingPlantId;
    }
}
