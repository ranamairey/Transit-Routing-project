package com.example.demo.transit.geo;

import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.entity.Station;

import java.util.List;

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
        if (speedKmh <= 0 || meters < 1.0) {
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

    /**
     * Cumulative distance (metres) from the first stop to each stop, measured along the drawn
     * polyline when one exists and the stops project onto it in order; otherwise straight-line
     * distances between consecutive stops.
     */
    public static double[] alongPolylineDistances(List<GeoPointDto> polyline, List<Station> stops) {
        double[] prefix = new double[stops.size()];
        if (polyline != null && polyline.size() >= 2) {
            double[] cum = new double[polyline.size()];
            for (int i = 1; i < polyline.size(); i++) {
                GeoPointDto a = polyline.get(i - 1);
                GeoPointDto b = polyline.get(i);
                cum[i] = cum[i - 1] + haversineMeters(a.lat(), a.lng(), b.lat(), b.lng());
            }
            double[] along = new double[stops.size()];
            boolean monotonic = true;
            for (int s = 0; s < stops.size(); s++) {
                along[s] = projectAlong(polyline, cum, stops.get(s));
                if (s > 0 && along[s] < along[s - 1]) {
                    monotonic = false;
                    break;
                }
            }
            if (monotonic) {
                for (int s = 1; s < stops.size(); s++) {
                    prefix[s] = prefix[s - 1] + Math.max(1.0, along[s] - along[s - 1]);
                }
                return prefix;
            }
        }
        for (int i = 1; i < stops.size(); i++) {
            Station a = stops.get(i - 1);
            Station b = stops.get(i);
            prefix[i] = prefix[i - 1] + haversineMeters(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        }
        return prefix;
    }

    /** Distance along the polyline of the point on it closest to the station (planar approximation per segment). */
    private static double projectAlong(List<GeoPointDto> line, double[] cum, Station station) {
        double bestDist = Double.MAX_VALUE;
        double bestAlong = 0;
        double px = station.getLongitude() * Math.cos(Math.toRadians(station.getLatitude()));
        double py = station.getLatitude();
        for (int i = 1; i < line.size(); i++) {
            GeoPointDto a = line.get(i - 1);
            GeoPointDto b = line.get(i);
            double ax = a.lng() * Math.cos(Math.toRadians(a.lat())), ay = a.lat();
            double bx = b.lng() * Math.cos(Math.toRadians(b.lat())), by = b.lat();
            double dx = bx - ax, dy = by - ay;
            double len2 = dx * dx + dy * dy;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
            double cx = ax + t * dx, cy = ay + t * dy;
            double d = (px - cx) * (px - cx) + (py - cy) * (py - cy);
            if (d < bestDist) {
                bestDist = d;
                bestAlong = cum[i - 1] + t * (cum[i] - cum[i - 1]);
            }
        }
        return bestAlong;
    }
}
