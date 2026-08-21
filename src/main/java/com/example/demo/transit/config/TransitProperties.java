package com.example.demo.transit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.transit")
public class TransitProperties {

    private int maxRideLegs = 3;
    private int alternativePaths = 3;
    private double walkRadiusMeters = 750;
    private double walkSpeedKmh = 5;
    private double servisSpeedKmh = 22;
    private double busSpeedKmh = 28;
    private double peakTimeMultiplier = 1.4;
    private int alertWindowHours = 3;
    private int autoApplyThreshold = 3;
}
