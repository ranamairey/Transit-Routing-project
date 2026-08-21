package com.example.demo.transit.dto;

import com.example.demo.transit.domain.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RouteRequest(
        @NotBlank String name,
        String code,
        @NotNull VehicleType vehicleType,
        String governorate,
        @NotNull BigDecimal currentFare,
        @NotEmpty List<Long> stationIds,
        @Valid List<GeoPointDto> polyline
) {
}
