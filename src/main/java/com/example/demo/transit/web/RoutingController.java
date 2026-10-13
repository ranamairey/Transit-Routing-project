package com.example.demo.transit.web;

import com.example.demo.transit.dto.RoutingSearchRequest;
import com.example.demo.transit.dto.RoutingSearchResponse;
import com.example.demo.transit.service.RoutingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public (NFR-3.1): no login required to search. */
@RestController
@RequestMapping("/api/v1/routing")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/search")
    public RoutingSearchResponse search(@Valid @RequestBody RoutingSearchRequest request) {
        return routingService.search(request);
    }
}
