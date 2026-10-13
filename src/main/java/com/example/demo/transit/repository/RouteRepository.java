package com.example.demo.transit.repository;

import com.example.demo.transit.entity.Route;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends MongoRepository<Route, String> {

    Optional<Route> findByCode(String code);

    List<Route> findByStopsStationId(String stationId);
}
