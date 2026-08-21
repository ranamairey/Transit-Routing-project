package com.example.demo.transit.service;

import com.example.demo.transit.config.TransitProperties;
import com.example.demo.transit.domain.ReportType;
import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.dto.ReportRequest;
import com.example.demo.transit.dto.ReportResponse;
import com.example.demo.transit.entity.CrowdsourceReport;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.repository.CrowdsourceReportRepository;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ReportService {

    private final CrowdsourceReportRepository reportRepository;
    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final TransitProperties properties;

    public ReportService(
            CrowdsourceReportRepository reportRepository,
            RouteRepository routeRepository,
            StationRepository stationRepository,
            TransitProperties properties
    ) {
        this.reportRepository = reportRepository;
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.properties = properties;
    }

    @Transactional
    public ReportResponse submit(ReportRequest request) {
        // قراءة هوية المستخدم المسجل تلقائياً من ترويسة Authorization المرفقة مع الطلب
        String reporterId = getCurrentUserIdentifier();

        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود"));

        CrowdsourceReport report = new CrowdsourceReport();
        report.setReportType(request.reportType());
        report.setSentAt(Instant.now());
        report.setReporterId(reporterId);
        report.setRoute(route);
        report.setNewFare(request.newFare());
        report.setDescription(request.description());
        if (request.stationId() != null) {
            Station station = stationRepository.findById(request.stationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة"));
            report.setStation(station);
        }
        if (request.location() != null && request.location().lat() != null) {
            report.setLatitude(request.location().lat());
            report.setLongitude(request.location().lng());
        }

        CrowdsourceReport saved = reportRepository.save(report);
        boolean applied = applyIfThresholdReached(route, request.reportType());
        return toResponse(saved, applied);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> recent(Long routeId) {
        Instant since = Instant.now().minus(Duration.ofHours(properties.getAlertWindowHours()));
        return reportRepository.findByRoute_IdInAndSentAtAfterOrderBySentAtDesc(List.of(routeId), since)
                .stream()
                .map(r -> toResponse(r, r.isApplied()))
                .toList();
    }

    private String getCurrentUserIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "إرسال البلاغ يتطلب توثيقاً وتسجيل دخول");
        }
        return auth.getName(); // يحصل على البريد الإلكتروني للمستخدم من الـ Token تلقائياً
    }

    private boolean applyIfThresholdReached(Route route, ReportType type) {
        Instant since = Instant.now().minus(Duration.ofHours(properties.getAlertWindowHours()));
        long distinct = reportRepository.countDistinctReporters(route.getId(), type, since);
        if (distinct < properties.getAutoApplyThreshold()) {
            return false;
        }

        List<CrowdsourceReport> pending = reportRepository
                .findByRoute_IdAndReportTypeAndAppliedFalseAndSentAtGreaterThanEqual(route.getId(), type, since);

        switch (type) {
            case CROWDING -> route.setStatus(RouteStatus.CROWDED);
            case ROUTE_CHANGE -> route.setStatus(RouteStatus.DIVERTED);
            case FARE_CHANGE -> pending.stream()
                    .map(CrowdsourceReport::getNewFare)
                    .filter(fare -> fare != null)
                    .findFirst()
                    .ifPresent(fare -> {
                        route.setCurrentFare(fare);
                        route.setStatus(RouteStatus.FARE_UPDATED);
                    });
            default -> {
                return false;
            }
        }
        pending.forEach(r -> r.setApplied(true));
        routeRepository.save(route);
        return true;
    }

    private ReportResponse toResponse(CrowdsourceReport report, boolean autoApplied) {
        return new ReportResponse(
                report.getId(),
                report.getReportType(),
                report.getSentAt(),
                report.getRoute() != null ? report.getRoute().getId() : null,
                report.getStation() != null ? report.getStation().getId() : null,
                report.getNewFare(),
                report.getDescription(),
                autoApplied
        );
    }
}