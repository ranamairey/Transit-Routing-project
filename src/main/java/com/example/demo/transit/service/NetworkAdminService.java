package com.example.demo.transit.service;

import com.example.demo.transit.domain.StationKind;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.dto.RouteRequest;
import com.example.demo.transit.dto.RouteResponse;
import com.example.demo.transit.dto.StationRequest;
import com.example.demo.transit.dto.StationResponse;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.RouteStation;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.geo.GeoMath;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class NetworkAdminService {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;

    public NetworkAdminService(StationRepository stationRepository, RouteRepository routeRepository) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
    }

    @Transactional
    public StationResponse createStation(StationRequest request) {
        Station station = new Station();
        station.setName(request.name());
        station.setAliases(request.aliases());
        station.setStationKind(request.stationKind() != null ? request.stationKind() : StationKind.STOP);
        station.setGovernorate(request.governorate() != null ? request.governorate() : "DAMASCUS");
        station.setLatitude(request.location().lat());
        station.setLongitude(request.location().lng());
        return toStationResponse(stationRepository.save(station));
    }

    @Transactional(readOnly = true)
    public List<StationResponse> searchStations(String query) {
        List<Station> stations = (query == null || query.isBlank())
                ? stationRepository.findAll()
                : stationRepository.searchByNameOrAlias(query.trim());
        return stations.stream().map(this::toStationResponse).toList();
    }

    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        Route route = new Route();
        route.setName(request.name());
        route.setCode(request.code());
        route.setVehicleType(request.vehicleType());
        route.setGovernorate(request.governorate() != null ? request.governorate() : "DAMASCUS");
        route.setCurrentFare(request.currentFare());

        if (request.stationIds().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "الخط يحتاج محطتين على الأقل");
        }

        List<Station> stations = new ArrayList<>();
        int sequence = 1;
        for (Long stationId : request.stationIds()) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "محطة غير موجودة: " + stationId));
            stations.add(station);
            RouteStation stop = new RouteStation();
            stop.setRoute(route);
            stop.setStation(station);
            stop.setSequenceNo(sequence++);
            route.getStops().add(stop);
        }

        if (request.polyline() != null && request.polyline().size() >= 2) {
            route.setPathPolyline(GeoMath.toPolyline(request.polyline()));
        } else {
            route.setPathPolyline(GeoMath.toPolyline(
                    stations.stream().map(s -> new GeoPointDto(s.getLatitude(), s.getLongitude())).toList()
            ));
        }
        return toRouteResponse(routeRepository.save(route));
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> listRoutes() {
        return routeRepository.findAllWithStops().stream().map(this::toRouteResponse).toList();
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

    private RouteResponse toRouteResponse(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getName(),
                route.getCode(),
                route.getVehicleType(),
                route.getGovernorate(),
                route.getCurrentFare(),
                route.getStatus(),
                route.getStops().stream().map(s -> s.getStation().getId()).toList()
        );
    }
}
