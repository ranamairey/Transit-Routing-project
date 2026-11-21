package com.example.demo.transit.service;

import com.example.demo.transit.dto.AlertDto;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.SavedPlace;
import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Saved places and followed lines, stored on the rider's account so they follow the rider across devices. */
@Service
public class AccountFeaturesService {

    static final int MAX_PLACES = 20;
    static final int MAX_FOLLOWS = 50;

    private final UserRepository users;
    private final TransitGraphFactory graphFactory;
    private final ReportService reportService;

    public AccountFeaturesService(UserRepository users, TransitGraphFactory graphFactory, ReportService reportService) {
        this.users = users;
        this.graphFactory = graphFactory;
        this.reportService = reportService;
    }

    public List<SavedPlace> places(String email) {
        List<SavedPlace> places = user(email).getSavedPlaces();
        return places == null ? List.of() : places;
    }

    public List<SavedPlace> savePlaces(String email, List<SavedPlace> places) {
        if (places == null) {
            places = List.of();
        }
        if (places.size() > MAX_PLACES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "يمكن حفظ 20 مكاناً كحد أقصى");
        }
        List<SavedPlace> clean = new ArrayList<>();
        for (SavedPlace p : places) {
            boolean hasStation = p.stationId() != null && !p.stationId().isBlank();
            boolean hasPoint = p.lat() != null && p.lng() != null && Math.abs(p.lat()) <= 90 && Math.abs(p.lng()) <= 180;
            if (!hasStation && !hasPoint) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "المكان المحفوظ يحتاج موقفاً أو نقطة على الخريطة");
            }
            String kind = p.kind() == null ? "OTHER" : switch (p.kind()) {
                case "HOME", "WORK" -> p.kind();
                default -> "OTHER";
            };
            clean.add(new SavedPlace(
                    p.id() == null || p.id().isBlank() ? UUID.randomUUID().toString() : p.id(),
                    kind, cut(p.label(), 40), cut(p.name(), 160), hasStation ? p.stationId() : null,
                    hasPoint ? p.lat() : null, hasPoint ? p.lng() : null));
        }
        User user = user(email);
        user.setSavedPlaces(clean);
        users.save(user);
        return clean;
    }

    public Set<String> follows(String email) {
        Set<String> ids = user(email).getFollowedRouteIds();
        return ids == null ? Set.of() : ids;
    }

    public Set<String> saveFollows(String email, List<String> routeIds) {
        Set<String> known = graphFactory.snapshot().routes().stream().map(Route::getId).collect(Collectors.toSet());
        Set<String> clean = new LinkedHashSet<>();
        for (String id : routeIds == null ? List.<String>of() : routeIds) {
            if (id != null && known.contains(id)) {
                clean.add(id);
            }
        }
        if (clean.size() > MAX_FOLLOWS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "يمكن متابعة 50 خطاً كحد أقصى");
        }
        User user = user(email);
        user.setFollowedRouteIds(clean);
        users.save(user);
        return clean;
    }

    /** Recent rider reports on the lines this rider follows. */
    public List<AlertDto> alerts(String email) {
        Set<String> follows = follows(email);
        return follows.isEmpty() ? List.of() : reportService.alertsForRouteIds(follows);
    }

    private User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "المستخدم غير موجود"));
    }

    private static String cut(String v, int max) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
