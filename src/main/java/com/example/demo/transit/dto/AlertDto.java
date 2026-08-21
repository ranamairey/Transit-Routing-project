package com.example.demo.transit.dto;

import com.example.demo.transit.domain.ReportType;

import java.time.Instant;

public record AlertDto(
        Long routeId,
        String routeName,
        ReportType reportType,
        String description,
        Instant sentAt
) {
}
