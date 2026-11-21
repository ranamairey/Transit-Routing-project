package com.example.demo.transit.dto;

import com.example.demo.transit.domain.SuggestionStatus;
import com.example.demo.transit.domain.SuggestionType;

import java.time.Instant;

public record SuggestionResponse(
        String id,
        SuggestionType type,
        SuggestionStatus status,
        String name,
        String nameEn,
        String description,
        GeoPointDto location,
        String stationId,
        String stationName,
        GeoPointDto stationLocation,
        String routeId,
        String routeName,
        String reporterId,
        Instant createdAt,
        String reviewedBy,
        Instant reviewedAt,
        String resultStationId
) {
}
