package com.example.demo.transit.dto;

import com.example.demo.transit.domain.RouteStatus;

import java.time.Instant;
import java.util.Map;

/** Live state of one line: its effective status plus rider reports inside the alert window. */
public record LiveRouteStatusDto(
        String routeId,
        RouteStatus status,
        Instant statusUpdatedAt,
        Map<String, Long> reportCounts,
        Instant lastReportAt
) {
}
