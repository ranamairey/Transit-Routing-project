package com.example.demo.transit.service;

import com.example.demo.transit.dto.AdminStatsDto;
import com.example.demo.transit.entity.CrowdsourceReport;
import com.example.demo.transit.repository.CrowdsourceReportRepository;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import com.example.demo.transit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AdminStatsService {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final CrowdsourceReportRepository reportRepository;
    private final TransitGraphFactory graphFactory;
    private final Clock clock;

    public AdminStatsService(StationRepository stationRepository, RouteRepository routeRepository,
                             UserRepository userRepository, CrowdsourceReportRepository reportRepository,
                             TransitGraphFactory graphFactory, Clock clock) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.graphFactory = graphFactory;
        this.clock = clock;
    }

    public AdminStatsDto stats() {
        Instant now = clock.instant();
        Instant dayAgo = now.minus(Duration.ofHours(24));
        List<CrowdsourceReport> all = reportRepository.findAll();
        Map<String, Long> byType = new TreeMap<>();
        long last24 = 0, pending = 0;
        for (CrowdsourceReport r : all) {
            if (!r.isApplied() && !r.isDismissed()) pending++;
            if (r.getSentAt() != null && r.getSentAt().isAfter(dayAgo)) {
                last24++;
                byType.merge(r.getReportType().name(), 1L, Long::sum);
            }
        }
        Map<String, Long> byStatus = new TreeMap<>();
        routeRepository.findAll().forEach(r -> byStatus.merge(graphFactory.effectiveStatus(r, now).name(), 1L, Long::sum));
        return new AdminStatsDto(
                stationRepository.count(), routeRepository.count(), userRepository.count(),
                all.size(), last24, pending, byType, byStatus);
    }
}
