package com.ezielnik.api.admin.content_management;

public record AdminPlantStatsResponse(
        long totalPlants,
        long recognizedPlants,
        long unrecognizedPlants,
        long totalPhotos
) {}
