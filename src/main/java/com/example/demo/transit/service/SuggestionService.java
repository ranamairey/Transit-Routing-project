package com.example.demo.transit.service;

import com.example.demo.transit.domain.StationKind;
import com.example.demo.transit.domain.SuggestionStatus;
import com.example.demo.transit.domain.SuggestionType;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.dto.SuggestionAcceptRequest;
import com.example.demo.transit.dto.SuggestionRequest;
import com.example.demo.transit.dto.SuggestionResponse;
import com.example.demo.transit.entity.PlaceSuggestion;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.repository.PlaceSuggestionRepository;
import com.example.demo.transit.repository.StationRepository;
import com.example.demo.transit.security.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Rider corrections to the network data and the admin review flow that applies them. */
@Service
public class SuggestionService {

    private static final int PER_HOUR = 10;

    private final PlaceSuggestionRepository repository;
    private final StationRepository stationRepository;
    private final TransitGraphFactory graphFactory;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public SuggestionService(PlaceSuggestionRepository repository, StationRepository stationRepository,
                             TransitGraphFactory graphFactory, RateLimiter rateLimiter, Clock clock) {
        this.repository = repository;
        this.stationRepository = stationRepository;
        this.graphFactory = graphFactory;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public SuggestionResponse submit(String reporter, SuggestionRequest request) {
        boolean hasPoint = request.location() != null && request.location().lat() != null && request.location().lng() != null;
        switch (request.type()) {
            case MISSING_PLACE -> {
                if (blank(request.name()) || !hasPoint) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "اقتراح مكان ناقص يتطلب الاسم وموقعه على الخريطة");
                }
            }
            case WRONG_LOCATION -> {
                if (blank(request.stationId()) || !hasPoint) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "حدد الموقف والموقع الصحيح على الخريطة");
                }
            }
            case WRONG_LINE -> {
                if (blank(request.routeId()) || blank(request.description())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "حدد الخط واكتب ما الخطأ فيه");
                }
            }
            default -> {
                if (blank(request.description())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "اكتب وصفاً للاقتراح");
                }
            }
        }
        var snapshot = graphFactory.snapshot();
        if (!blank(request.stationId()) && !snapshot.stationsById().containsKey(request.stationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة");
        }
        if (!blank(request.routeId()) && snapshot.routes().stream().noneMatch(r -> r.getId().equals(request.routeId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "الخط غير موجود");
        }
        if (!rateLimiter.tryAcquire("suggest:" + reporter, PER_HOUR, Duration.ofHours(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "تجاوزت الحد المسموح من الاقتراحات لهذه الساعة");
        }
        PlaceSuggestion s = new PlaceSuggestion();
        s.setType(request.type());
        s.setName(trim(request.name()));
        s.setNameEn(trim(request.nameEn()));
        s.setDescription(trim(request.description()));
        if (hasPoint) {
            s.setLatitude(request.location().lat());
            s.setLongitude(request.location().lng());
        }
        s.setStationId(trim(request.stationId()));
        s.setRouteId(trim(request.routeId()));
        s.setReporterId(reporter);
        s.setCreatedAt(clock.instant());
        return toResponse(repository.save(s));
    }

    public List<SuggestionResponse> list(SuggestionStatus status) {
        List<PlaceSuggestion> items = status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);
        return items.stream().limit(500).map(this::toResponse).toList();
    }

    public long pendingCount() {
        return repository.countByStatus(SuggestionStatus.PENDING);
    }

    /** Accepting a missing place creates a station; accepting a wrong location moves the station. */
    public SuggestionResponse accept(String id, String admin, SuggestionAcceptRequest edits) {
        PlaceSuggestion s = find(id);
        SuggestionAcceptRequest e = edits == null ? new SuggestionAcceptRequest(null, null, null, null, null) : edits;
        GeoPointDto point = e.location() != null && e.location().lat() != null ? e.location()
                : s.getLatitude() != null ? new GeoPointDto(s.getLatitude(), s.getLongitude()) : null;
        if (s.getType() == SuggestionType.MISSING_PLACE) {
            String name = !blank(e.name()) ? e.name().trim() : s.getName();
            if (blank(name) || point == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "اقتراح مكان ناقص يتطلب الاسم وموقعه على الخريطة");
            }
            Station station = new Station();
            station.setName(name);
            station.setNameEn(!blank(e.nameEn()) ? e.nameEn().trim() : s.getNameEn());
            station.setStationKind(e.stationKind() != null ? e.stationKind() : StationKind.POINT);
            station.setGovernorate(!blank(e.governorate()) ? e.governorate().trim() : "DAMASCUS");
            station.setLatitude(point.lat());
            station.setLongitude(point.lng());
            s.setResultStationId(stationRepository.save(station).getId());
            graphFactory.invalidate();
        } else if (s.getType() == SuggestionType.WRONG_LOCATION && point != null) {
            Station station = stationRepository.findById(s.getStationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المحطة غير موجودة"));
            station.setLatitude(point.lat());
            station.setLongitude(point.lng());
            stationRepository.save(station);
            s.setResultStationId(station.getId());
            graphFactory.invalidate();
        }
        s.setStatus(SuggestionStatus.ACCEPTED);
        s.setReviewedBy(admin);
        s.setReviewedAt(clock.instant());
        return toResponse(repository.save(s));
    }

    public SuggestionResponse reject(String id, String admin) {
        PlaceSuggestion s = find(id);
        s.setStatus(SuggestionStatus.REJECTED);
        s.setReviewedBy(admin);
        s.setReviewedAt(clock.instant());
        return toResponse(repository.save(s));
    }

    public void delete(String id) {
        repository.delete(find(id));
    }

    private PlaceSuggestion find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "الاقتراح غير موجود"));
    }

    private SuggestionResponse toResponse(PlaceSuggestion s) {
        var snapshot = graphFactory.snapshot();
        Station station = s.getStationId() == null ? null : snapshot.stationsById().get(s.getStationId());
        Map<String, String> routeNames = new java.util.HashMap<>();
        for (Route r : snapshot.routes()) {
            routeNames.put(r.getId(), r.getName());
        }
        return new SuggestionResponse(
                s.getId(), s.getType(), s.getStatus(), s.getName(), s.getNameEn(), s.getDescription(),
                s.getLatitude() != null ? new GeoPointDto(s.getLatitude(), s.getLongitude()) : null,
                s.getStationId(), station != null ? station.getName() : null,
                station != null ? new GeoPointDto(station.getLatitude(), station.getLongitude()) : null,
                s.getRouteId(), s.getRouteId() != null ? routeNames.get(s.getRouteId()) : null,
                s.getReporterId(), s.getCreatedAt(), s.getReviewedBy(), s.getReviewedAt(), s.getResultStationId());
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static String trim(String v) {
        return blank(v) ? null : v.trim();
    }
}
