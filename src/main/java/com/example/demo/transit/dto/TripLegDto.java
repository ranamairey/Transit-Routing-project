package com.example.demo.transit.dto;

import com.example.demo.transit.domain.VehicleType;

import java.math.BigDecimal;
import java.util.List;

public record TripLegDto(
        String type,
        String fromName,
        String toName,
        GeoPointDto from,
        GeoPointDto to,
        Long routeId,
        String routeName,
        VehicleType vehicleType,
        BigDecimal fare,
        int durationSeconds,
        double distanceMeters
) {
}
