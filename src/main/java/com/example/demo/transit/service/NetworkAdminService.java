package com.example.demo.transit.service;

import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.StationKind;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.dto.NetworkSnapshotDto;
import com.example.demo.transit.dto.RouteRequest;
import com.example.demo.transit.dto.RouteResponse;
import com.example.demo.transit.dto.RouteStatusRequest;
import com.example.demo.transit.dto.StationRequest;
import com.example.demo.transit.dto.StationResponse;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.RouteStop;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.geo.GeoMath;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** FR-3.1: network management (stations, routes, polylines) + public read models. */
@Service
public class NetworkAdminService {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final StationSearchService stationSearch;
    private final TransitGraphFactory graphFactory;
    private final Clock clock;

    public NetworkAdminService(StationRepository stationRepository,
                               RouteRepository routeRepository,
                               StationSearchService stationSearch,
                               TransitGraphFactory graphFactory,
                               Clock clock) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.stationSearch = stationSearch;
        this.graphFactory = graphFactory;
        this.clock = clock;
    }

    // ---------- stations ----------

    public StationResponse createStation(StationRequest request) {
        Station station = apply(new Station(), request);
        Station saved = stationRepository.save(station);
        graphFactory.invalidate();
        return toStationResponse(saved);
    }

    public StationResponse updateStation(String id, StationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة"));
        Station saved = stationRepository.save(apply(station, request));
        graphFactory.invalidate();
        return toStationResponse(saved);
    }

    public void deleteStation(String id) {
        if (!stationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة");
        }
        List<Route> using = routeRepository.findByStopsStationId(id);
        if (!using.isEmpty()) {
            String names = using.stream().map(Route::getName).collect(Collectors.joining("، "));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "لا يمكن حذف المحطة لأنها مستخدمة في الخطوط: " + names);
        }
        stationRepository.deleteById(id);
        graphFactory.invalidate();
    }

    public StationResponse getStation(String id) {
        return stationRepository.findById(id).map(this::toStationResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة"));
    }

    public List<StationResponse> searchStations(String query, String governorate) {
        return stationSearch.search(query, governorate).stream().map(this::toStationResponse).toList();
    }

    private Station apply(Station station, StationRequest request) {
        station.setName(request.name().trim());
        station.setAliases(request.aliases());
        station.setStationKind(request.stationKind() != null ? request.stationKind() : StationKind.STOP);
        station.setGovernorate(request.governorate() != null && !request.governorate().isBlank()
                ? request.governorate().trim().toUpperCase() : "DAMASCUS");
        station.setLatitude(request.location().lat());
        station.setLongitude(request.location().lng());
        return station;
    }

    // ---------- routes ----------

    public RouteResponse createRoute(RouteRequest request) {
        Route route = apply(new Route(), request);
        Route saved = routeRepository.save(route);
        graphFactory.invalidate();
        return toRouteResponse(saved);
    }

    public RouteResponse updateRoute(String id, RouteRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود"));
        Route saved = routeRepository.save(apply(route, request));
        graphFactory.invalidate();
        return toRouteResponse(saved);
    }

    public RouteResponse updateRouteStatus(String id, RouteStatusRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود"));
        if (request.status() != null) {
            route.setStatus(request.status());
            route.setStatusUpdatedAt(request.status() == RouteStatus.NORMAL ? null : clock.instant());
        }
        if (request.currentFare() != null) {
            route.setCurrentFare(request.currentFare());
        }
        Route saved = routeRepository.save(route);
        graphFactory.invalidate();
        return toRouteResponse(saved);
    }

    public void deleteRoute(String id) {
        if (!routeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود");
        }
        routeRepository.deleteById(id);
        graphFactory.invalidate();
    }

    public RouteResponse getRoute(String id) {
        return routeRepository.findById(id).map(this::toRouteResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود"));
    }

    public List<RouteResponse> listRoutes() {
        return routeRepository.findAll().stream().map(this::toRouteResponse).toList();
    }

    private Route apply(Route route, RouteRequest request) {
        route.setName(request.name().trim());
        route.setCode(request.code() != null && !request.code().isBlank() ? request.code().trim() : null);
        route.setVehicleType(request.vehicleType());
        route.setGovernorate(request.governorate() != null && !request.governorate().isBlank()
                ? request.governorate().trim().toUpperCase() : "DAMASCUS");
        route.setCurrentFare(request.currentFare());
        if (route.getStatus() == null) {
            route.setStatus(RouteStatus.NORMAL);
        }

        List<String> ids = new ArrayList<>(new LinkedHashSet<>(request.stationIds()));
        if (ids.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "الخط يحتاج محطتين مختلفتين على الأقل");
        }
        List<Station> stations = new ArrayList<>();
        List<RouteStop> stops = new ArrayList<>();
        int sequence = 1;
        for (String stationId : ids) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "محطة غير موجودة: " + stationId));
            stations.add(station);
            stops.add(new RouteStop(station.getId(), sequence++));
        }
        route.setStops(stops);

        if (request.polyline() != null && request.polyline().size() >= 2) {
            route.setPolyline(new ArrayList<>(request.polyline()));
        } else {
            route.setPolyline(stations.stream().map(GeoMath::toDto).toList());
        }
        return route;
    }

    // ---------- public read models ----------

    /** NFR-1.2: whole network with a content hash the client can cache and revalidate with ETag. */
    public NetworkSnapshotDto networkSnapshot() {
        List<StationResponse> stations = stationRepository.findAll().stream().map(this::toStationResponse).toList();
        List<RouteResponse> routes = listRoutes();
        String fingerprint = stations.stream().map(Objects::toString).collect(Collectors.joining("|"))
                + "#" + routes.stream().map(Objects::toString).collect(Collectors.joining("|"));
        return new NetworkSnapshotDto(sha1(fingerprint), stations, routes);
    }

    public StationResponse toStationResponse(Station station) {
        return new StationResponse(
                station.getId(),
                station.getName(),
                station.getAliases(),
                station.getStationKind(),
                station.getGovernorate(),
                GeoMath.toDto(station)
        );
    }

    public RouteResponse toRouteResponse(Route route) {
        List<String> ids = route.stationIds();
        Map<String, Station> stations = stationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Station::getId, Function.identity(), (a, b) -> a));
        List<StationResponse> stops = ids.stream()
                .map(stations::get)
                .filter(Objects::nonNull)
                .map(this::toStationResponse)
                .toList();
        List<GeoPointDto> polyline = route.getPolyline() == null || route.getPolyline().isEmpty()
                ? stops.stream().map(StationResponse::location).toList()
                : route.getPolyline();
        return new RouteResponse(
                route.getId(),
                route.getName(),
                route.getCode(),
                route.getVehicleType(),
                route.getGovernorate(),
                route.getCurrentFare(),
                graphFactory.effectiveStatus(route, clock.instant()),
                route.getStatusUpdatedAt(),
                ids,
                stops,
                polyline
        );
    }

    private static String sha1(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 10);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
