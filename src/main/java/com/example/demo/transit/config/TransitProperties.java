package com.example.demo.transit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.transit")
public class TransitProperties {

    private int maxRideLegs = 4;
    /** When no route exists within maxRideLegs, retry once with up to this many rides. */
    private int maxRideLegsFallback = 5;
    private int alternativePaths = 3;
    private double walkRadiusMeters = 750;
    /** Total walking allowed in one trip (first/last mile plus transfers). */
    private double maxWalkMeters = 1500;
    private double walkSpeedKmh = 5;
    /** Straight-line walking distances are multiplied by this when no street routing is available. */
    private double walkDetourFactor = 1.25;
    /** Use OSRM for station-to-station transfer walks too (only with a self-hosted OSRM: many requests at startup). */
    private boolean osrmTransfers = false;
    /** Maximum number of trip options returned. */
    private int maxOptions = 5;
    private double servisSpeedKmh = 22;
    private double busSpeedKmh = 28;
    private double peakTimeMultiplier = 1.4;
    private double crowdedTimeMultiplier = 1.5;
    private double divertedTimeMultiplier = 1.3;
    private double delayedTimeMultiplier = 1.25;
    /** Peak windows as "HH:mm-HH:mm" in Asia/Damascus local time. */
    private List<String> peakWindows = new ArrayList<>(List.of("07:00-09:30", "13:00-16:00"));
    private int alertWindowHours = 3;
    private int autoApplyThreshold = 3;
    private int statusTtlHours = 6;

    public boolean isPeak(LocalTime time) {
        for (String window : peakWindows) {
            String[] parts = window.split("-");
            if (parts.length != 2) {
                continue;
            }
            LocalTime from = LocalTime.parse(parts[0].trim());
            LocalTime to = LocalTime.parse(parts[1].trim());
            if (!time.isBefore(from) && time.isBefore(to)) {
                return true;
            }
        }
        return false;
    }
}
