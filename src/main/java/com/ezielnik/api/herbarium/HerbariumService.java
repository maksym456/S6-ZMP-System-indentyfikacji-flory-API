package com.ezielnik.api.herbarium;

import com.ezielnik.api.friend.FriendshipRepository;
import com.ezielnik.api.photo.PhotoStorageService;
import com.ezielnik.api.photo.PlantPhotoRepository;
import com.ezielnik.api.plant.PlantRepository;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.ezielnik.api.admin.AdminService.getString;

@Service
public class HerbariumService {

    private final HerbariumRepository herbariumRepository;
    private final UserRepository userRepository;
    private final PlantRepository plantRepository;
    private final PlantPhotoRepository plantPhotoRepository;
    private final PhotoStorageService photoStorageService;
    private final FriendshipRepository friendshipRepository;

    public HerbariumService(HerbariumRepository herbariumRepository,
                            UserRepository userRepository,
                            PlantRepository plantRepository,
                            PlantPhotoRepository plantPhotoRepository,
                            PhotoStorageService photoStorageService,
                            FriendshipRepository friendshipRepository) {
        this.herbariumRepository = herbariumRepository;
        this.userRepository = userRepository;
        this.plantRepository = plantRepository;
        this.plantPhotoRepository = plantPhotoRepository;
        this.photoStorageService = photoStorageService;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public HerbariumResponse createHerbarium(UUID userId, HerbariumRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Herbarium name is required");
        }

        String name = request.getName().trim();

        if (herbariumRepository.existsByUser_IdAndName(userId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A herbarium with this name already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Herbarium herbarium = new Herbarium(
                user,
                name,
                request.getDescription() == null ? null : request.getDescription().trim(),
                request.isPublic()
        );

        Herbarium savedHerbarium = herbariumRepository.save(herbarium);

        return new HerbariumResponse(savedHerbarium, 0);
    }

    @Transactional(readOnly = true)
    public List<HerbariumResponse> getMyHerbaria(UUID userId, OffsetDateTime updatedSince) {
        List<Herbarium> herbaria = updatedSince != null
                ? herbariumRepository.findByUser_IdAndUpdatedAtAfterOrderByCreatedAtDesc(userId, updatedSince)
                : herbariumRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return herbaria.stream()
                .map(h -> new HerbariumResponse(h, plantRepository.countByHerbarium_Id(h.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public HerbariumResponse getHerbarium(UUID userId, UUID herbariumId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(userId) && !herbarium.isPublic()
                && !isAdmin(userId)
                && !friendshipRepository.areFriends(userId, herbarium.getUserId())) {
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

        String newName = request.getName().trim();

        if (!herbarium.getName().equals(newName) && herbariumRepository.existsByUser_IdAndName(userId, newName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A herbarium with this name already exists");
        }

        herbarium.setName(newName);
        herbarium.setDescription(request.getDescription());
        herbarium.setPublic(request.isPublic());

        Herbarium savedHerbarium = herbariumRepository.save(herbarium);

        return new HerbariumResponse(savedHerbarium, plantRepository.countByHerbarium_Id(savedHerbarium.getId()));
    }

    @SuppressWarnings("SameReturnValue")
    @Transactional
    public String deleteHerbarium(UUID userId, UUID herbariumId) {
        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this herbarium");
        }

        return getString(herbariumId, herbarium, plantRepository, plantPhotoRepository, photoStorageService, herbariumRepository);
    }

    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId).map(com.ezielnik.api.user.User::isAdmin).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<HerbariumResponse> getUserHerbaria(UUID viewerId, UUID targetUserId) {
        boolean isFriend = friendshipRepository.areFriends(viewerId, targetUserId);
        boolean isAdmin = isAdmin(viewerId);

        return herbariumRepository.findByUser_IdOrderByCreatedAtDesc(targetUserId)
                .stream()
                .filter(h -> h.isPublic() || isFriend || isAdmin)
                .map(h -> new HerbariumResponse(h, plantRepository.countByHerbarium_Id(h.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HerbariumResponse> getPublicHerbaria() {
        return herbariumRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(h -> new HerbariumResponse(h, plantRepository.countByHerbarium_Id(h.getId())))
                .toList();
    }
}