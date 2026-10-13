package com.example.demo.transit.entity;

import com.example.demo.transit.domain.StationKind;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("stations")
@Getter
@Setter
@NoArgsConstructor
public class Station {

    @Id
    private String id;

    private String name;

    /** Optional English display name. */
    private String nameEn;

    /** Local alternative names for text search: جسر الرئيس، البرامكة، مشروع دمر */
    private String aliases;

    private StationKind stationKind = StationKind.STOP;

    /** Geographic partition (NFR-4.1): allows adding other governorates without code changes. */
    @Indexed
    private String governorate = "DAMASCUS";

    private Double latitude;

    private Double longitude;
}
