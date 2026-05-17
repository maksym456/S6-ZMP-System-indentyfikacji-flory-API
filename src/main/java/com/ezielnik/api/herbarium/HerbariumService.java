package com.ezielnik.api.herbarium;

import com.ezielnik.api.plant.PhotoStorageService;
import com.ezielnik.api.plant.Plant;
import com.ezielnik.api.plant.PlantRepository;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class HerbariumService {

    private final HerbariumRepository herbariumRepository;
    private final UserRepository userRepository;
    private final PlantRepository plantRepository;
    private final PhotoStorageService photoStorageService;

    public HerbariumService(HerbariumRepository herbariumRepository,
                            UserRepository userRepository,
                            PlantRepository plantRepository,
                            PhotoStorageService photoStorageService) {
        this.herbariumRepository = herbariumRepository;
        this.userRepository = userRepository;
        this.plantRepository = plantRepository;
        this.photoStorageService = photoStorageService;
    }

    @Transactional
    public HerbariumResponse createHerbarium(UUID userId, HerbariumRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Herbarium name is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Herbarium herbarium = new Herbarium(
                user,
                request.getName().trim(),
                request.getDescription() == null ? null : request.getDescription().trim(),
                request.isPublic()
        );

        Herbarium savedHerbarium = herbariumRepository.save(herbarium);

        return new HerbariumResponse(savedHerbarium, 0);
    }

    @Transactional(readOnly = true)
    public List<HerbariumResponse> getMyHerbaria(UUID userId) {
        return herbariumRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(h -> new HerbariumResponse(h, plantRepository.countByHerbarium_Id(h.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public HerbariumResponse getHerbarium(UUID userId, UUID herbariumId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(userId) && !herbarium.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this herbarium");
        }

        return new HerbariumResponse(herbarium, plantRepository.countByHerbarium_Id(herbariumId));
    }

    @Transactional
    public HerbariumResponse updateHerbarium(UUID userId, UUID herbariumId, HerbariumRequest request) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update this herbarium");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Herbarium name is required");
        }

        herbarium.setName(request.getName());
        herbarium.setDescription(request.getDescription());
        herbarium.setPublic(request.isPublic());

        Herbarium savedHerbarium = herbariumRepository.save(herbarium);

        return new HerbariumResponse(savedHerbarium, plantRepository.countByHerbarium_Id(savedHerbarium.getId()));
    }

    @Transactional
    public String deleteHerbarium(UUID userId, UUID herbariumId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this herbarium");
        }

        plantRepository.findByHerbarium_IdOrderByCreatedAtDesc(herbariumId)
                .forEach(plant -> photoStorageService.delete(plant.getPhotoUrl()));

        plantRepository.deleteByHerbarium_Id(herbariumId);
        herbariumRepository.delete(herbarium);

        return "Herbarium deleted successfully";
    }

    @Transactional(readOnly = true)
    public List<HerbariumResponse> getPublicHerbaria() {
        return herbariumRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(h -> new HerbariumResponse(h, plantRepository.countByHerbarium_Id(h.getId())))
                .toList();
    }
}