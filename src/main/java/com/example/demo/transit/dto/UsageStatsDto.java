package com.example.demo.transit.dto;

import java.time.Instant;
import java.util.List;

/** Trip-search statistics for the admin console. */
public record UsageStatsDto(
        int days,
        long searches,
        long noResult,
        List<DayCount> perDay,
        List<TripCount> topTrips,
        List<TripCount> topNoResult,
        List<PlaceCount> topDestinations
) {
    public record DayCount(String date, long searches, long noResult) {
    }

    public record TripCount(String origin, String destination, long count, Instant lastAt,
                            GeoPointDto originPoint, GeoPointDto destinationPoint) {
    }

    public record PlaceCount(String name, long count) {
    }
}
