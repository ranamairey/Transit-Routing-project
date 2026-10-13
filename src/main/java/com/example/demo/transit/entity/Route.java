package com.example.demo.transit.entity;

import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.VehicleType;
import com.example.demo.transit.dto.GeoPointDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("routes")
@Getter
@Setter
@NoArgsConstructor
public class Route {

    @Id
    private String id;

    private String name;

    /** Optional English display name. */
    private String nameEn;

    @Indexed(unique = true, sparse = true)
    private String code;

    private VehicleType vehicleType;

    @Indexed
    private String governorate = "DAMASCUS";

    private BigDecimal currentFare = BigDecimal.ZERO;

    private RouteStatus status = RouteStatus.NORMAL;

    /** When the current status was set (auto-applied statuses expire after app.transit.status-ttl-hours). */
    private Instant statusUpdatedAt;

    /** Drawn path of the line (FR-3.1 polylines). Falls back to the stop coordinates when empty. */
    private List<GeoPointDto> polyline = new ArrayList<>();

    private List<RouteStop> stops = new ArrayList<>();

    public List<String> stationIds() {
        return stops.stream().map(RouteStop::getStationId).toList();
    }
}
