package com.example.demo.transit.web;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Liveness/readiness for containers and load balancers. */
@RestController
public class HealthController {

    private final MongoTemplate mongoTemplate;

    public HealthController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN", "database", "DOWN"));
        }
    }
}
