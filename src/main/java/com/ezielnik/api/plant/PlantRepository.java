package com.ezielnik.api.plant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantRepository extends JpaRepository<Plant, UUID> {

    List<Plant> findByHerbarium_IdOrderByCreatedAtDesc(UUID herbariumId);

    long countByHerbarium_Id(UUID herbariumId);

    void deleteByHerbarium_Id(UUID herbariumId);

    Optional<Plant> findByHerbarium_IdAndSpeciesId(UUID herbariumId, String speciesId);

    Optional<Plant> findByHerbarium_IdAndDetectedSpecies(UUID herbariumId, String detectedSpecies);

    List<Plant> findByHerbarium_IdAndIsRecognizedFalse(UUID herbariumId);
}