package com.example.demo.transit.repository;

import com.example.demo.transit.entity.RouteStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteStationRepository extends JpaRepository<RouteStation, Long> {

    List<RouteStation> findByRouteIdOrderBySequenceNoAsc(Long routeId);
}
