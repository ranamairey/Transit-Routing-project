package com.example.demo.transit.dto;

import com.example.demo.transit.domain.ReportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReportRequest(
        @NotNull ReportType reportType,
        @NotBlank String routeId,
        String stationId,
        BigDecimal newFare,
        @Size(max = 500) String description,
        @Valid GeoPointDto location
) {
}
