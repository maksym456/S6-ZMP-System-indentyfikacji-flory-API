package com.ezielnik.api.admin;

public class AdminUsersStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long verifiedUsers;
    private long unverifiedUsers;
    private long admins;

    public AdminUsersStatsResponse(long totalUsers,
                                   long activeUsers,
                                   long inactiveUsers,
                                   long verifiedUsers,
                                   long unverifiedUsers,
                                   long admins) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.inactiveUsers = inactiveUsers;
        this.verifiedUsers = verifiedUsers;
        this.unverifiedUsers = unverifiedUsers;
        this.admins = admins;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public long getInactiveUsers() {
        return inactiveUsers;
    }

    public long getVerifiedUsers() {
        return verifiedUsers;
    }

    public long getUnverifiedUsers() {
        return unverifiedUsers;
    }

    public long getAdmins() {
        return admins;
    }
}