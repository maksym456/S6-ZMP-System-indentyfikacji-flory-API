package com.ezielnik.api.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantPhotoRepository extends JpaRepository<PlantPhoto, UUID> {

    List<PlantPhoto> findByPlant_IdOrderByCreatedAtAsc(UUID plantId);

    List<PlantPhoto> findByPlant_IdInOrderByCreatedAtAsc(Collection<UUID> plantIds);

    Optional<PlantPhoto> findByUrl(String url);

    long countByPlant_Id(UUID plantId);

    void deleteByPlant_Id(UUID plantId);

    void deleteByPlant_IdIn(Collection<UUID> plantIds);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PlantPhoto p WHERE p.plant.herbarium.user.id = :userId")
    long countByUserId(@org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PlantPhoto p WHERE p.plant.herbarium.id = :herbariumId")
    long countByHerbariumId(@org.springframework.data.repository.query.Param("herbariumId") UUID herbariumId);
}