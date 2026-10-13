package com.example.demo.transit.service;

import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.StationKind;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.dto.NetworkImportRequest;
import com.example.demo.transit.dto.NetworkImportResult;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.RouteStop;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bulk upsert of stations and routes (FR-3.1): lets admins load a whole governorate at once. */
@Service
public class NetworkImportService {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final TransitGraphFactory graphFactory;

    public NetworkImportService(StationRepository stationRepository, RouteRepository routeRepository, TransitGraphFactory graphFactory) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.graphFactory = graphFactory;
    }

    public NetworkImportResult importNetwork(NetworkImportRequest request) {
        List<String> warnings = new ArrayList<>();
        int sc = 0, su = 0, rc = 0, ru = 0;

        Map<String, Station> byKey = new HashMap<>();
        stationRepository.findAll().forEach(s -> byKey.put(key(s.getName(), s.getGovernorate()), s));

        for (NetworkImportRequest.StationItem item : request.stations() == null ? List.<NetworkImportRequest.StationItem>of() : request.stations()) {
            String gov = gov(item.governorate());
            Station station = byKey.get(key(item.name(), gov));
            boolean created = station == null;
            if (created) {
                station = new Station();
                station.setName(item.name().trim());
                station.setGovernorate(gov);
            }
            if (item.nameEn() != null) station.setNameEn(item.nameEn().trim());
            if (item.aliases() != null) station.setAliases(item.aliases());
            station.setStationKind(item.stationKind() != null ? item.stationKind() : StationKind.STOP);
            station.setLatitude(item.lat());
            station.setLongitude(item.lng());
            station = stationRepository.save(station);
            byKey.put(key(station.getName(), station.getGovernorate()), station);
            if (created) sc++; else su++;
        }

        Map<String, Station> byName = new HashMap<>();
        byKey.values().forEach(s -> byName.putIfAbsent(s.getName().trim(), s));

        Map<String, Route> byCode = new HashMap<>();
        Map<String, Route> byRouteName = new HashMap<>();
        routeRepository.findAll().forEach(r -> {
            if (r.getCode() != null) byCode.put(r.getCode(), r);
            byRouteName.put(r.getName(), r);
        });

        for (NetworkImportRequest.RouteItem item : request.routes() == null ? List.<NetworkImportRequest.RouteItem>of() : request.routes()) {
            List<Station> stops = new ArrayList<>();
            boolean ok = true;
            for (String name : item.stations()) {
                Station s = byName.get(name.trim());
                if (s == null) {
                    warnings.add("الخط «" + item.name() + "»: محطة غير معروفة «" + name + "»");
                    ok = false;
                    break;
                }
                stops.add(s);
            }
            if (!ok || stops.size() < 2) {
                if (ok) warnings.add("الخط «" + item.name() + "» يحتاج محطتين على الأقل");
                continue;
            }
            Route route = item.code() != null && !item.code().isBlank() ? byCode.get(item.code().trim()) : null;
            if (route == null) route = byRouteName.get(item.name().trim());
            boolean created = route == null;
            if (created) {
                route = new Route();
                route.setStatus(RouteStatus.NORMAL);
            }
            route.setName(item.name().trim());
            if (item.nameEn() != null) route.setNameEn(item.nameEn().trim());
            route.setCode(item.code() != null && !item.code().isBlank() ? item.code().trim() : route.getCode());
            route.setVehicleType(item.vehicleType());
            route.setGovernorate(gov(item.governorate()));
            route.setCurrentFare(item.currentFare());
            List<RouteStop> routeStops = new ArrayList<>();
            List<GeoPointDto> fallback = new ArrayList<>();
            for (int i = 0; i < stops.size(); i++) {
                routeStops.add(new RouteStop(stops.get(i).getId(), i + 1));
                fallback.add(new GeoPointDto(stops.get(i).getLatitude(), stops.get(i).getLongitude()));
            }
            route.setStops(routeStops);
            route.setPolyline(item.polyline() != null && item.polyline().size() >= 2 ? new ArrayList<>(item.polyline()) : fallback);
            route = routeRepository.save(route);
            if (route.getCode() != null) byCode.put(route.getCode(), route);
            byRouteName.put(route.getName(), route);
            if (created) rc++; else ru++;
        }

        graphFactory.invalidate();
        return new NetworkImportResult(sc, su, rc, ru, warnings);
    }

    private static String gov(String g) {
        return g == null || g.isBlank() ? "DAMASCUS" : g.trim().toUpperCase(Locale.ROOT);
    }

    private static String key(String name, String gov) {
        return name.trim() + "@" + gov;
    }
}
