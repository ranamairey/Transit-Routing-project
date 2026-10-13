package com.example.demo.transit.dto;

import com.example.demo.transit.domain.ReportType;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportResponse(
        String id,
        ReportType reportType,
        Instant sentAt,
        String routeId,
        String routeName,
        String stationId,
        String stationName,
        BigDecimal newFare,
        String description,
        GeoPointDto location,
        boolean autoApplied,
        boolean applied,
        boolean dismissed,
        String reviewedBy,
        Instant reviewedAt
) {
}
