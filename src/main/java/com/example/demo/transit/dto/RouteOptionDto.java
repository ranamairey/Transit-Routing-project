package com.example.demo.transit.dto;

import java.math.BigDecimal;
import java.util.List;

public record RouteOptionDto(
        BigDecimal totalFare,
        int totalDurationSeconds,
        double walkingMeters,
        int transfers,
        List<TripLegDto> legs,
        /** Why this option is offered: FASTEST, FEWEST_TRANSFERS, CHEAPEST, LEAST_WALKING (may be empty). */
        List<String> labels
) {
}
