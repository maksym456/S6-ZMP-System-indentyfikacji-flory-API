package com.ezielnik.api.herbarium;

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

    public HerbariumService(HerbariumRepository herbariumRepository,
                            UserRepository userRepository) {
        this.herbariumRepository = herbariumRepository;
        this.userRepository = userRepository;
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

        return new HerbariumResponse(savedHerbarium);
    }

    @Transactional(readOnly = true)
    public List<HerbariumResponse> getMyHerbaria(UUID userId) {
        return herbariumRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(HerbariumResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public HerbariumResponse getHerbarium(UUID userId, UUID herbariumId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(userId) && !herbarium.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this herbarium");
        }

        return new HerbariumResponse(herbarium);
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

        return new HerbariumResponse(savedHerbarium);
    }

    @Transactional
    public String deleteHerbarium(UUID userId, UUID herbariumId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this herbarium");
        }

        herbariumRepository.delete(herbarium);

        return "Herbarium deleted successfully";
    }

    @Transactional(readOnly = true)
    public List<HerbariumResponse> getPublicHerbaria() {
        return herbariumRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(HerbariumResponse::new)
                .toList();
    }
}