package com.ezielnik.api.plant;

import com.ezielnik.api.friend.FriendshipRepository;
import com.ezielnik.api.herbarium.Herbarium;
import com.ezielnik.api.herbarium.HerbariumRepository;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
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

    private static final String UNRECOGNIZED_NAME_PREFIX = "Unrecognized Plant #";
    private static final String NOT_DETECTED_PREFIX = "NotDetected#";
    private static final Set<String> TAXONOMIC_NOISE = Set.of(
            "spp.", "sp.", "var.", "subsp.", "f.", "cf.", "aff.", "×", "x"
    );

    private final PlantRepository plantRepository;
    private final PlantPhotoRepository plantPhotoRepository;
    private final HerbariumRepository herbariumRepository;
    private final PhotoStorageService photoStorageService;
    private final PlantIdentificationService plantIdentificationService;
    private final PendingPlantService pendingPlantService;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public PlantService(PlantRepository plantRepository,
                        PlantPhotoRepository plantPhotoRepository,
                        HerbariumRepository herbariumRepository,
                        PhotoStorageService photoStorageService,
                        PlantIdentificationService plantIdentificationService,
                        PendingPlantService pendingPlantService,
                        FriendshipRepository friendshipRepository,
                        UserRepository userRepository) {
        this.plantRepository = plantRepository;
        this.plantPhotoRepository = plantPhotoRepository;
        this.herbariumRepository = herbariumRepository;
        this.photoStorageService = photoStorageService;
        this.plantIdentificationService = plantIdentificationService;
        this.pendingPlantService = pendingPlantService;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    private Plant requirePlant(UUID plantId, UUID herbariumId) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));
        if (!plant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
        }
        return plant;
    }

    private PlantPhoto requirePhoto(UUID photoId, UUID plantId) {
        PlantPhoto photo = plantPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        if (!photo.getPlant().getId().equals(plantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }
        return photo;
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

        if (userId == null) {
            if (!herbarium.isPublic()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to access this herbarium");
            }
            return;
        }

        boolean isAdmin = userRepository.findById(userId).map(User::isAdmin).orElse(false);
        if (!herbarium.getUserId().equals(userId) && !herbarium.isPublic()
                && !isAdmin
                && !friendshipRepository.areFriends(userId, herbarium.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this herbarium");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    Optional<Plant> findExactMatch(UUID herbariumId, PlantIdentificationService.IdentificationResult id) {
        if (id.speciesId() != null) {
            Optional<Plant> match = plantRepository.findByHerbarium_IdAndSpeciesId(herbariumId, id.speciesId());
            if (match.isPresent()) return match;
        }
        return plantRepository.findByHerbarium_IdAndDetectedSpecies(herbariumId, id.detectedSpecies());
    }

    List<Plant> findWordMatches(UUID herbariumId, String detectedSpecies) {
        if (detectedSpecies == null) return List.of();
        Set<String> searchWords = tokenize(detectedSpecies);
        if (searchWords.isEmpty()) return List.of();

        return plantRepository.findByHerbarium_IdOrderByCreatedAtDesc(herbariumId)
                .stream()
                .filter(p -> p.getDetectedSpecies() != null)
                .filter(p -> !p.getDetectedSpecies().startsWith(NOT_DETECTED_PREFIX))
                .filter(p -> !Collections.disjoint(searchWords, tokenize(p.getDetectedSpecies())))
                .toList();
    }

    private Set<String> tokenize(String species) {
        return Arrays.stream(species.split("\\s+"))
                .map(String::toLowerCase)
                .filter(w -> !TAXONOMIC_NOISE.contains(w))
                .collect(Collectors.toSet());
    }

    Plant findOrCreateRecognizedPlant(Herbarium herbarium, PlantIdentificationService.IdentificationResult id) {
        Optional<Plant> existing = findExactMatch(herbarium.getId(), id);
        if (existing.isPresent()) return existing.get();

        Plant plant = new Plant(herbarium, id.detectedSpecies(),
                id.detectedSpecies(), id.speciesId(), id.family(), id.genus(), id.commonNames());
        return plantRepository.save(plant);
    }

    Plant createUnrecognizedPlant(Herbarium herbarium) {
        String name = nextUnrecognizedName(herbarium.getId());
        String detectedSpecies = nextNotDetectedSpecies(herbarium.getId());
        return plantRepository.save(new Plant(herbarium, name, detectedSpecies, null, null, null, null));
    }

    private String nextUnrecognizedName(UUID herbariumId) {
        List<Plant> unrecognized = plantRepository
                .findByHerbarium_IdAndDetectedSpeciesStartingWith(herbariumId, NOT_DETECTED_PREFIX);
        Set<Integer> used = new HashSet<>();
        for (Plant p : unrecognized) {
            if (p.getName() != null && p.getName().startsWith(UNRECOGNIZED_NAME_PREFIX)) {
                try {
                    used.add(Integer.parseInt(p.getName().substring(UNRECOGNIZED_NAME_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int n = 1;
        while (used.contains(n)) n++;
        return UNRECOGNIZED_NAME_PREFIX + n;
    }

    private String nextNotDetectedSpecies(UUID herbariumId) {
        int maxN = plantRepository
                .findByHerbarium_IdAndDetectedSpeciesStartingWith(herbariumId, NOT_DETECTED_PREFIX)
                .stream()
                .mapToInt(p -> {
                    try {
                        return Integer.parseInt(p.getDetectedSpecies().substring(NOT_DETECTED_PREFIX.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return NOT_DETECTED_PREFIX + (maxN + 1);
    }

    // ── public API ───────────────────────────────────────────────────────────

    @Transactional
    public PlantIdentificationChoice identifyPlant(UUID userId, UUID herbariumId,
                                                   MultipartFile photo, String photoDescription) {
        Herbarium herbarium = verifyOwner(herbariumId, userId);

        PlantIdentificationService.IdentificationResult id =
                plantIdentificationService.identify(photo);

        if (id.isRecognized()) {
            Optional<Plant> exactMatch = findExactMatch(herbariumId, id);
            if (exactMatch.isPresent()) {
                String photoUrl = photoStorageService.save(photo);
                plantPhotoRepository.save(
                        new PlantPhoto(exactMatch.get(), photoUrl, photoDescription, id.confidence()));
                List<PlantPhoto> photos = plantPhotoRepository
                        .findByPlant_IdOrderByCreatedAtAsc(exactMatch.get().getId());
                return PlantIdentificationChoice.resolved(new PlantResponse(exactMatch.get(), photos));
            }

            List<Plant> wordMatches = findWordMatches(herbariumId, id.detectedSpecies());
            if (!wordMatches.isEmpty()) {
                String pendingPhotoId = pendingPlantService.save(photo, id, photoDescription);
                List<PlantResponse> recommendations = wordMatches.stream()
                        .map(p -> new PlantResponse(p,
                                plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(p.getId())))
                        .toList();
                PlantIdentificationChoice.IdentificationInfo info =
                        new PlantIdentificationChoice.IdentificationInfo(
                                id.detectedSpecies(), id.confidence(), id.speciesId(),
                                id.family(), id.genus(), id.commonNames());
                return PlantIdentificationChoice.recognized(pendingPhotoId, info, recommendations);
            }

            Plant plant = findOrCreateRecognizedPlant(herbarium, id);
            String photoUrl = photoStorageService.save(photo);
            plantPhotoRepository.save(new PlantPhoto(plant, photoUrl, photoDescription, id.confidence()));
            List<PlantPhoto> photos = plantPhotoRepository
                    .findByPlant_IdOrderByCreatedAtAsc(plant.getId());
            return PlantIdentificationChoice.resolved(new PlantResponse(plant, photos));
        }

        String pendingPhotoId = pendingPlantService.save(photo, id, photoDescription);
        return PlantIdentificationChoice.unrecognized(pendingPhotoId);
    }

    @Transactional
    public PlantResponse confirmPlant(UUID userId, UUID herbariumId, PlantConfirmRequest request) {
        verifyOwner(herbariumId, userId);

        PendingPlantService.PendingEntry entry = pendingPlantService.consume(request.getPendingPhotoId());
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Pending photo not found or already confirmed");
        }

        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        Plant plant;
        String decisionType = request.getDecisionType();

        if ("existing".equals(decisionType)) {
            UUID targetId = request.getExistingPlantId();
            if (targetId == null) {
                photoStorageService.deletePendingFile(entry.pendingFilename());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "existingPlantId is required when decisionType is 'existing'");
            }
            plant = plantRepository.findById(targetId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found"));
            if (!plant.getHerbariumId().equals(herbariumId)) {
                photoStorageService.deletePendingFile(entry.pendingFilename());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plant not found");
            }
        } else if ("new".equals(decisionType)) {
            PlantIdentificationService.IdentificationResult id = entry.identification();
            if (id.isRecognized()) {
                plant = findOrCreateRecognizedPlant(herbarium, id);
            } else {
                plant = createUnrecognizedPlant(herbarium);
            }
        } else {
            photoStorageService.deletePendingFile(entry.pendingFilename());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "decisionType must be 'existing' or 'new'");
        }

        String photoUrl = photoStorageService.moveToPermanent(entry.pendingFilename());
        plantPhotoRepository.save(new PlantPhoto(plant, photoUrl, entry.photoDescription(),
                entry.identification().confidence()));

        List<PlantPhoto> photos = plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(plant.getId());
        return new PlantResponse(plant, photos);
    }

    @Transactional(readOnly = true)
    public List<PlantResponse> getPlantsForHerbarium(UUID userId, UUID herbariumId) {
        verifyAccess(herbariumId, userId);

        List<Plant> plants = plantRepository.findByHerbarium_IdOrderByCreatedAtDesc(herbariumId);
        if (plants.isEmpty()) return List.of();

        List<UUID> plantIds = plants.stream().map(Plant::getId).toList();
        Map<UUID, List<PlantPhoto>> photosByPlant = plantPhotoRepository
                .findByPlant_IdInOrderByCreatedAtAsc(plantIds)
                .stream().collect(Collectors.groupingBy(p -> p.getPlant().getId()));

        return plants.stream()
                .map(p -> new PlantResponse(p, photosByPlant.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlantResponse getPlant(UUID userId, UUID herbariumId, UUID plantId) {
        verifyAccess(herbariumId, userId);
        Plant plant = requirePlant(plantId, herbariumId);
        List<PlantPhoto> photos = plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(plantId);
        return new PlantResponse(plant, photos);
    }

    @Transactional
    public PlantResponse updatePlantName(UUID userId, UUID herbariumId, UUID plantId, String name) {
        verifyOwner(herbariumId, userId);
        Plant plant = requirePlant(plantId, herbariumId);

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plant name is required");
        }

        plant.setName(name.trim());
        plantRepository.save(plant);

        List<PlantPhoto> photos = plantPhotoRepository.findByPlant_IdOrderByCreatedAtAsc(plantId);
        return new PlantResponse(plant, photos);
    }

    @Transactional(readOnly = true)
    public PlantPhotoResponse getPhoto(UUID userId, UUID herbariumId, UUID plantId, UUID photoId) {
        verifyAccess(herbariumId, userId);
        requirePlant(plantId, herbariumId);
        PlantPhoto photo = requirePhoto(photoId, plantId);
        return new PlantPhotoResponse(photo);
    }

    @Transactional
    public PlantPhotoResponse updatePhotoDescription(UUID userId, UUID herbariumId, UUID plantId,
                                                     UUID photoId, String description) {
        verifyOwner(herbariumId, userId);
        requirePlant(plantId, herbariumId);
        PlantPhoto photo = requirePhoto(photoId, plantId);
        photo.setDescription(description == null || description.isBlank() ? null : description.trim());
        return new PlantPhotoResponse(plantPhotoRepository.save(photo));
    }

    @SuppressWarnings("SameReturnValue")
    @Transactional
    public String deletePhoto(UUID userId, UUID herbariumId, UUID plantId, UUID photoId) {
        verifyOwner(herbariumId, userId);
        Plant plant = requirePlant(plantId, herbariumId);
        PlantPhoto photo = requirePhoto(photoId, plantId);

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

        Plant targetPlant = plantRepository.findById(targetPlantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target plant not found"));

        if (!targetPlant.getHerbariumId().equals(herbariumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target plant not found");
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

    @SuppressWarnings("SameReturnValue")
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
