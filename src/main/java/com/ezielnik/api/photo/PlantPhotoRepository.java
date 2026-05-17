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
}