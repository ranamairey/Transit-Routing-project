package com.example.demo.transit.repository;

import com.example.demo.transit.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    @Query("""
            SELECT DISTINCT r FROM Route r
            LEFT JOIN FETCH r.stops rs
            LEFT JOIN FETCH rs.station
            """)
    List<Route> findAllWithStops();
}
