package com.example.demo.transit.dto;

import com.example.demo.transit.domain.StationKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StationRequest(
        @NotBlank String name,
        String nameEn,
        String aliases,
        StationKind stationKind,
        String governorate,
        @NotNull @Valid GeoPointDto location
) {
}
