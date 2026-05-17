package com.ezielnik.api.herbarium;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HerbariumRepository extends JpaRepository<Herbarium, UUID> {

    List<Herbarium> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<Herbarium> findByIsPublicTrueOrderByCreatedAtDesc();

    boolean existsByUser_IdAndName(UUID userId, String name);
}