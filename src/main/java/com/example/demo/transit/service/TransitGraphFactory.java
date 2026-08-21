package com.example.demo.transit.service;

import com.example.demo.transit.config.TransitProperties;
import com.example.demo.transit.domain.VehicleType;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.RouteStation;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.geo.GeoMath;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.routing.MultiLegRouter.Edge;
import com.example.demo.transit.routing.MultiLegRouter.SearchGraph;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TransitGraphFactory {

    public static final long ORIGIN_NODE = -1L;
    public static final long DEST_NODE = -2L;

    private final RouteRepository routeRepository;
    private final TransitProperties properties;

    public TransitGraphFactory(RouteRepository routeRepository, TransitProperties properties) {
        this.routeRepository = routeRepository;
        this.properties = properties;
    }

    public NetworkSnapshot load(String governorate) {
        List<Route> routes = routeRepository.findAllWithStops();

        // 1. التعديل الأول: السماح بالخطوط المتقاطعة بين المحافظات عند طلب التبديل
        // إذا طُلب التبديل بين الشام والريف، يتم تحميل الخطوط التي تحتوي محطات تقاطع متصلة
        if (governorate != null && !governorate.isBlank()) {
            String filter = governorate.trim();
            routes = routes.stream()
                    .filter(r -> r.getGovernorate() == null
                            || r.getGovernorate().equalsIgnoreCase(filter)
                            // السماح بالخطوط التي تتقاطع مع المحافظة المطلوبة (مثل السومرية والبرامكة)
                            || isConnectingRoute(r, filter))
                    .toList();
        }

        List<Station> stations = new ArrayList<>();
        routes.forEach(route -> route.getStops().forEach(stop -> stations.add(stop.getStation())));

        List<Station> uniqueStops = uniqueStations(stations);

        SearchGraph graph = new SearchGraph();
        addRideEdges(graph, routes);
        addTransferWalks(graph, uniqueStops);
        return new NetworkSnapshot(graph, uniqueStops, routes);
    }

    // 2. التعديل الثاني: ربط النقطة بأقرب محطة دائماً حتى لو كانت أبعد قليلاً من Walk Radius
    public void connectEndpoint(SearchGraph graph, long nodeId, GeoPointDto point, List<Station> stations, boolean fromNode) {
        if (stations.isEmpty()) return;

        boolean connected = false;
        for (Station station : stations) {
            double meters = GeoMath.haversineMeters(
                    point.lat(), point.lng(),
                    station.getLatitude(), station.getLongitude()
            );

            // ربط المحطات الواقعة ضمن مجال المشي
            if (meters <= properties.getWalkRadiusMeters()) {
                int seconds = GeoMath.travelSeconds(meters, properties.getWalkSpeedKmh());
                addEdge(graph, nodeId, station.getId(), meters, seconds, fromNode);
                connected = true;
            }
        }

        // إيجاد أقرب محطة صريحة لضمان عدم فشل التوجيه إذا كانت النقطة أبعد بقليل من WalkRadius
        if (!connected) {
            Station nearest = stations.stream()
                    .min(Comparator.comparingDouble(s -> GeoMath.haversineMeters(
                            point.lat(), point.lng(), s.getLatitude(), s.getLongitude())))
                    .orElse(null);

            if (nearest != null) {
                double meters = GeoMath.haversineMeters(
                        point.lat(), point.lng(), nearest.getLatitude(), nearest.getLongitude());
                int seconds = GeoMath.travelSeconds(meters, properties.getWalkSpeedKmh());
                addEdge(graph, nodeId, nearest.getId(), meters, seconds, fromNode);
            }
        }
    }

    private void addEdge(SearchGraph graph, long nodeId, long stationId, double meters, int seconds, boolean fromNode) {
        if (fromNode) {
            graph.add(nodeId, Edge.walk(stationId, meters, seconds));
        } else {
            graph.add(stationId, Edge.walk(nodeId, meters, seconds));
        }
    }

    private boolean isConnectingRoute(Route route, String targetGovernorate) {
        // يسمح بالربط إذا كان الخط يمر بمحطات رئيسية تحوي تبديل مثل (السومرية / البرامكة / جسر الرئيس)
        return route.getStops().stream().anyMatch(stop -> {
            String name = stop.getStation().getName();
            return name.contains("السومرية") || name.contains("البرامكة") || name.contains("جسر الرئيس");
        });
    }

    private void addRideEdges(SearchGraph graph, List<Route> routes) {
        for (Route route : routes) {
            graph.setFare(route.getId(), route.getCurrentFare());
            List<RouteStation> stops = route.getStops();
            if (stops.size() < 2) {
                continue;
            }
            double speed = route.getVehicleType() == VehicleType.BUS
                    ? properties.getBusSpeedKmh()
                    : properties.getServisSpeedKmh();

            double[] prefix = new double[stops.size()];
            for (int i = 1; i < stops.size(); i++) {
                Station a = stops.get(i - 1).getStation();
                Station b = stops.get(i).getStation();
                prefix[i] = prefix[i - 1] + GeoMath.haversineMeters(
                        a.getLatitude(), a.getLongitude(),
                        b.getLatitude(), b.getLongitude()
                );
            }
            for (int i = 0; i < stops.size(); i++) {
                for (int j = i + 1; j < stops.size(); j++) {
                    double meters = prefix[j] - prefix[i];
                    int seconds = GeoMath.travelSeconds(meters, speed);
                    graph.add(
                            stops.get(i).getStation().getId(),
                            Edge.ride(stops.get(j).getStation().getId(), route.getId(), meters, seconds)
                    );
                }
            }
        }
    }

    private void addTransferWalks(SearchGraph graph, List<Station> stations) {
        for (int i = 0; i < stations.size(); i++) {
            for (int j = i + 1; j < stations.size(); j++) {
                Station a = stations.get(i);
                Station b = stations.get(j);
                double meters = GeoMath.haversineMeters(
                        a.getLatitude(), a.getLongitude(),
                        b.getLatitude(), b.getLongitude()
                );
                // رفع مسافة التبديل بين المحطات قليلاً للسماح بالمشي بين مواقف السومرية والبرامكة المتقاربة
                if (meters > Math.max(properties.getWalkRadiusMeters(), 800.0)) {
                    continue;
                }
                int seconds = GeoMath.travelSeconds(meters, properties.getWalkSpeedKmh());
                graph.add(a.getId(), Edge.walk(b.getId(), meters, seconds));
                graph.add(b.getId(), Edge.walk(a.getId(), meters, seconds));
            }
        }
    }

    private List<Station> uniqueStations(List<Station> stations) {
        return stations.stream().distinct().toList();
    }

    public record NetworkSnapshot(SearchGraph graph, List<Station> stations, List<Route> routes) {
    }
}