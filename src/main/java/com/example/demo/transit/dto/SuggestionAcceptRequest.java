package com.example.demo.transit.dto;

import com.example.demo.transit.domain.StationKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * How an admin accepts a suggestion. For a missing place a station is created (name/kind may be edited);
 * for a wrong location the station is moved to the suggested point. Other types are only marked accepted.
 */
public record SuggestionAcceptRequest(
        @Size(max = 120) String name,
        @Size(max = 120) String nameEn,
        StationKind stationKind,
        String governorate,
        @Valid GeoPointDto location
) {
}
