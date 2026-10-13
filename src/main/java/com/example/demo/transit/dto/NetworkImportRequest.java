package com.example.demo.transit.dto;

import com.example.demo.transit.domain.StationKind;
import com.example.demo.transit.domain.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bulk network import (upsert). Stations match on name + governorate; routes match on code,
 * else on name. Route stops reference stations by name (same file or already stored).
 */
public record NetworkImportRequest(
        @Valid List<StationItem> stations,
        @Valid List<RouteItem> routes
) {
    public record StationItem(
            @NotBlank String name,
            String nameEn,
            String aliases,
            StationKind stationKind,
            String governorate,
            @NotNull Double lat,
            @NotNull Double lng
    ) {
    }

    public record RouteItem(
            @NotBlank String name,
            String nameEn,
            String code,
            @NotNull VehicleType vehicleType,
            String governorate,
            @NotNull BigDecimal currentFare,
            @NotNull List<String> stations,
            @Valid List<GeoPointDto> polyline
    ) {
    }
}
