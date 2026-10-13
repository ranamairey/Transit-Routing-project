package com.example.demo.transit.service;

import com.example.demo.transit.config.TransitProperties;
import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.VehicleType;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.RouteStop;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.geo.GeoMath;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import com.example.demo.transit.routing.MultiLegRouter.Edge;
import com.example.demo.transit.routing.MultiLegRouter.SearchGraph;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the in-memory search graph from MongoDB and caches it (NFR-1.3). Mongo ids are strings;
 * the router works on long ids, so each snapshot carries both mappings.
 * Call {@link #invalidate()} after any change to stations, routes or route status.
 */
@Component
public class TransitGraphFactory {

    public static final long ORIGIN_NODE = -1L;
    public static final long DEST_NODE = -2L;

    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final TransitProperties properties;

    private volatile NetworkSnapshot cached;

    public TransitGraphFactory(RouteRepository routeRepository,
                               StationRepository stationRepository,
                               TransitProperties properties) {
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.properties = properties;
    }

    public void invalidate() {
        cached = null;
    }

    /** Cached snapshot of the whole network (all governorates, so rural ↔ city transfers work). */
    public NetworkSnapshot snapshot() {
        NetworkSnapshot snapshot = cached;
        if (snapshot == null) {
            synchronized (this) {
                snapshot = cached;
                if (snapshot == null) {
                    snapshot = build();
                    cached = snapshot;
                }
            }
        }
        return snapshot;
    }

    private NetworkSnapshot build() {
        List<Station> stations = stationRepository.findAll();
        List<Route> routes = routeRepository.findAll();

        Map<String, Station> stationsById = new LinkedHashMap<>();
        stations.forEach(s -> stationsById.put(s.getId(), s));

        Map<String, Long> stationIndex = new HashMap<>();
        Map<Long, Station> stationByIndex = new HashMap<>();
        long next = 1;
        for (Station station : stations) {
            stationIndex.put(station.getId(), next);
            stationByIndex.put(next, station);
            next++;
        }

        Map<String, Long> routeIndex = new HashMap<>();
        Map<Long, Route> routeByIndex = new HashMap<>();
        long routeNext = 1;
        for (Route route : routes) {
            routeIndex.put(route.getId(), routeNext);
            routeByIndex.put(routeNext, route);
            routeNext++;
        }

        SearchGraph graph = new SearchGraph();
        addRideEdges(graph, routes, stationsById, stationIndex, routeIndex);
        addTransferWalks(graph, stations, stationIndex);

        return new NetworkSnapshot(
                graph,
                Collections.unmodifiableList(stations),
                Collections.unmodifiableList(routes),
                Collections.unmodifiableMap(stationsById),
                Collections.unmodifiableMap(stationIndex),
                Collections.unmodifiableMap(stationByIndex),
                Collections.unmodifiableMap(routeIndex),
                Collections.unmodifiableMap(routeByIndex),
                Instant.now()
        );
    }

    /**
     * Connects a free point (GPS / pin) to every station within walking radius, or to the nearest
     * station when none is in range so the search never fails for a slightly distant point.
     */
    public void connectEndpoint(SearchGraph graph, long nodeId, GeoPointDto point, NetworkSnapshot snapshot, boolean fromNode) {
        List<Station> stations = snapshot.stations();
        if (stations.isEmpty()) {
            return;
        }
        boolean connected = false;
        for (Station station : stations) {
            double meters = GeoMath.haversineMeters(point.lat(), point.lng(), station.getLatitude(), station.getLongitude());
            if (meters <= properties.getWalkRadiusMeters()) {
                int seconds = GeoMath.travelSeconds(meters, properties.getWalkSpeedKmh());
                addEdge(graph, nodeId, snapshot.index(station), meters, seconds, fromNode);
                connected = true;
            }
        }
        if (!connected) {
            Station nearest = stations.stream()
                    .min(Comparator.comparingDouble(s -> GeoMath.haversineMeters(
                            point.lat(), point.lng(), s.getLatitude(), s.getLongitude())))
                    .orElseThrow();
            double meters = GeoMath.haversineMeters(point.lat(), point.lng(), nearest.getLatitude(), nearest.getLongitude());
            int seconds = GeoMath.travelSeconds(meters, properties.getWalkSpeedKmh());
            addEdge(graph, nodeId, snapshot.index(nearest), meters, seconds, fromNode);
        }
    }

    /** Time multiplier for a route given its current (non-expired) status. */
    public double statusMultiplier(Route route, Instant now) {
        RouteStatus status = effectiveStatus(route, now);
        return switch (status) {
            case CROWDED -> properties.getCrowdedTimeMultiplier();
            case DIVERTED -> properties.getDivertedTimeMultiplier();
            case DELAYED -> properties.getDelayedTimeMultiplier();
            default -> 1.0;
        };
    }

    /** Auto-applied statuses expire after app.transit.status-ttl-hours. */
    public RouteStatus effectiveStatus(Route route, Instant now) {
        RouteStatus status = route.getStatus() == null ? RouteStatus.NORMAL : route.getStatus();
        if (status == RouteStatus.NORMAL || status == RouteStatus.FARE_UPDATED) {
            return status;
        }
        Instant since = route.getStatusUpdatedAt();
        if (since != null && since.plus(Duration.ofHours(properties.getStatusTtlHours())).isBefore(now)) {
            return RouteStatus.NORMAL;
        }
        return status;
    }

    private void addEdge(SearchGraph graph, long nodeId, long stationIdx, double meters, int seconds, boolean fromNode) {
        if (fromNode) {
            graph.add(nodeId, Edge.walk(stationIdx, meters, seconds));
        } else {
            graph.add(stationIdx, Edge.walk(nodeId, meters, seconds));
        }
    }

    private void addRideEdges(SearchGraph graph, List<Route> routes, Map<String, Station> stationsById,
                              Map<String, Long> stationIndex, Map<String, Long> routeIndex) {
        for (Route route : routes) {
            long rid = routeIndex.get(route.getId());
            graph.setFare(rid, route.getCurrentFare());

            List<Station> stops = new ArrayList<>();
            for (RouteStop stop : route.getStops()) {
                Station s = stationsById.get(stop.getStationId());
                if (s != null) {
                    stops.add(s);
                }
            }
            if (stops.size() < 2) {
                continue;
            }
            double speed = route.getVehicleType() == VehicleType.BUS
                    ? properties.getBusSpeedKmh()
                    : properties.getServisSpeedKmh();

            double[] prefix = new double[stops.size()];
            for (int i = 1; i < stops.size(); i++) {
                Station a = stops.get(i - 1);
                Station b = stops.get(i);
                prefix[i] = prefix[i - 1] + GeoMath.haversineMeters(
                        a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
            }
            for (int i = 0; i < stops.size(); i++) {
                for (int j = i + 1; j < stops.size(); j++) {
                    double meters = prefix[j] - prefix[i];
                    int seconds = GeoMath.travelSeconds(meters, speed);
                    graph.add(stationIndex.get(stops.get(i).getId()),
                            Edge.ride(stationIndex.get(stops.get(j).getId()), rid, meters, seconds));
                }
            }
        }
    }

    private void addTransferWalks(SearchGraph graph, List<Station> stations, Map<String, Long> stationIndex) {
        double maxWalk = Math.max(properties.getWalkRadiusMeters(), 800.0);
        for (int i = 0; i < stations.size(); i++) {
            for (int j = i + 1; j < stations.size(); j++) {
                Station a = stations.get(i);
                Station b = stations.get(j);
                double meters = GeoMath.haversineMeters(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
                if (meters > maxWalk) {
                    continue;
                }
                int seconds = GeoMath.travelSeconds(meters, properties.getWalkSpeedKmh());
                long ai = stationIndex.get(a.getId());
                long bi = stationIndex.get(b.getId());
                graph.add(ai, Edge.walk(bi, meters, seconds));
                graph.add(bi, Edge.walk(ai, meters, seconds));
            }
        }
    }

    public record NetworkSnapshot(
            SearchGraph graph,
            List<Station> stations,
            List<Route> routes,
            Map<String, Station> stationsById,
            Map<String, Long> stationIndex,
            Map<Long, Station> stationByIndex,
            Map<String, Long> routeIndex,
            Map<Long, Route> routeByIndex,
            Instant builtAt
    ) {
        public long index(Station station) {
            return stationIndex.get(station.getId());
        }
    }
}
