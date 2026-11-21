package com.example.demo.transit.entity;

import com.example.demo.transit.domain.SuggestionStatus;
import com.example.demo.transit.domain.SuggestionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A rider's correction to the network data (missing place, wrong stop position, wrong line), reviewed by an admin. */
@Document("suggestions")
@Getter
@Setter
@NoArgsConstructor
public class PlaceSuggestion {
    @Id
    private String id;
    private SuggestionType type;
    @Indexed
    private SuggestionStatus status = SuggestionStatus.PENDING;
    /** Name of the missing place (MISSING_PLACE) or the place being corrected. */
    private String name;
    private String nameEn;
    private String description;
    private Double latitude;
    private Double longitude;
    /** Station the suggestion is about (WRONG_LOCATION), if any. */
    private String stationId;
    /** Line the suggestion is about (WRONG_LINE), if any. */
    private String routeId;
    private String reporterId;
    @Indexed
    private Instant createdAt;
    private String reviewedBy;
    private Instant reviewedAt;
    /** Station created or moved when the suggestion was accepted. */
    private String resultStationId;
}
