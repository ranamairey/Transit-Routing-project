package com.example.demo.transit.dto;

import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.VehicleType;

import java.math.BigDecimal;

public record TripLegDto(
        String type,
        String fromName,
        String toName,
        GeoPointDto from,
        GeoPointDto to,
        String routeId,
        String routeName,
        VehicleType vehicleType,
        RouteStatus routeStatus,
        BigDecimal fare,
        int durationSeconds,
        double distanceMeters
) {
}
