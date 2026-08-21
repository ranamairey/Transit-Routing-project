package com.example.demo.transit.dto;

import java.util.List;

public record RoutingSearchResponse(
        List<RouteOptionDto> paths,
        List<AlertDto> alerts,
        long processingMillis
) {
}
