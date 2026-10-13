package com.example.demo.transit.service;

import com.example.demo.transit.dto.GeoPointDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Pedestrian routing through an OSRM server (foot profile) with an in-memory cache and a circuit
 * breaker. When OSRM is not configured or unreachable, callers fall back to straight-line walking.
 */
@Component
public class WalkingRouter {

    private static final Logger log = LoggerFactory.getLogger(WalkingRouter.class);
    private static final int CACHE_SIZE = 20_000;

    public record Walk(double meters, List<GeoPointDto> path) {
    }

    private final String baseUrl;
    private final String profile;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final Map<String, Optional<Walk>> cache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Optional<Walk>> eldest) {
            return size() > CACHE_SIZE;
        }
    });
    private volatile long circuitOpenUntil = 0;
    private volatile int failures = 0;

    public WalkingRouter(@Value("${app.transit.osrm-url:}") String baseUrl,
                         @Value("${app.transit.osrm-profile:foot}") String profile,
                         @Value("${app.transit.osrm-timeout-ms:1500}") long timeoutMs,
                         ObjectMapper mapper) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.profile = profile;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        if (enabled()) {
            log.info("Walking routes via OSRM at {} (profile {})", this.baseUrl, profile);
        }
    }

    public boolean enabled() {
        return !baseUrl.isBlank();
    }

    /** Real walking distance (and geometry when requested) between two points, or empty on any failure. */
    public Optional<Walk> route(GeoPointDto a, GeoPointDto b, boolean withGeometry) {
        if (!enabled() || System.currentTimeMillis() < circuitOpenUntil) {
            return Optional.empty();
        }
        String key = key(a, b, withGeometry);
        Optional<Walk> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<Walk> result = fetch(a, b, withGeometry);
        if (result.isPresent() || failures == 0) {
            cache.put(key, result);
        }
        return result;
    }

    private Optional<Walk> fetch(GeoPointDto a, GeoPointDto b, boolean withGeometry) {
        String url = String.format(Locale.ROOT, "%s/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=%s&geometries=geojson&steps=false",
                baseUrl, profile, a.lng(), a.lat(), b.lng(), b.lat(), withGeometry ? "full" : "false");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(1500))
                    .header("User-Agent", "SmartSyrianTransit/1.0").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return failure("HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            if (!"Ok".equals(root.path("code").asText()) || root.path("routes").isEmpty()) {
                failures = 0;
                return Optional.empty(); // no path (e.g. unreachable); caller uses straight line
            }
            JsonNode r = root.path("routes").get(0);
            List<GeoPointDto> path = new ArrayList<>();
            if (withGeometry) {
                for (JsonNode c : r.path("geometry").path("coordinates")) {
                    path.add(new GeoPointDto(c.get(1).asDouble(), c.get(0).asDouble()));
                }
            }
            failures = 0;
            return Optional.of(new Walk(r.path("distance").asDouble(), path));
        } catch (Exception ex) {
            return failure(ex.getMessage());
        }
    }

    private Optional<Walk> failure(String why) {
        failures++;
        if (failures >= 3) {
            circuitOpenUntil = System.currentTimeMillis() + 60_000;
            failures = 0;
            log.warn("OSRM unreachable ({}); using straight-line walking for 60s", why);
        }
        return Optional.empty();
    }

    private static String key(GeoPointDto a, GeoPointDto b, boolean geometry) {
        return String.format(Locale.ROOT, "%.5f,%.5f>%.5f,%.5f:%b", a.lat(), a.lng(), b.lat(), b.lng(), geometry);
    }
}
