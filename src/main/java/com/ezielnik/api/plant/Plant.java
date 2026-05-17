package com.ezielnik.api.plant;

import com.ezielnik.api.herbarium.Herbarium;
import com.ezielnik.api.photo.PlantPhoto;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plants", schema = "public")
public class Plant {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "herbarium_id", nullable = false)
    private Herbarium herbarium;

    @Column(name = "name")
    private String name;

    @Column(name = "is_recognized", nullable = false)
    private boolean isRecognized;

    @Column(name = "detected_species")
    private String detectedSpecies;

    @Column(name = "species_id")
    private String speciesId;

    @Column(name = "family")
    private String family;

    @Column(name = "genus")
    private String genus;

    @Column(name = "common_names")
    private String commonNames;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "plant", fetch = FetchType.LAZY)
    private final List<PlantPhoto> photos = new ArrayList<>();

    public Plant() {
    }

    public Plant(Herbarium herbarium, String name, boolean isRecognized,
                 String detectedSpecies, String speciesId, String family, String genus, String commonNames) {
        this.herbarium = herbarium;
        this.name = name;
        this.isRecognized = isRecognized;
        this.detectedSpecies = detectedSpecies;
        this.speciesId = speciesId;
        this.family = family;
        this.genus = genus;
        this.commonNames = commonNames;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Herbarium getHerbarium() {
        return herbarium;
    }

    public UUID getHerbariumId() {
        return herbarium.getId();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public boolean isRecognized() {
        return isRecognized;
    }

    public String getDetectedSpecies() {
        return detectedSpecies;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public String getFamily() {
        return family;
    }

    public String getGenus() {
        return genus;
    }

    public String getCommonNames() {
        return commonNames;
    }

    public List<PlantPhoto> getPhotos() {
        return photos;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}