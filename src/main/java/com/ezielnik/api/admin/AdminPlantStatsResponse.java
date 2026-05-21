package com.ezielnik.api.admin;

public record AdminPlantStatsResponse(
        long totalPlants,
        long recognizedPlants,
        long unrecognizedPlants,
        long totalPhotos
) {}
