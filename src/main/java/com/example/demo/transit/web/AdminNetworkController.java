package com.example.demo.transit.web;

import com.example.demo.transit.dto.RouteRequest;
import com.example.demo.transit.dto.RouteResponse;
import com.example.demo.transit.dto.RouteStatusRequest;
import com.example.demo.transit.dto.StationRequest;
import com.example.demo.transit.dto.StationResponse;
import com.example.demo.transit.service.NetworkAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** FR-3.1: admin network management. Secured with ROLE_ADMIN in SecurityConfig. */
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
    public List<StationResponse> stations(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) String governorate) {
        return networkAdminService.searchStations(q, governorate);
    }

    @PutMapping("/stations/{id}")
    public StationResponse updateStation(@PathVariable String id, @Valid @RequestBody StationRequest request) {
        return networkAdminService.updateStation(id, request);
    }

    @DeleteMapping("/stations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStation(@PathVariable String id) {
        networkAdminService.deleteStation(id);
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

    @PutMapping("/routes/{id}")
    public RouteResponse updateRoute(@PathVariable String id, @Valid @RequestBody RouteRequest request) {
        return networkAdminService.updateRoute(id, request);
    }

    /** Manually set status (e.g. back to NORMAL) and/or the official fare. */
    @PatchMapping("/routes/{id}/status")
    public RouteResponse updateRouteStatus(@PathVariable String id, @Valid @RequestBody RouteStatusRequest request) {
        return networkAdminService.updateRouteStatus(id, request);
    }

    @DeleteMapping("/routes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable String id) {
        networkAdminService.deleteRoute(id);
    }
}
