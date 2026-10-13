package com.example.demo.transit.entity;

import com.example.demo.transit.domain.ReportType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("crowdsource_reports")
@Getter
@Setter
@NoArgsConstructor
public class CrowdsourceReport {

    @Id
    private String id;

    private ReportType reportType;

    @Indexed
    private Instant sentAt = Instant.now();

    /** Authenticated reporter (email) - required to limit spam (NFR-3.1). */
    private String reporterId;

    @Indexed
    private String routeId;

    private String stationId;

    private BigDecimal newFare;

    private String description;

    /** Applied to the route (automatically after N distinct reporters, or manually by an admin). */
    private boolean applied = false;

    /** Rejected by an admin; ignored by the auto-apply logic and hidden from riders. */
    private boolean dismissed = false;

    private String reviewedBy;

    private Instant reviewedAt;

    private Double latitude;

    private Double longitude;
}
