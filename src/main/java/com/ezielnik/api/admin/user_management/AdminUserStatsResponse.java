package com.ezielnik.api.admin.user_management;

public record AdminUserStatsResponse(long totalUsers, long activeUsers, long inactiveUsers, long verifiedUsers,
                                     long unverifiedUsers, long admins) {

}