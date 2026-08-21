package com.example.demo.transit.web;

import com.example.demo.transit.dto.RouteRequest;
import com.example.demo.transit.dto.RouteResponse;
import com.example.demo.transit.dto.StationRequest;
import com.example.demo.transit.dto.StationResponse;
import com.example.demo.transit.service.NetworkAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminNetworkController {

    private final NetworkAdminService networkAdminService;

    public AdminNetworkController(NetworkAdminService networkAdminService) {
        this.networkAdminService = networkAdminService;
    }

    @PostMapping("/stations")
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse createStation(@Valid @RequestBody StationRequest request) {
        return networkAdminService.createStation(request);
    }

    @GetMapping("/stations")
    public List<StationResponse> stations(@RequestParam(required = false) String q) {
        return networkAdminService.searchStations(q);
    }

    @PostMapping("/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse createRoute(@Valid @RequestBody RouteRequest request) {
        return networkAdminService.createRoute(request);
    }

    @GetMapping("/routes")
    public List<RouteResponse> routes() {
        return networkAdminService.listRoutes();
    }
}
