package com.example.demo.transit.geo;

import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.entity.Station;

import java.util.List;
import java.util.stream.Collectors;

public final class GeoMath {

    private GeoMath() {
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earthRadius * Math.asin(Math.sqrt(a));
    }

    public static int travelSeconds(double meters, double speedKmh) {
        if (speedKmh <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.round((meters / 1000.0) / speedKmh * 3600.0));
    }

    public static GeoPointDto toDto(Station station) {
        if (station == null || station.getLatitude() == null || station.getLongitude() == null) {
            return null;
        }
        return new GeoPointDto(station.getLatitude(), station.getLongitude());
    }

    public static String toPolyline(List<GeoPointDto> points) {
        return points.stream()
                .map(p -> p.lat() + "," + p.lng())
                .collect(Collectors.joining(";"));
    }
}
