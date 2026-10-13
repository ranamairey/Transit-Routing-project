package com.example.demo.transit.dto;

import com.example.demo.transit.domain.RouteStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/** Admin: manually set a route's status and/or fare. */
public record RouteStatusRequest(
        RouteStatus status,
        @DecimalMin("0") BigDecimal currentFare
) {
}
