package com.ezielnik.api.admin;

public record AdminOverviewStatsResponse(
        AdminUserStatsResponse users,
        AdminHerbariumStatsResponse herbaria,
        AdminPlantStatsResponse plants,
        AdminFriendshipStatsResponse friendships
) {}
