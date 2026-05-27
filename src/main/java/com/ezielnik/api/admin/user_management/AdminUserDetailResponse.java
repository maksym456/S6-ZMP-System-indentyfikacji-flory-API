package com.ezielnik.api.admin.user_management;

import com.ezielnik.api.herbarium.HerbariumResponse;
import com.ezielnik.api.user.User;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AdminUserDetailResponse {

    private UUID id;
    private String email;
    private String username;
    private boolean active;
    private boolean verified;
    private boolean admin;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private long herbariumCount;
    private long plantCount;
    private long photoCount;
    private long friendCount;
    private List<HerbariumResponse> herbaria;

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

    @JsonCreator
    public AdminUserDetailResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public long getHerbariumCount() { return herbariumCount; }
    public void setHerbariumCount(long herbariumCount) { this.herbariumCount = herbariumCount; }

    public long getPlantCount() { return plantCount; }
    public void setPlantCount(long plantCount) { this.plantCount = plantCount; }

    public long getPhotoCount() { return photoCount; }
    public void setPhotoCount(long photoCount) { this.photoCount = photoCount; }

    public long getFriendCount() { return friendCount; }
    public void setFriendCount(long friendCount) { this.friendCount = friendCount; }

    public List<HerbariumResponse> getHerbaria() { return herbaria; }
    public void setHerbaria(List<HerbariumResponse> herbaria) { this.herbaria = herbaria; }
}
