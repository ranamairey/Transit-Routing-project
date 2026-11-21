package com.example.demo.transit.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One trip search, kept for 90 days for the admin usage statistics. No user identity is stored and
 * map points are rounded to about 100 m.
 */
@Document("search_logs")
@Getter
@Setter
@NoArgsConstructor
public class SearchLog {
    @Id
    private String id;
    @Indexed(expireAfterSeconds = 90 * 24 * 3600)
    private Instant at;
    private String originName;
    private String destinationName;
    private String originStationId;
    private String destinationStationId;
    private Double originLat;
    private Double originLng;
    private Double destinationLat;
    private Double destinationLng;
    private int resultCount;
}
