package com.example.demo.transit.web;

import com.example.demo.transit.dto.StationResponse;
import com.example.demo.transit.service.NetworkAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final NetworkAdminService networkAdminService;

    public StationController(NetworkAdminService networkAdminService) {
        this.networkAdminService = networkAdminService;
    }

    @GetMapping
    public List<StationResponse> list(@RequestParam(required = false) String governorate) {
        return networkAdminService.searchStations(null, governorate);
    }

    /** FR-1.1 text search: ?q=جسر الرئيس */
    @GetMapping("/search")
    public List<StationResponse> search(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String governorate) {
        return networkAdminService.searchStations(q, governorate);
    }

    @GetMapping("/{id}")
    public StationResponse get(@PathVariable String id) {
        return networkAdminService.getStation(id);
    }
}
