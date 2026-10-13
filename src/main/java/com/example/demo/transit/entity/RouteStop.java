package com.example.demo.transit.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ordered stop of a route, embedded in the route document. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {
    private String stationId;
    private int sequenceNo;
}
