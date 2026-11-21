package com.example.demo.transit.repository;

import com.example.demo.transit.domain.SuggestionStatus;
import com.example.demo.transit.entity.PlaceSuggestion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PlaceSuggestionRepository extends MongoRepository<PlaceSuggestion, String> {
    List<PlaceSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    List<PlaceSuggestion> findAllByOrderByCreatedAtDesc();

    long countByStatus(SuggestionStatus status);
}
