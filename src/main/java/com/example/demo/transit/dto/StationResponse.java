package com.example.demo.transit.dto;

import com.example.demo.transit.domain.StationKind;

public record StationResponse(
        String id,
        String name,
        String aliases,
        StationKind stationKind,
        String governorate,
        GeoPointDto location
) {
}
