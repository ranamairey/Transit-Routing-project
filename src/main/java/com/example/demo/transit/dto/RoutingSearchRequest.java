package com.example.demo.transit.dto;

import jakarta.validation.Valid;

/**
 * FR-1.1: origin/destination given as GPS/pin coordinates, a station id, or a free-text query.
 * {@code peak} may be left null to detect peak hours automatically from Damascus local time.
 */
public record RoutingSearchRequest(
        @Valid GeoPointDto origin,
        @Valid GeoPointDto destination,
        String originStationId,
        String destinationStationId,
        String originQuery,
        String destinationQuery,
        String governorate,
        Boolean peak
) {
}
