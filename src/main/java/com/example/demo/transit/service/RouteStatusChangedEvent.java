package com.example.demo.transit.service;

import com.example.demo.transit.domain.RouteStatus;

import java.math.BigDecimal;

/** Published when a line's status (or fare) changes, so followers can be alerted. */
public record RouteStatusChangedEvent(String routeId, String routeName, RouteStatus status, BigDecimal fare) {
}
