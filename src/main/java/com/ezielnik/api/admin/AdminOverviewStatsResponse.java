package com.ezielnik.api.admin;

import com.ezielnik.api.admin.content_management.AdminHerbariumStatsResponse;
import com.ezielnik.api.admin.content_management.AdminPlantStatsResponse;
import com.ezielnik.api.admin.user_management.AdminFriendshipStatsResponse;
import com.ezielnik.api.admin.user_management.AdminUserStatsResponse;

public record AdminOverviewStatsResponse(
        AdminUserStatsResponse users,
        AdminHerbariumStatsResponse herbaria,
        AdminPlantStatsResponse plants,
        AdminFriendshipStatsResponse friendships
) {}
