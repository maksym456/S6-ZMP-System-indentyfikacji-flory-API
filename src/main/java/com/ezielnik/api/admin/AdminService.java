package com.ezielnik.api.admin;

import com.ezielnik.api.admin.content_management.AdminHerbariumDetailResponse;
import com.ezielnik.api.admin.content_management.AdminHerbariumListItemResponse;
import com.ezielnik.api.admin.content_management.AdminHerbariumStatsResponse;
import com.ezielnik.api.admin.content_management.AdminPlantStatsResponse;
import com.ezielnik.api.admin.user_management.*;
import com.ezielnik.api.auth.EmailService;
import com.ezielnik.api.friend.FriendResponse;
import com.ezielnik.api.friend.FriendshipRepository;
import com.ezielnik.api.friend.FriendshipStatus;
import com.ezielnik.api.herbarium.Herbarium;
import com.ezielnik.api.herbarium.HerbariumRepository;
import com.ezielnik.api.herbarium.HerbariumResponse;
import com.ezielnik.api.notification.NotificationService;
import com.ezielnik.api.photo.PhotoStorageService;
import com.ezielnik.api.photo.PlantPhoto;
import com.ezielnik.api.photo.PlantPhotoRepository;
import com.ezielnik.api.plant.Plant;
import com.ezielnik.api.plant.PlantRepository;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import com.ezielnik.api.user.UserService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final HerbariumRepository herbariumRepository;
    private final PlantRepository plantRepository;
    private final PlantPhotoRepository plantPhotoRepository;
    private final FriendshipRepository friendshipRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final PhotoStorageService photoStorageService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        HerbariumRepository herbariumRepository,
                        PlantRepository plantRepository,
                        PlantPhotoRepository plantPhotoRepository,
                        FriendshipRepository friendshipRepository,
                        EmailService emailService,
                        NotificationService notificationService,
                        PhotoStorageService photoStorageService,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.herbariumRepository = herbariumRepository;
        this.plantRepository = plantRepository;
        this.plantPhotoRepository = plantPhotoRepository;
        this.friendshipRepository = friendshipRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.photoStorageService = photoStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    private void validateAdmin(UUID adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Admin user not found"
                ));

        if (!admin.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin account is inactive");
        }

        if (!admin.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(UUID adminUserId) {
        validateAdmin(adminUserId);

        return userRepository.findAll()
                .stream()
                .map(AdminUserResponse::new)
                .toList();
    }

    @Transactional
    public String banUser(UUID adminUserId, UUID targetUserId) {
        validateAdmin(adminUserId);

        if (adminUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot ban your own account");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!targetUser.isActive()) {
            return "User is already inactive";
        }

        targetUser.setActive(false);
        userRepository.save(targetUser);

        return "User banned successfully";
    }

    @Transactional
    public String makeAdmin(UUID adminUserId, UUID targetUserId) {
        validateAdmin(adminUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!targetUser.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be active to become an admin");
        }

        if (!targetUser.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be verified to become an admin");
        }

        if (targetUser.isAdmin()) {
            return "User is already an admin";
        }

        targetUser.setAdmin(true);
        userRepository.save(targetUser);

        return "User promoted to admin successfully";
    }

    @SuppressWarnings("SameReturnValue")
    @Transactional
    public String sendAdminWarning(UUID adminUserId, UUID targetUserId, AdminWarningRequest request) {
        validateAdmin(adminUserId);

        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warning subject is required");
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warning message is required");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!targetUser.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send warning to inactive user");
        }

        String subject = request.getSubject().trim();
        String message = request.getMessage().trim();

        emailService.sendAdminWarningEmail(
                targetUser.getEmail(),
                subject,
                message
        );

        notificationService.createNotification(
                targetUser,
                subject,
                message
        );

        return "Warning sent successfully";
    }

    @Transactional
    public String unbanUser(UUID adminUserId, UUID targetUserId) {
        validateAdmin(adminUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (targetUser.getEmail().endsWith("@deleted.local")
                || targetUser.getUsername().startsWith("deleted-user-")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot unban a deleted account");
        }

        if (targetUser.isActive()) {
            return "User is already active";
        }

        targetUser.setActive(true);
        userRepository.save(targetUser);

        return "User unbanned successfully";
    }

    @Transactional
    public String removeAdmin(UUID adminUserId, UUID targetUserId) {
        validateAdmin(adminUserId);

        if (adminUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove your own admin role");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!targetUser.isAdmin()) {
            return "User is not an admin";
        }

        targetUser.setAdmin(false);
        userRepository.save(targetUser);

        return "Admin role removed successfully";
    }

    @Transactional(readOnly = true)
    public AdminUserStatsResponse getStats(UUID adminUserId) {
        validateAdmin(adminUserId);

        List<User> users = userRepository.findAll();

        long totalUsers = users.size();
        long activeUsers = users.stream().filter(User::isActive).count();
        long inactiveUsers = totalUsers - activeUsers;
        long verifiedUsers = users.stream().filter(User::isVerified).count();
        long unverifiedUsers = totalUsers - verifiedUsers;
        long admins = users.stream().filter(User::isAdmin).count();

        return new AdminUserStatsResponse(
                totalUsers,
                activeUsers,
                inactiveUsers,
                verifiedUsers,
                unverifiedUsers,
                admins
        );
    }

    @Transactional(readOnly = true)
    public AdminHerbariumStatsResponse getHerbariumStats(UUID adminUserId) {
        validateAdmin(adminUserId);

        long totalHerbaria = herbariumRepository.count();
        long publicHerbaria = herbariumRepository.countByIsPublicTrue();
        long privateHerbaria = totalHerbaria - publicHerbaria;

        return new AdminHerbariumStatsResponse(totalHerbaria, publicHerbaria, privateHerbaria);
    }

    @Transactional(readOnly = true)
    public AdminPlantStatsResponse getPlantStats(UUID adminUserId) {
        validateAdmin(adminUserId);

        long totalPlants = plantRepository.count();
        long unrecognizedPlants = plantRepository.countByDetectedSpeciesStartingWith("NotDetected#");
        long recognizedPlants = totalPlants - unrecognizedPlants;
        long totalPhotos = plantPhotoRepository.count();

        return new AdminPlantStatsResponse(totalPlants, recognizedPlants, unrecognizedPlants, totalPhotos);
    }

    @Transactional(readOnly = true)
    public AdminFriendshipStatsResponse getFriendshipStats(UUID adminUserId) {
        validateAdmin(adminUserId);

        long totalFriendships = friendshipRepository.countByStatus(FriendshipStatus.ACCEPTED);
        long pendingRequests = friendshipRepository.countByStatus(FriendshipStatus.PENDING);

        return new AdminFriendshipStatsResponse(totalFriendships, pendingRequests);
    }

    @Transactional(readOnly = true)
    public AdminOverviewStatsResponse getOverviewStats(UUID adminUserId) {
        return new AdminOverviewStatsResponse(
                getStats(adminUserId),
                getHerbariumStats(adminUserId),
                getPlantStats(adminUserId),
                getFriendshipStats(adminUserId)
        );
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(UUID adminUserId, UUID targetUserId) {
        validateAdmin(adminUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        long herbariumCount = herbariumRepository.countByUser_Id(targetUserId);
        long plantCount = plantRepository.countByUserId(targetUserId);
        long photoCount = plantPhotoRepository.countByUserId(targetUserId);
        long friendCount = friendshipRepository.countAcceptedByUserId(targetUserId);

        List<HerbariumResponse> herbaria = herbariumRepository.findByUser_IdOrderByCreatedAtDesc(targetUserId)
                .stream()
                .map(h -> new HerbariumResponse(h, plantRepository.countByHerbarium_Id(h.getId())))
                .toList();

        return new AdminUserDetailResponse(targetUser, herbariumCount, plantCount, photoCount, friendCount, herbaria);
    }

    @Transactional(readOnly = true)
    public AdminUserFriendsResponse getUserFriends(UUID adminUserId, UUID targetUserId) {
        validateAdmin(adminUserId);

        if (!userRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<FriendResponse> accepted = friendshipRepository.findAcceptedByUserId(targetUserId)
                .stream()
                .map(f -> new FriendResponse(f, targetUserId))
                .toList();

        List<FriendResponse> incoming = friendshipRepository.findByAddressee_IdAndStatus(targetUserId, FriendshipStatus.PENDING)
                .stream()
                .map(f -> new FriendResponse(f, targetUserId))
                .toList();

        List<FriendResponse> sent = friendshipRepository.findByRequester_IdAndStatus(targetUserId, FriendshipStatus.PENDING)
                .stream()
                .map(f -> new FriendResponse(f, targetUserId))
                .toList();

        return new AdminUserFriendsResponse(accepted, incoming, sent);
    }

    @Transactional(readOnly = true)
    public List<AdminHerbariumListItemResponse> listHerbariaWithOwners(UUID adminUserId) {
        validateAdmin(adminUserId);

        return herbariumRepository.findAll()
                .stream()
                .map(h -> new AdminHerbariumListItemResponse(h, plantRepository.countByHerbarium_Id(h.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminHerbariumDetailResponse getHerbariumDetail(UUID adminUserId, UUID herbariumId) {
        validateAdmin(adminUserId);

        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        long plantCount = plantRepository.countByHerbarium_Id(herbariumId);
        long photoCount = plantPhotoRepository.countByHerbariumId(herbariumId);

        return new AdminHerbariumDetailResponse(herbarium, plantCount, photoCount);
    }

    @Transactional
    public String adminDeleteUser(UUID adminId, UUID targetUserId) {
        validateAdmin(adminId);

        if (adminId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (targetUser.getEmail().endsWith("@deleted.local")
                || targetUser.getUsername().startsWith("deleted-user-")) {
            return "User is already deleted";
        }

        UserService.deleteUser(targetUser, passwordEncoder, userRepository);

        return "User deleted successfully";
    }

    @SuppressWarnings("SameReturnValue")
    @Transactional
    public String adminDeleteHerbarium(UUID adminId, UUID targetUserId, UUID herbariumId) {
        validateAdmin(adminId);

        Herbarium herbarium = herbariumRepository.findById(herbariumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found"));

        if (!herbarium.getUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Herbarium not found");
        }

        return getString(herbariumId, herbarium, plantRepository, plantPhotoRepository, photoStorageService, herbariumRepository);
    }

    @SuppressWarnings("SameReturnValue")
    @NonNull
    public static String getString(UUID herbariumId, Herbarium herbarium, PlantRepository plantRepository, PlantPhotoRepository plantPhotoRepository, PhotoStorageService photoStorageService, HerbariumRepository herbariumRepository) {
        List<Plant> plants = plantRepository.findByHerbarium_IdOrderByCreatedAtDesc(herbariumId);
        if (!plants.isEmpty()) {
            List<UUID> plantIds = plants.stream().map(Plant::getId).toList();
            plantPhotoRepository.findByPlant_IdInOrderByCreatedAtAsc(plantIds)
                    .forEach(photo -> photoStorageService.delete(photo.getUrl()));
            plantPhotoRepository.deleteByPlant_IdIn(plantIds);
        }
        plantRepository.deleteByHerbarium_Id(herbariumId);
        herbariumRepository.delete(herbarium);

        return "Herbarium deleted successfully";
    }

    @SuppressWarnings("SameReturnValue")
    @Transactional
    public String adminDeletePlant(UUID adminId, UUID herbariumId, UUID plantId) {
        validateAdmin(adminId);

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

    @SuppressWarnings("SameReturnValue")
    @Transactional
    public String adminDeletePhoto(UUID adminId, UUID herbariumId, UUID plantId, UUID photoId) {
        validateAdmin(adminId);

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
}