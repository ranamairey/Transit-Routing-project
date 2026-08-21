package com.example.demo.transit.dto;

import jakarta.validation.Valid;

public record RoutingSearchRequest(
        @Valid GeoPointDto origin,
        @Valid GeoPointDto destination,
        Long originStationId,
        Long destinationStationId,
        String originQuery,
        String destinationQuery,
        String governorate,
        boolean peak
) {
}
