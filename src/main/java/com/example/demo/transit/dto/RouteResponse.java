package com.example.demo.transit.dto;

import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.VehicleType;

import java.math.BigDecimal;
import java.util.List;

public record RouteResponse(
        Long id,
        String name,
        String code,
        VehicleType vehicleType,
        String governorate,
        BigDecimal currentFare,
        RouteStatus status,
        List<Long> stationIds
) {
}
