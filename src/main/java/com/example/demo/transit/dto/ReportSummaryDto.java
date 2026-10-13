package com.example.demo.transit.dto;

import com.example.demo.transit.domain.ReportType;
import com.example.demo.transit.domain.RouteStatus;

import java.time.Instant;

/** FR-3.2: reports grouped by route and type inside the alert window. */
public record ReportSummaryDto(
        String routeId,
        String routeName,
        RouteStatus routeStatus,
        ReportType reportType,
        long reportCount,
        long distinctReporters,
        long pendingCount,
        int autoApplyThreshold,
        boolean thresholdReached,
        Instant latestAt
) {
}
