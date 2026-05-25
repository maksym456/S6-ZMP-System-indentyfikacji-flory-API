package com.ezielnik.api.admin.content_management;

public record AdminHerbariumStatsResponse(
        long totalHerbaria,
        long publicHerbaria,
        long privateHerbaria
) {}
