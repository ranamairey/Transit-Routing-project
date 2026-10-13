package com.example.demo.transit.dto;

import com.example.demo.transit.domain.ReportType;

import java.time.Instant;

public record AlertDto(
        String id,
        String routeId,
        String routeName,
        ReportType reportType,
        String description,
        java.math.BigDecimal newFare,
        Instant sentAt,
        boolean applied
) {
}
