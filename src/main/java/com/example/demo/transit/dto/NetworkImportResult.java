package com.example.demo.transit.dto;

import java.util.List;

public record NetworkImportResult(
        int stationsCreated, int stationsUpdated,
        int routesCreated, int routesUpdated,
        List<String> warnings
) {
}
