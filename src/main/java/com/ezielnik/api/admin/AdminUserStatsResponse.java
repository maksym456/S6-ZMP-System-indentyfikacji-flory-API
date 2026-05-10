package com.ezielnik.api.admin;

public record AdminUserStatsResponse(long totalUsers, long activeUsers, long inactiveUsers, long verifiedUsers,
                                     long unverifiedUsers, long admins) {

}