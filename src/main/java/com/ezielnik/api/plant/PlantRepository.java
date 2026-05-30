package com.ezielnik.api.plant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantRepository extends JpaRepository<Plant, UUID> {

    List<Plant> findByHerbarium_IdOrderByCreatedAtDesc(UUID herbariumId);

    List<Plant> findByHerbarium_IdAndUpdatedAtAfterOrderByCreatedAtDesc(UUID herbariumId, OffsetDateTime updatedSince);

    long countByHerbarium_Id(UUID herbariumId);

    void deleteByHerbarium_Id(UUID herbariumId);

    Optional<Plant> findByHerbarium_IdAndSpeciesId(UUID herbariumId, String speciesId);

    Optional<Plant> findByHerbarium_IdAndDetectedSpecies(UUID herbariumId, String detectedSpecies);

    List<Plant> findByHerbarium_IdAndDetectedSpeciesStartingWith(UUID herbariumId, String prefix);

    long countByDetectedSpeciesStartingWith(String prefix);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Plant p WHERE p.herbarium.user.id = :userId")
    long countByUserId(@org.springframework.data.repository.query.Param("userId") java.util.UUID userId);
}