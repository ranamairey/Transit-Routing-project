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
import com.example.demo.transit.repository.CrowdsourceReportRepository;
import com.example.demo.transit.repository.StationRepository;
import com.example.demo.transit.routing.MultiLegRouter;
import com.example.demo.transit.routing.MultiLegRouter.FoundPath;
import com.example.demo.transit.routing.MultiLegRouter.SearchGraph;
import com.example.demo.transit.routing.MultiLegRouter.Step;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    private final TransitGraphFactory graphFactory;
    private final StationRepository stationRepository;
    private final CrowdsourceReportRepository reportRepository;
    private final TransitProperties properties;
    private final MultiLegRouter router = new MultiLegRouter();

    public RoutingService(
            TransitGraphFactory graphFactory,
            StationRepository stationRepository,
            CrowdsourceReportRepository reportRepository,
            TransitProperties properties
    ) {
        this.graphFactory = graphFactory;
        this.stationRepository = stationRepository;
        this.reportRepository = reportRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public RoutingSearchResponse search(RoutingSearchRequest request) {
        long started = System.nanoTime();
        // تحميل كامل الشبكة أو دمج المحافظات لضمان التوصيل بين الريف والمدينة
        TransitGraphFactory.NetworkSnapshot snapshot = graphFactory.load(null);
        SearchGraph graph = snapshot.graph();

        Endpoint origin = resolveEndpoint(request.origin(), request.originStationId(), request.originQuery());
        Endpoint dest = resolveEndpoint(request.destination(), request.destinationStationId(), request.destinationQuery());

        graphFactory.connectEndpoint(graph, TransitGraphFactory.ORIGIN_NODE, origin.point(), snapshot.stations(), true);
        graphFactory.connectEndpoint(graph, TransitGraphFactory.DEST_NODE, dest.point(), snapshot.stations(), false);
        graph.add(TransitGraphFactory.ORIGIN_NODE, MultiLegRouter.Edge.walk(
                TransitGraphFactory.DEST_NODE,
                GeoMath.haversineMeters(
                        origin.point().lat(), origin.point().lng(),
                        dest.point().lat(), dest.point().lng()),
                Integer.MAX_VALUE / 4
        ));

        if (request.peak()) {
            graph.scaleRideTimes(properties.getPeakTimeMultiplier());
        }

        List<FoundPath> paths = router.findPaths(
                graph,
                TransitGraphFactory.ORIGIN_NODE,
                TransitGraphFactory.DEST_NODE,
                properties.getMaxRideLegs(),
                properties.getAlternativePaths()
        );

        Map<Long, Station> stationsById = snapshot.stations().stream()
                .collect(Collectors.toMap(Station::getId, s -> s, (a, b) -> a));
        stationsById.put(TransitGraphFactory.ORIGIN_NODE, virtualStation("نقطة الانطلاق", origin.point()));
        stationsById.put(TransitGraphFactory.DEST_NODE, virtualStation("الوجهة", dest.point()));

        Map<Long, Route> routesById = snapshot.routes().stream()
                .collect(Collectors.toMap(Route::getId, r -> r, (a, b) -> a));

        List<RouteOptionDto> options = paths.stream()
                .filter(p -> p.totalDurationSeconds() < Integer.MAX_VALUE / 8)
                .map(path -> toOption(path, stationsById, routesById))
                .toList();
        if (options.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "لا يوجد مسار تبديلي ضمن نطاق المشي المحدد"
            );
        }

        List<Long> routeIds = options.stream()
                .flatMap(o -> o.legs().stream())
                .map(TripLegDto::routeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Instant since = Instant.now().minus(Duration.ofHours(properties.getAlertWindowHours()));
        List<AlertDto> alerts = routeIds.isEmpty()
                ? List.of()
                : reportRepository.findByRoute_IdInAndSentAtAfterOrderBySentAtDesc(routeIds, since).stream()
                .limit(20)
                .map(r -> new AlertDto(
                        r.getRoute() != null ? r.getRoute().getId() : null,
                        r.getRoute() != null ? r.getRoute().getName() : null,
                        r.getReportType(),
                        r.getDescription(),
                        r.getSentAt()
                ))
                .toList();

        long millis = (System.nanoTime() - started) / 1_000_000;
        return new RoutingSearchResponse(options, alerts, millis);
    }

    private Endpoint resolveEndpoint(GeoPointDto pin, Long stationId, String query) {
        if (stationId != null) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة"));
            return new Endpoint(GeoMath.toDto(station), station);
        }
        if (query != null && !query.isBlank()) {
            List<Station> matches = stationRepository.searchByNameOrAlias(query.trim());
            if (matches.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "لم يُعثر على الموقع: " + query);
            }
            Station station = matches.get(0);
            return new Endpoint(GeoMath.toDto(station), station);
        }
        if (pin != null && pin.lat() != null && pin.lng() != null) {
            return new Endpoint(pin, null);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "حدد نقطة البداية/الوجهة عبر GPS أو دبوس أو بحث نصي");
    }

    private RouteOptionDto toOption(FoundPath path, Map<Long, Station> stations, Map<Long, Route> routes) {
        List<TripLegDto> legs = new ArrayList<>();
        List<Step> merged = mergeSteps(path.steps());
        for (Step step : merged) {
            Station from = stations.get(step.fromStationId());
            Station to = stations.get(step.toStationId());
            boolean walk = step.routeId() == MultiLegRouter.WALK_ROUTE_ID;
            Route route = walk ? null : routes.get(step.routeId());
            legs.add(new TripLegDto(
                    walk ? "WALK" : "RIDE",
                    from != null ? from.getName() : null,
                    to != null ? to.getName() : null,
                    GeoMath.toDto(from),
                    GeoMath.toDto(to),
                    walk ? null : step.routeId(),
                    route != null ? route.getName() : null,
                    route != null ? route.getVehicleType() : null,
                    walk || route == null ? null : route.getCurrentFare(),
                    step.timeSeconds(),
                    step.distanceMeters()
            ));
        }
        return new RouteOptionDto(path.totalFare(), path.totalDurationSeconds(), path.walkingMeters(), legs);
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
                        last.fromStationId(),
                        step.toStationId(),
                        last.routeId(),
                        last.distanceMeters() + step.distanceMeters(),
                        last.timeSeconds() + step.timeSeconds()
                ));
            } else {
                merged.add(step);
            }
        }
        return merged;
    }

    private Station virtualStation(String name, GeoPointDto point) {
        Station station = new Station();
        station.setName(name);
        station.setLatitude(point.lat());
        station.setLongitude(point.lng());
        return station;
    }

    private record Endpoint(GeoPointDto point, Station station) {
    }
}
