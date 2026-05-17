package com.ezielnik.api.plant;

import com.ezielnik.api.herbarium.Herbarium;
import com.ezielnik.api.herbarium.HerbariumRepository;
import com.ezielnik.api.photo.PhotoStorageService;
import com.ezielnik.api.photo.PlantPhoto;
import com.ezielnik.api.photo.PlantPhotoRepository;
import com.ezielnik.api.photo.PlantPhotoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final PlantPhotoRepository plantPhotoRepository;
    private final HerbariumRepository herbariumRepository;
    private final PhotoStorageService photoStorageService;
    private final PlantIdentificationService plantIdentificationService;

    public PlantService(PlantRepository plantRepository,
                        PlantPhotoRepository plantPhotoRepository,
                        HerbariumRepository herbariumRepository,
                        PhotoStorageService photoStorageService,
                        PlantIdentificationService plantIdentificationService) {
        this.plantRepository = plantRepository;
        this.plantPhotoRepository = plantPhotoRepository;
        this.herbariumRepository = herbariumRepository;
        this.photoStorageService = photoStorageService;
        this.plantIdentificationService = plantIdentificationService;
    }

    private Herbarium verifyOwner(UUID herbariumId, UUID userId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));
        if (!herbarium.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot modify this herbarium");
        }
        return herbarium;
    }

    private void verifyAccess(UUID herbariumId, UUID userId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));
        if (!herbarium.getUserId().equals(userId) && !herbarium.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this herbarium");
        }
    }

    @Transactional
    public PlantResponse addPlant(UUID userId, UUID herbariumId, MultipartFile photo, String photoDescription) {
        Herbarium herbarium = verifyOwner(herbariumId, userId);

        PlantIdentificationService.IdentificationResult identification = plantIdentificationService.identify(photo);
        String photoUrl = photoStorageService.save(photo);

        Plant plant;
        if (identification.isRecognized()) {
            plant = findOrCreateRecognizedPlant(herbarium, identification);
        } else {
            plant = createUnrecognizedPlant(herbarium);
        }

        String trimmedDescription = photoDescription == null || photoDescription.isBlank()
                ? null : photoDescription.trim();
        plantPhotoRepository.save(new PlantPhoto(plant, photoUrl, trimmedDescription, identification.confidence()));

        List<PlantPhoto> photos = plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(plant.getId());
        return new PlantResponse(plant, photos);
    }

    private Plant findOrCreateRecognizedPlant(Herbarium herbarium,
                                              PlantIdentificationService.IdentificationResult id) {
        Optional<Plant> existing = Optional.empty();
        if (id.speciesId() != null) {
            existing = plantRepository.findByHerbarium_IdAndSpeciesId(herbarium.getId(), id.speciesId());
        }
        if (existing.isEmpty()) {
            existing = plantRepository.findByHerbarium_IdAndDetectedSpecies(herbarium.getId(), id.detectedSpecies());
        }
        if (existing.isPresent()) {
            return existing.get();
        }

        Plant plant = new Plant(herbarium, id.detectedSpecies(), true,
                id.detectedSpecies(), id.speciesId(), id.family(), id.genus(), id.commonNames());
        return plantRepository.save(plant);
    }

    private Plant createUnrecognizedPlant(Herbarium herbarium) {
        String name = nextUnrecognizedName(herbarium.getId());
        Plant plant = new Plant(herbarium, name, false, null, null, null, null, null);
        return plantRepository.save(plant);
    }

    private String nextUnrecognizedName(UUID herbariumId) {
        List<Plant> unrecognized = plantRepository.findByHerbarium_IdAndIsRecognizedFalse(herbariumId);
        Set<Integer> usedNumbers = new HashSet<>();
        String prefix = "Unrecognized Plant #";
        for (Plant p : unrecognized) {
            if (p.getName() != null && p.getName().startsWith(prefix)) {
                try {
                    usedNumbers.add(Integer.parseInt(p.getName().substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int n = 1;
        while (usedNumbers.contains(n)) n++;
        return prefix + n;
    }

    @Transactional(readOnly = true)
    public List<PlantResponse> getPlantsForHerbarium(UUID userId, UUID herbariumId) {
        verifyAccess(herbariumId, userId);

        List<Plant> plants = plantRepository.findByHerbarium_IdOrderByCreatedAtDesc(herbariumId);
        if (plants.isEmpty()) return List.of();

        List<UUID> plantIds = plants.stream().map(Plant::getId).toList();
        Map<UUID, List<PlantPhoto>> photosByPlant = plantPhotoRepository.findByPlant_IdInOrderByCreatedAtAsc(plantIds)
                .stream().collect(Collectors.groupingBy(p -> p.getPlant().getId()));

        return plants.stream()
                .map(p -> new PlantResponse(p, photosByPlant.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlantResponse getPlant(UUID userId, UUID herbariumId, UUID plantId) {
        verifyAccess(herbariumId, userId);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));

        if (!plant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
        }

        List<PlantPhoto> photos = plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(plantId);
        return new PlantResponse(plant, photos);
    }

    @Transactional
    public PlantResponse updatePlantName(UUID userId, UUID herbariumId, UUID plantId, String name) {
        verifyOwner(herbariumId, userId);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));

        if (!plant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
        }

        if (!plant.isRecognized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot rename an unrecognized plant");
        }

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plant name is required");
        }

        plant.setName(name.trim());
        plantRepository.save(plant);

        List<PlantPhoto> photos = plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(plantId);
        return new PlantResponse(plant, photos);
    }

    @Transactional
    public PlantPhotoResponse updatePhotoDescription(UUID userId, UUID herbariumId, UUID plantId,
                                                     UUID photoId, String description) {
        verifyOwner(herbariumId, userId);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));

        if (!plant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
        }

        PlantPhoto photo = plantPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));

        if (!photo.getPlant().getId().equals(plantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }

        photo.setDescription(description == null || description.isBlank() ? null : description.trim());
        return new PlantPhotoResponse(plantPhotoRepository.save(photo));
    }

    @Transactional
    public String deletePhoto(UUID userId, UUID herbariumId, UUID plantId, UUID photoId) {
        verifyOwner(herbariumId, userId);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));

        if (!plant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
        }

        PlantPhoto photo = plantPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));

        if (!photo.getPlant().getId().equals(plantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }

        photoStorageService.delete(photo.getUrl());
        plantPhotoRepository.delete(photo);

        if (plantPhotoRepository.countByPlant_Id(plantId) == 0) {
            plantRepository.delete(plant);
        }

        return "Photo deleted successfully";
    }

    @Transactional
    public PlantResponse movePhoto(UUID userId, UUID herbariumId, UUID sourcePlantId,
                                   UUID photoId, UUID targetPlantId) {
        verifyOwner(herbariumId, userId);

        Plant sourcePlant = plantRepository.findById(sourcePlantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));

        if (!sourcePlant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
        }

        if (sourcePlant.isRecognized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only move photos from unrecognized plants");
        }

        Plant targetPlant = plantRepository.findById(targetPlantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target plant not found"));

        if (!targetPlant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target plant not found");
        }

        if (targetPlant.isRecognized()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only move photos to unrecognized plants");
        }

        PlantPhoto photo = plantPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));

        if (!photo.getPlant().getId().equals(sourcePlantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }

        photo.setPlant(targetPlant);
        plantPhotoRepository.save(photo);

        if (plantPhotoRepository.countByPlant_Id(sourcePlantId) == 0) {
            plantRepository.delete(sourcePlant);
        }

        List<PlantPhoto> photos = plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(targetPlantId);
        return new PlantResponse(targetPlant, photos);
    }

    @Transactional
    public String deletePlant(UUID userId, UUID herbariumId, UUID plantId) {
        verifyOwner(herbariumId, userId);

        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));

        if (!plant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
        }

        plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(plantId)
                .forEach(p -> photoStorageService.delete(p.getUrl()));
        plantPhotoRepository.deleteByPlant_Id(plantId);
        plantRepository.delete(plant);

        return "Plant deleted successfully";
    }
}