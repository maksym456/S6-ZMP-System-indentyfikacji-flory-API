package com.ezielnik.api.photo;

import com.ezielnik.api.plant.Plant;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "plant_photos", schema = "public")
public class PlantPhoto {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "description")
    private String description;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PlantPhoto() {
    }

    public PlantPhoto(Plant plant, String url, String description, Double confidence) {
        this.plant = plant;
        this.url = url;
        this.description = description;
        this.confidence = confidence;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getConfidence() {
        return confidence;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}