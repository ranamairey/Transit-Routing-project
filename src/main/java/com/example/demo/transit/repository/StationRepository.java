package com.example.demo.transit.repository;

import com.example.demo.transit.entity.Station;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StationRepository extends MongoRepository<Station, String> {

    List<Station> findByGovernorateIgnoreCase(String governorate);
}
