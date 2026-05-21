package com.ezielnik.api.admin;

public record AdminFriendshipStatsResponse(
        long totalFriendships,
        long pendingRequests
) {}
