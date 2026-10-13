package com.example.demo.transit.dto;

import java.util.List;

/** NFR-1.2 offline-first: the whole static network in one cacheable payload. */
public record NetworkSnapshotDto(
        String version,
        List<StationResponse> stations,
        List<RouteResponse> routes
) {
}
