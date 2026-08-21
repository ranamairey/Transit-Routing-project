package com.example.demo.transit.dto;

import java.math.BigDecimal;
import java.util.List;

public record RouteOptionDto(
        BigDecimal totalFare,
        int totalDurationSeconds,
        double walkingMeters,
        List<TripLegDto> legs
) {
}
