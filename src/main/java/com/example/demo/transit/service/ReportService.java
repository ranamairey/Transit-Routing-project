package com.example.demo.transit.service;

import com.example.demo.transit.config.SecurityLimits;
import com.example.demo.transit.config.TransitProperties;
import com.example.demo.transit.security.RateLimiter;
import com.example.demo.transit.domain.ReportType;
import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.dto.AlertDto;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.dto.ReportRequest;
import com.example.demo.transit.dto.ReportResponse;
import com.example.demo.transit.dto.ReportSummaryDto;
import com.example.demo.transit.entity.CrowdsourceReport;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.repository.CrowdsourceReportRepository;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import com.example.demo.transit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** FR-2.1 / FR-2.2 / FR-3.2: crowdsourced reports, live alerts and the admin review flow. */
@Service
public class ReportService {

    private final CrowdsourceReportRepository reportRepository;
    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final TransitGraphFactory graphFactory;
    private final TransitProperties properties;
    private final SecurityLimits limits;
    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final UserRepository userRepository;
    private final boolean requireVerified;

    public ReportService(CrowdsourceReportRepository reportRepository,
                         RouteRepository routeRepository,
                         StationRepository stationRepository,
                         TransitGraphFactory graphFactory,
                         TransitProperties properties,
                         SecurityLimits limits,
                         RateLimiter rateLimiter,
                         Clock clock,
                         UserRepository userRepository,
                         @Value("${app.auth.require-verified-for-reports:false}") boolean requireVerified) {
        this.userRepository = userRepository;
        this.requireVerified = requireVerified;
        this.reportRepository = reportRepository;
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.graphFactory = graphFactory;
        this.properties = properties;
        this.limits = limits;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    // ---------- riders ----------

    public ReportResponse submit(ReportRequest request) {
        String reporterId = currentUser();
        if (requireVerified && userRepository.findByEmail(reporterId).map(u -> !u.isEmailVerified()).orElse(true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "يجب تأكيد البريد الإلكتروني قبل إرسال البلاغات");
        }

        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود"));
        if (request.reportType() == ReportType.FARE_CHANGE && request.newFare() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "بلاغ تغيير التسعيرة يتطلب تحديد التسعيرة الجديدة");
        }
        // Anti-spam: one report per user per route per cooldown, and a global hourly cap.
        Duration cooldown = Duration.ofMinutes(limits.getReportCooldownMinutes());
        String routeKey = "report:" + reporterId + ":" + route.getId();
        if (!rateLimiter.tryAcquire("reports:" + reporterId, limits.getReportsPerHour(), Duration.ofHours(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "تجاوزت الحد المسموح من البلاغات لهذه الساعة");
        }
        if (!rateLimiter.tryAcquire(routeKey, 1, cooldown)) {
            long wait = Math.max(1, rateLimiter.secondsUntilFree(routeKey, cooldown) / 60);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "أرسلت بلاغاً عن هذا الخط قبل قليل، يمكنك الإبلاغ مجدداً بعد " + wait + " دقيقة");
        }

        CrowdsourceReport report = new CrowdsourceReport();
        report.setReportType(request.reportType());
        report.setSentAt(clock.instant());
        report.setReporterId(reporterId);
        report.setRouteId(route.getId());
        report.setNewFare(request.newFare());
        report.setDescription(request.description());
        if (request.stationId() != null && !request.stationId().isBlank()) {
            Station station = stationRepository.findById(request.stationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة"));
            report.setStationId(station.getId());
            if (request.location() == null) {
                report.setLatitude(station.getLatitude());
                report.setLongitude(station.getLongitude());
            }
        }
        if (request.location() != null && request.location().lat() != null) {
            report.setLatitude(request.location().lat());
            report.setLongitude(request.location().lng());
        }

        CrowdsourceReport saved = reportRepository.save(report);
        boolean applied = applyIfThresholdReached(route, request.reportType());
        if (applied) {
            saved = reportRepository.findById(saved.getId()).orElse(saved);
        }
        return toResponse(saved, applied, routeNames(), stationNames());
    }

    /** Recent (non-rejected) reports of one route inside the alert window. */
    public List<ReportResponse> recent(String routeId) {
        Instant since = windowStart();
        Map<String, String> routes = routeNames();
        Map<String, String> stations = stationNames();
        return reportRepository.findByRouteIdAndSentAtAfterAndDismissedFalseOrderBySentAtDesc(routeId, since)
                .stream()
                .map(r -> toResponse(r, false, routes, stations))
                .toList();
    }

    /** FR-2.2: alerts for the routes of a search result. */
    public List<AlertDto> alertsForRoutes(Collection<String> routeIds, List<Route> routes) {
        if (routeIds.isEmpty()) {
            return List.of();
        }
        Map<String, String> names = routes.stream().collect(Collectors.toMap(Route::getId, Route::getName, (a, b) -> a));
        return reportRepository.findByRouteIdInAndSentAtAfterAndDismissedFalseOrderBySentAtDesc(routeIds, windowStart())
                .stream()
                .limit(20)
                .map(r -> new AlertDto(r.getId(), r.getRouteId(), names.get(r.getRouteId()), r.getReportType(),
                        r.getDescription(), r.getNewFare(), r.getSentAt(), r.isApplied()))
                .toList();
    }

    // ---------- admin (FR-3.2) ----------

    public com.example.demo.transit.dto.PageDto<ReportResponse> adminList(String routeId, ReportType type, Boolean applied,
                                                                          Boolean dismissed, Integer hours, int page, int size) {
        Instant since = clock.instant().minus(Duration.ofHours(hours != null && hours > 0 ? hours : 24 * 7));
        Map<String, String> routes = routeNames();
        Map<String, String> stations = stationNames();
        List<CrowdsourceReport> matching = reportRepository.findBySentAtAfterOrderBySentAtDesc(since).stream()
                .filter(r -> routeId == null || routeId.isBlank() || routeId.equals(r.getRouteId()))
                .filter(r -> type == null || type == r.getReportType())
                .filter(r -> applied == null || applied == r.isApplied())
                .filter(r -> dismissed == null || dismissed == r.isDismissed())
                .toList();
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        List<ReportResponse> items = matching.stream()
                .skip((long) safePage * safeSize)
                .limit(safeSize)
                .map(r -> toResponse(r, false, routes, stations))
                .toList();
        return new com.example.demo.transit.dto.PageDto<>(items, safePage, safeSize, matching.size());
    }

    /** Reports grouped by route + type inside the alert window, with distinct reporter counts. */
    public List<ReportSummaryDto> adminSummary() {
        Instant now = clock.instant();
        Map<String, Route> routes = routeRepository.findAll().stream()
                .collect(Collectors.toMap(Route::getId, Function.identity(), (a, b) -> a));
        Map<String, List<CrowdsourceReport>> groups = new LinkedHashMap<>();
        for (CrowdsourceReport r : reportRepository.findBySentAtAfterOrderBySentAtDesc(windowStart())) {
            if (r.isDismissed()) {
                continue;
            }
            groups.computeIfAbsent(r.getRouteId() + "#" + r.getReportType(), k -> new ArrayList<>()).add(r);
        }
        List<ReportSummaryDto> summary = new ArrayList<>();
        for (List<CrowdsourceReport> group : groups.values()) {
            CrowdsourceReport first = group.get(0);
            Route route = routes.get(first.getRouteId());
            long distinct = group.stream().map(CrowdsourceReport::getReporterId).filter(Objects::nonNull).distinct().count();
            long pending = group.stream().filter(r -> !r.isApplied()).count();
            long pendingDistinct = group.stream().filter(r -> !r.isApplied())
                    .map(CrowdsourceReport::getReporterId).distinct().count();
            summary.add(new ReportSummaryDto(
                    first.getRouteId(),
                    route != null ? route.getName() : null,
                    route != null ? graphFactory.effectiveStatus(route, now) : null,
                    first.getReportType(),
                    group.size(),
                    distinct,
                    pending,
                    properties.getAutoApplyThreshold(),
                    pendingDistinct >= properties.getAutoApplyThreshold(),
                    group.stream().map(CrowdsourceReport::getSentAt).max(Comparator.naturalOrder()).orElse(null)
            ));
        }
        summary.sort(Comparator.comparingLong(ReportSummaryDto::reportCount).reversed());
        return summary;
    }

    /** Admin approves one report: the route status / fare is applied immediately. */
    public ReportResponse approve(String reportId) {
        CrowdsourceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "البلاغ غير موجود"));
        Route route = routeRepository.findById(report.getRouteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود"));
        applyToRoute(route, report.getReportType(), report.getNewFare());
        report.setApplied(true);
        report.setDismissed(false);
        report.setReviewedBy(currentUser());
        report.setReviewedAt(clock.instant());
        CrowdsourceReport saved = reportRepository.save(report);
        return toResponse(saved, false, routeNames(), stationNames());
    }

    public ReportResponse reject(String reportId) {
        CrowdsourceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "البلاغ غير موجود"));
        report.setDismissed(true);
        report.setReviewedBy(currentUser());
        report.setReviewedAt(clock.instant());
        CrowdsourceReport saved = reportRepository.save(report);
        return toResponse(saved, false, routeNames(), stationNames());
    }

    public void delete(String reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "البلاغ غير موجود");
        }
        reportRepository.deleteById(reportId);
    }

    // ---------- internals ----------

    private Instant windowStart() {
        return clock.instant().minus(Duration.ofHours(properties.getAlertWindowHours()));
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "إرسال البلاغ يتطلب توثيقاً وتسجيل دخول");
        }
        return auth.getName();
    }

    /**
     * FR-3.2 automation: when the same report type arrives from N distinct users inside the window,
     * the route status / fare is updated without an admin.
     */
    private boolean applyIfThresholdReached(Route route, ReportType type) {
        Instant since = windowStart();
        List<CrowdsourceReport> pending = reportRepository
                .findByRouteIdAndReportTypeAndAppliedFalseAndDismissedFalseAndSentAtGreaterThanEqual(route.getId(), type, since);
        long distinct = pending.stream().map(CrowdsourceReport::getReporterId).filter(Objects::nonNull).distinct().count();
        if (distinct < properties.getAutoApplyThreshold()) {
            return false;
        }
        java.math.BigDecimal fare = pending.stream()
                .sorted(Comparator.comparing(CrowdsourceReport::getSentAt).reversed())
                .map(CrowdsourceReport::getNewFare)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (!applyToRoute(route, type, fare)) {
            return false;
        }
        Instant now = clock.instant();
        pending.forEach(r -> {
            r.setApplied(true);
            r.setReviewedBy("auto");
            r.setReviewedAt(now);
        });
        reportRepository.saveAll(pending);
        return true;
    }

    private boolean applyToRoute(Route route, ReportType type, java.math.BigDecimal fare) {
        switch (type) {
            case CROWDING -> route.setStatus(RouteStatus.CROWDED);
            case ROUTE_CHANGE -> route.setStatus(RouteStatus.DIVERTED);
            case DELAY -> route.setStatus(RouteStatus.DELAYED);
            case FARE_CHANGE -> {
                if (fare == null) {
                    return false;
                }
                route.setCurrentFare(fare);
                route.setStatus(RouteStatus.FARE_UPDATED);
            }
            default -> {
                return false;
            }
        }
        route.setStatusUpdatedAt(clock.instant());
        routeRepository.save(route);
        graphFactory.invalidate();
        return true;
    }

    private Map<String, String> routeNames() {
        Map<String, String> names = new HashMap<>();
        routeRepository.findAll().forEach(r -> names.put(r.getId(), r.getName()));
        return names;
    }

    private Map<String, String> stationNames() {
        Map<String, String> names = new HashMap<>();
        stationRepository.findAll().forEach(s -> names.put(s.getId(), s.getName()));
        return names;
    }

    private ReportResponse toResponse(CrowdsourceReport r, boolean autoApplied,
                                      Map<String, String> routes, Map<String, String> stations) {
        GeoPointDto location = r.getLatitude() != null && r.getLongitude() != null
                ? new GeoPointDto(r.getLatitude(), r.getLongitude()) : null;
        return new ReportResponse(
                r.getId(),
                r.getReportType(),
                r.getSentAt(),
                r.getRouteId(),
                routes.get(r.getRouteId()),
                r.getStationId(),
                r.getStationId() != null ? stations.get(r.getStationId()) : null,
                r.getNewFare(),
                r.getDescription(),
                location,
                autoApplied,
                r.isApplied(),
                r.isDismissed(),
                r.getReviewedBy(),
                r.getReviewedAt()
        );
    }
}
