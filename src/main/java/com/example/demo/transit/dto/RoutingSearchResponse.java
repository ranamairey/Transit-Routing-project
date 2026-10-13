package com.example.demo.transit.dto;

import java.util.List;

public record RoutingSearchResponse(
        GeoPointDto origin,
        GeoPointDto destination,
        String originName,
        String destinationName,
        boolean peak,
        List<RouteOptionDto> paths,
        List<AlertDto> alerts,
        long processingMillis
) {
}
