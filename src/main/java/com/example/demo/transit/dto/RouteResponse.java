package com.example.demo.transit.dto;

import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RouteResponse(
        String id,
        String name,
        String code,
        VehicleType vehicleType,
        String governorate,
        BigDecimal currentFare,
        RouteStatus status,
        Instant statusUpdatedAt,
        List<String> stationIds,
        List<StationResponse> stops,
        List<GeoPointDto> polyline
) {
}
