package com.ezielnik.api.admin;

public record AdminHerbariumStatsResponse(
        long totalHerbaria,
        long publicHerbaria,
        long privateHerbaria
) {}
