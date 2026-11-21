package com.example.demo.transit.repository;

import com.example.demo.transit.entity.SearchLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface SearchLogRepository extends MongoRepository<SearchLog, String> {
    List<SearchLog> findByAtAfter(Instant since);
}
