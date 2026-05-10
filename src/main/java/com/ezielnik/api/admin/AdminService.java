package com.ezielnik.api.admin;

import com.ezielnik.api.auth.EmailService;
import com.ezielnik.api.notification.NotificationService;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        EmailService emailService,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    private User getActiveAdmin(UUID adminUserId) {
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

        return admin;
    }

    public List<AdminUserResponse> listUsers(UUID adminUserId) {
        getActiveAdmin(adminUserId);

        return userRepository.findAll()
                .stream()
                .map(AdminUserResponse::new)
                .toList();
    }

    public String banUser(UUID adminUserId, UUID targetUserId) {
        getActiveAdmin(adminUserId);

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

    public String makeAdmin(UUID adminUserId, UUID targetUserId) {
        getActiveAdmin(adminUserId);

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

    public String sendAdminWarning(UUID adminUserId, UUID targetUserId, AdminWarningRequest request) {
        getActiveAdmin(adminUserId);

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

    public String unbanUser(UUID adminUserId, UUID targetUserId) {
        getActiveAdmin(adminUserId);

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

    public String removeAdmin(UUID adminUserId, UUID targetUserId) {
        getActiveAdmin(adminUserId);

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

    public AdminUsersStatsResponse getStats(UUID adminUserId) {
        getActiveAdmin(adminUserId);

        List<User> users = userRepository.findAll();

        long totalUsers = users.size();
        long activeUsers = users.stream().filter(User::isActive).count();
        long inactiveUsers = totalUsers - activeUsers;
        long verifiedUsers = users.stream().filter(User::isVerified).count();
        long unverifiedUsers = totalUsers - verifiedUsers;
        long admins = users.stream().filter(User::isAdmin).count();

        return new AdminUsersStatsResponse(
                totalUsers,
                activeUsers,
                inactiveUsers,
                verifiedUsers,
                unverifiedUsers,
                admins
        );
    }
}