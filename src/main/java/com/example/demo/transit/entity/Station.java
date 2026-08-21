package com.example.demo.transit.entity;

import com.example.demo.transit.domain.StationKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** أسماء محلية بديلة للبحث النصي: جسر الرئيس، البرامكة، مشروع دمر */
    @Column(length = 1000)
    private String aliases;

    @Enumerated(EnumType.STRING)
    @Column(name = "station_kind", nullable = false, length = 32)
    private StationKind stationKind = StationKind.STOP;

    /** تقسيم جغرافي للتوسع بين المحافظات دون تغيير الكود */
    @Column(nullable = false, length = 64)
    private String governorate = "DAMASCUS";

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
}
