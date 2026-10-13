package com.example.demo.transit.web;

import com.example.demo.transit.domain.ReportType;
import com.example.demo.transit.dto.ReportResponse;
import com.example.demo.transit.dto.ReportSummaryDto;
import com.example.demo.transit.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** FR-3.2: review, filter and group crowdsourced reports. */
@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportResponse> list(@RequestParam(required = false) String routeId,
                                     @RequestParam(required = false) ReportType type,
                                     @RequestParam(required = false) Boolean applied,
                                     @RequestParam(required = false) Boolean dismissed,
                                     @RequestParam(required = false) Integer hours) {
        return reportService.adminList(routeId, type, applied, dismissed, hours);
    }

    @GetMapping("/summary")
    public List<ReportSummaryDto> summary() {
        return reportService.adminSummary();
    }

    @PostMapping("/{id}/approve")
    public ReportResponse approve(@PathVariable String id) {
        return reportService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public ReportResponse reject(@PathVariable String id) {
        return reportService.reject(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        reportService.delete(id);
    }
}
