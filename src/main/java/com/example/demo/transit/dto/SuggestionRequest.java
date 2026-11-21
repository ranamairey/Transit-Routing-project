package com.example.demo.transit.dto;

import com.example.demo.transit.domain.SuggestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A rider's suggestion: a missing place (name + map point), a stop in the wrong place, or a wrong line. */
public record SuggestionRequest(
        @NotNull SuggestionType type,
        @Size(max = 120) String name,
        @Size(max = 120) String nameEn,
        @Size(max = 600) String description,
        @Valid GeoPointDto location,
        String stationId,
        String routeId
) {
}
