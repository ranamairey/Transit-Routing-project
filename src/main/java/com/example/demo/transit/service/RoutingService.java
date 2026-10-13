package com.example.demo.transit.service;

import com.example.demo.transit.config.TransitProperties;
import com.example.demo.transit.dto.AlertDto;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.dto.RouteOptionDto;
import com.example.demo.transit.dto.RoutingSearchRequest;
import com.example.demo.transit.dto.RoutingSearchResponse;
import com.example.demo.transit.dto.TripLegDto;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.geo.GeoMath;
import com.example.demo.transit.routing.MultiLegRouter;
import com.example.demo.transit.routing.MultiLegRouter.Edge;
import com.example.demo.transit.routing.MultiLegRouter.FoundPath;
import com.example.demo.transit.routing.MultiLegRouter.SearchGraph;
import com.example.demo.transit.routing.MultiLegRouter.Step;
import com.example.demo.transit.service.TransitGraphFactory.NetworkSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    public static final ZoneId DAMASCUS = ZoneId.of("Asia/Damascus");

    private final TransitGraphFactory graphFactory;
    private final StationSearchService stationSearch;
    private final ReportService reportService;
    private final TransitProperties properties;
    private final MultiLegRouter router = new MultiLegRouter();
    private final Clock clock;
    private final WalkingRouter walkingRouter;

    public RoutingService(TransitGraphFactory graphFactory,
                          StationSearchService stationSearch,
                          ReportService reportService,
                          TransitProperties properties,
                          Clock clock,
                          WalkingRouter walkingRouter) {
        this.walkingRouter = walkingRouter;
        this.graphFactory = graphFactory;
        this.stationSearch = stationSearch;
        this.reportService = reportService;
        this.properties = properties;
        this.clock = clock;
    }

    public RoutingSearchResponse search(RoutingSearchRequest request) {
        long started = System.nanoTime();
        Instant now = clock.instant();

        NetworkSnapshot snapshot = graphFactory.snapshot();
        SearchGraph graph = snapshot.graph().copy();

        Endpoint origin = resolveEndpoint(request.origin(), request.originStationId(), request.originQuery(),
                request.governorate(), "نقطة الانطلاق");
        Endpoint dest = resolveEndpoint(request.destination(), request.destinationStationId(), request.destinationQuery(),
                request.governorate(), "الوجهة");

        long originNode = origin.station() != null ? snapshot.index(origin.station()) : TransitGraphFactory.ORIGIN_NODE;
        long destNode = dest.station() != null ? snapshot.index(dest.station()) : TransitGraphFactory.DEST_NODE;

        if (origin.station() == null) {
            graphFactory.connectEndpoint(graph, originNode, origin.point(), snapshot, true);
        }
        if (dest.station() == null) {
            graphFactory.connectEndpoint(graph, destNode, dest.point(), snapshot, false);
        }

        // Direct walk when the two points are within walking distance of each other.
        double directStraight = GeoMath.haversineMeters(origin.point().lat(), origin.point().lng(), dest.point().lat(), dest.point().lng());
        if (originNode != destNode && directStraight <= properties.getWalkRadiusMeters()) {
            double direct = graphFactory.walkingMeters(origin.point(), dest.point());
            graph.add(originNode, Edge.walk(destNode, direct, GeoMath.travelSeconds(direct, properties.getWalkSpeedKmh())));
        }

        // FR-1.3 / FR-2.1: peak hours and live crowding reports slow down affected routes.
        boolean peak = request.peak() != null
                ? request.peak()
                : properties.isPeak(now.atZone(DAMASCUS).toLocalTime());
        double peakMultiplier = peak ? properties.getPeakTimeMultiplier() : 1.0;
        graph.scaleRideTimes(routeIdx -> {
            Route route = snapshot.routeByIndex().get(routeIdx);
            return peakMultiplier * (route == null ? 1.0 : graphFactory.statusMultiplier(route, now));
        });

        if (originNode == destNode) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "نقطة الانطلاق والوجهة متطابقتان");
        }

        List<FoundPath> paths = router.findPaths(graph, originNode, destNode,
                properties.getMaxRideLegs(), properties.getAlternativePaths());

        Map<Long, Station> stationByIndex = new java.util.HashMap<>(snapshot.stationByIndex());
        stationByIndex.put(TransitGraphFactory.ORIGIN_NODE, virtualStation("نقطة الانطلاق", origin.point()));
        stationByIndex.put(TransitGraphFactory.DEST_NODE, virtualStation("الوجهة", dest.point()));

        List<RouteOptionDto> options = paths.stream()
                .map(path -> toOption(path, stationByIndex, snapshot.routeByIndex(), now))
                .toList();
        if (options.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "لا يوجد مسار تبديلي ضمن نطاق المشي المحدد");
        }

        List<String> routeIds = options.stream()
                .flatMap(o -> o.legs().stream())
                .map(TripLegDto::routeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<AlertDto> alerts = reportService.alertsForRoutes(routeIds, snapshot.routes());

        long millis = (System.nanoTime() - started) / 1_000_000;
        return new RoutingSearchResponse(
                origin.point(), dest.point(), origin.name(), dest.name(), peak, options, alerts, millis);
    }

    private Endpoint resolveEndpoint(GeoPointDto pin, String stationId, String query, String governorate, String fallbackName) {
        NetworkSnapshot snapshot = graphFactory.snapshot();
        if (stationId != null && !stationId.isBlank()) {
            Station station = snapshot.stationsById().get(stationId);
            if (station == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة");
            }
            return new Endpoint(GeoMath.toDto(station), station, station.getName());
        }
        if (query != null && !query.isBlank()) {
            Station station = stationSearch.best(query, governorate);
            if (station == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "لم يُعثر على الموقع: " + query);
            }
            // use the cached instance so the graph index lookup works
            Station cached = snapshot.stationsById().get(station.getId());
            return new Endpoint(GeoMath.toDto(cached), cached, cached.getName());
        }
        if (pin != null && pin.lat() != null && pin.lng() != null) {
            return new Endpoint(pin, null, fallbackName);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "حدد نقطة البداية/الوجهة عبر GPS أو دبوس أو بحث نصي");
    }

    private RouteOptionDto toOption(FoundPath path, Map<Long, Station> stations, Map<Long, Route> routes, Instant now) {
        List<TripLegDto> legs = new ArrayList<>();
        for (Step step : mergeSteps(path.steps())) {
            boolean walk = step.routeId() == MultiLegRouter.WALK_ROUTE_ID;
            if (walk && step.distanceMeters() < 1.0) {
                continue; // zero-length walk (endpoint is the station itself)
            }
            Station from = stations.get(step.fromStationId());
            Station to = stations.get(step.toStationId());
            Route route = walk ? null : routes.get(step.routeId());
            legs.add(new TripLegDto(
                    walk ? "WALK" : "RIDE",
                    from != null ? from.getName() : null,
                    to != null ? to.getName() : null,
                    GeoMath.toDto(from),
                    GeoMath.toDto(to),
                    route != null ? route.getId() : null,
                    route != null ? route.getName() : null,
                    route != null ? route.getVehicleType() : null,
                    route != null ? graphFactory.effectiveStatus(route, now) : null,
                    route != null ? route.getCurrentFare() : null,
                    step.timeSeconds(),
                    step.distanceMeters(),
                    walk ? walkPath(from, to) : null
            ));
        }
        int transfers = Math.max(0, path.rideLegs() - 1);
        return new RouteOptionDto(path.totalFare(), path.totalDurationSeconds(), path.walkingMeters(), transfers, legs);
    }

    private List<Step> mergeSteps(List<Step> steps) {
        List<Step> merged = new ArrayList<>();
        for (Step step : steps) {
            if (merged.isEmpty()) {
                merged.add(step);
                continue;
            }
            Step last = merged.get(merged.size() - 1);
            if (last.routeId() == step.routeId() && last.routeId() != MultiLegRouter.WALK_ROUTE_ID) {
                merged.set(merged.size() - 1, new Step(
                        last.fromStationId(), step.toStationId(), last.routeId(),
                        last.distanceMeters() + step.distanceMeters(),
                        last.timeSeconds() + step.timeSeconds()));
            } else {
                merged.add(step);
            }
        }
        return merged;
    }

    /** Street geometry of a walking leg from OSRM (null when unavailable; the client draws a straight line). */
    private List<GeoPointDto> walkPath(Station from, Station to) {
        if (from == null || to == null || !walkingRouter.enabled()) {
            return null;
        }
        return walkingRouter.route(GeoMath.toDto(from), GeoMath.toDto(to), true)
                .map(WalkingRouter.Walk::path).filter(p -> p.size() >= 2).orElse(null);
    }

    private Station virtualStation(String name, GeoPointDto point) {
        Station station = new Station();
        station.setName(name);
        station.setLatitude(point.lat());
        station.setLongitude(point.lng());
        return station;
    }

    private record Endpoint(GeoPointDto point, Station station, String name) {
    }
}
