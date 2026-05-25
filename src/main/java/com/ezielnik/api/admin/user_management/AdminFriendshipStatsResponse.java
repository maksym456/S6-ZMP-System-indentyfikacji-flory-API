package com.ezielnik.api.admin.user_management;

public record AdminFriendshipStatsResponse(
        long totalFriendships,
        long pendingRequests
) {}
