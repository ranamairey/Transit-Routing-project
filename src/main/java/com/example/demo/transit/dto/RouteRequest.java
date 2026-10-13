package com.example.demo.transit.dto;

import com.example.demo.transit.domain.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RouteRequest(
        @NotBlank String name,
        String nameEn,
        String code,
        @NotNull VehicleType vehicleType,
        String governorate,
        @NotNull @DecimalMin("0") BigDecimal currentFare,
        @NotNull @Size(min = 2, message = "الخط يحتاج محطتين على الأقل") List<String> stationIds,
        @Valid List<GeoPointDto> polyline
) {
}
