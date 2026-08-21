package com.example.demo.transit.dto;

import com.example.demo.transit.domain.ReportType;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportResponse(
        Long id,
        ReportType reportType,
        Instant sentAt,
        Long routeId,
        Long stationId,
        BigDecimal newFare,
        String description,
        boolean autoApplied
) {
}
