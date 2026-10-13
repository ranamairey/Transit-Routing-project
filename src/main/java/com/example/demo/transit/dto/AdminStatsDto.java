package com.example.demo.transit.dto;

import java.util.Map;

/** Numbers for the admin console dashboard. */
public record AdminStatsDto(
        long stations,
        long routes,
        long users,
        long reportsTotal,
        long reportsLast24h,
        long reportsPending,
        Map<String, Long> reportsByTypeLast24h,
        Map<String, Long> routesByStatus
) {
}
