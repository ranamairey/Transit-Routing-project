package com.example.demo.transit.entity;

import com.example.demo.transit.domain.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "crowdsource_reports")
@Getter
@Setter
@NoArgsConstructor
public class CrowdsourceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 64)
    private ReportType reportType;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    /** معرّف المبلّغ بعد التوثيق — مطلوب للحد من البلاغات الوهمية */
    @Column(name = "reporter_id", nullable = false, length = 128)
    private String reporterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(name = "new_fare", precision = 12, scale = 2)
    private BigDecimal newFare;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean applied = false;

    private Double latitude;

    private Double longitude;
}
