package com.example.demo.transit.dto;

import com.example.demo.transit.domain.ReportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReportRequest(
        @NotNull ReportType reportType,
        @NotNull Long routeId,
        Long stationId,
        BigDecimal newFare,
        String description,
        @Valid GeoPointDto location
) {
}
