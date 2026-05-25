package com.ezielnik.api.admin.user_management;

import com.ezielnik.api.herbarium.HerbariumResponse;
import com.ezielnik.api.user.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AdminUserDetailResponse {

    private final UUID id;
    private final String email;
    private final String username;
    private final boolean active;
    private final boolean verified;
    private final boolean admin;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final long herbariumCount;
    private final long plantCount;
    private final long photoCount;
    private final long friendCount;
    private final List<HerbariumResponse> herbaria;

    public AdminUserDetailResponse(User user, long herbariumCount, long plantCount,
                                   long photoCount, long friendCount,
                                   List<HerbariumResponse> herbaria) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.active = user.isActive();
        this.verified = user.isVerified();
        this.admin = user.isAdmin();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.herbariumCount = herbariumCount;
        this.plantCount = plantCount;
        this.photoCount = photoCount;
        this.friendCount = friendCount;
        this.herbaria = herbaria;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public boolean isActive() { return active; }
    public boolean isVerified() { return verified; }
    public boolean isAdmin() { return admin; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public long getHerbariumCount() { return herbariumCount; }
    public long getPlantCount() { return plantCount; }
    public long getPhotoCount() { return photoCount; }
    public long getFriendCount() { return friendCount; }
    public List<HerbariumResponse> getHerbaria() { return herbaria; }
}
