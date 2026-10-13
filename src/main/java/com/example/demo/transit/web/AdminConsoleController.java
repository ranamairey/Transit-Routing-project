package com.example.demo.transit.web;

import com.example.demo.transit.dto.AdminStatsDto;
import com.example.demo.transit.dto.NetworkImportRequest;
import com.example.demo.transit.dto.NetworkImportResult;
import com.example.demo.transit.dto.NetworkSnapshotDto;
import com.example.demo.transit.service.AdminStatsService;
import com.example.demo.transit.service.NetworkAdminService;
import com.example.demo.transit.service.NetworkImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard numbers plus bulk import/export for the admin console. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminConsoleController {

    private final AdminStatsService statsService;
    private final NetworkImportService importService;
    private final NetworkAdminService networkAdminService;

    public AdminConsoleController(AdminStatsService statsService, NetworkImportService importService,
                                  NetworkAdminService networkAdminService) {
        this.statsService = statsService;
        this.importService = importService;
        this.networkAdminService = networkAdminService;
    }

    @GetMapping("/stats")
    public AdminStatsDto stats() {
        return statsService.stats();
    }

    @PostMapping("/network/import")
    public NetworkImportResult importNetwork(@Valid @RequestBody NetworkImportRequest request) {
        return importService.importNetwork(request);
    }

    @GetMapping("/network/export")
    public NetworkSnapshotDto export() {
        return networkAdminService.networkSnapshot();
    }
}
