package com.example.demo.transit.web;

import com.example.demo.transit.dto.NetworkSnapshotDto;
import com.example.demo.transit.dto.RouteResponse;
import com.example.demo.transit.service.NetworkAdminService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Public read-only network data (routes + offline snapshot). */
@RestController
public class NetworkController {

    private final NetworkAdminService networkAdminService;

    public NetworkController(NetworkAdminService networkAdminService) {
        this.networkAdminService = networkAdminService;
    }

    @GetMapping("/api/v1/routes")
    public List<RouteResponse> routes() {
        return networkAdminService.listRoutes();
    }

    @GetMapping("/api/v1/routes/{id}")
    public RouteResponse route(@PathVariable String id) {
        return networkAdminService.getRoute(id);
    }

    /**
     * NFR-1.2 offline-first: the whole static network in one payload. Clients cache it and
     * revalidate with If-None-Match; an unchanged network answers 304 with no body.
     */
    @GetMapping("/api/v1/network")
    public ResponseEntity<NetworkSnapshotDto> network(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        NetworkSnapshotDto snapshot = networkAdminService.networkSnapshot();
        String etag = "\"" + snapshot.version() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(snapshot);
    }
}
