package com.ezielnik.api.photo;

import java.util.UUID;

public class MovePhotoRequest {

    private UUID targetPlantId;

    public MovePhotoRequest() {
    }

    public UUID getTargetPlantId() {
        return targetPlantId;
    }

    public void setTargetPlantId(UUID targetPlantId) {
        this.targetPlantId = targetPlantId;
    }
}
