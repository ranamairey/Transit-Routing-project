package com.example.demo.transit.web;

import com.example.demo.transit.dto.ReportRequest;
import com.example.demo.transit.dto.ReportResponse;
import com.example.demo.transit.service.ReportService;
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
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** FR-2.1: one-tap report; requires a logged-in user (NFR-3.1 anti-spam). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse submit(@Valid @RequestBody ReportRequest request) {
        return reportService.submit(request);
    }

    /** FR-2.2: recent alerts of a route (public). */
    @GetMapping
    public List<ReportResponse> recent(@RequestParam String routeId) {
        return reportService.recent(routeId);
    }
}
