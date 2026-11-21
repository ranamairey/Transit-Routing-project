package com.example.demo.transit.service;

import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.dto.UsageStatsDto;
import com.example.demo.transit.dto.UsageStatsDto.DayCount;
import com.example.demo.transit.dto.UsageStatsDto.PlaceCount;
import com.example.demo.transit.dto.UsageStatsDto.TripCount;
import com.example.demo.transit.entity.SearchLog;
import com.example.demo.transit.repository.SearchLogRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Records every trip search (anonymously, off the request thread) and summarises them for the admin
 * console: busiest trips, most searched destinations, and searches that found no route (gaps in the network).
 */
@Service
public class UsageStatsService {

    private static final Logger log = LoggerFactory.getLogger(UsageStatsService.class);
    private static final ZoneId DAMASCUS = ZoneId.of("Asia/Damascus");

    private final SearchLogRepository repository;
    private final Clock clock;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "search-log");
        t.setDaemon(true);
        return t;
    });

    public UsageStatsService(SearchLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** One search as seen by the router; stored without any user identity, points rounded to ~100 m. */
    public record SearchTrace(String originName, String destinationName, String originStationId, String destinationStationId,
                              GeoPointDto originPoint, GeoPointDto destinationPoint) {
    }

    public void record(SearchTrace trace, int resultCount) {
        if (trace == null) {
            return;
        }
        SearchLog entry = new SearchLog();
        entry.setAt(clock.instant());
        entry.setOriginName(trace.originName());
        entry.setDestinationName(trace.destinationName());
        entry.setOriginStationId(trace.originStationId());
        entry.setDestinationStationId(trace.destinationStationId());
        if (trace.originPoint() != null) {
            entry.setOriginLat(round(trace.originPoint().lat()));
            entry.setOriginLng(round(trace.originPoint().lng()));
        }
        if (trace.destinationPoint() != null) {
            entry.setDestinationLat(round(trace.destinationPoint().lat()));
            entry.setDestinationLng(round(trace.destinationPoint().lng()));
        }
        entry.setResultCount(resultCount);
        writer.submit(() -> {
            try {
                repository.save(entry);
            } catch (Exception e) {
                log.debug("Could not store search log: {}", e.getMessage());
            }
        });
    }

    public UsageStatsDto stats(int days) {
        int window = Math.max(1, Math.min(days, 90));
        Instant since = clock.instant().minus(Duration.ofDays(window));
        List<SearchLog> logs = repository.findByAtAfter(since);

        Map<String, long[]> perDay = new java.util.TreeMap<>();
        LocalDate today = LocalDate.now(clock.withZone(DAMASCUS));
        for (int i = window - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i).toString(), new long[2]);
        }
        Map<String, Agg> trips = new HashMap<>();
        Map<String, Agg> failed = new HashMap<>();
        Map<String, Long> destinations = new HashMap<>();
        long noResult = 0;
        for (SearchLog l : logs) {
            String day = l.getAt().atZone(DAMASCUS).toLocalDate().toString();
            long[] d = perDay.computeIfAbsent(day, k -> new long[2]);
            d[0]++;
            boolean none = l.getResultCount() == 0;
            if (none) {
                d[1]++;
                noResult++;
            }
            String key = label(l.getOriginName(), l.getOriginLat(), l.getOriginLng()) + "→" + label(l.getDestinationName(), l.getDestinationLat(), l.getDestinationLng());
            (none ? failed : trips).computeIfAbsent(key, k -> new Agg(l)).add(l);
            destinations.merge(label(l.getDestinationName(), l.getDestinationLat(), l.getDestinationLng()), 1L, Long::sum);
        }
        List<DayCount> dayCounts = new ArrayList<>();
        perDay.forEach((date, c) -> dayCounts.add(new DayCount(date, c[0], c[1])));
        List<PlaceCount> topDest = destinations.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new PlaceCount(e.getKey(), e.getValue()))
                .toList();
        return new UsageStatsDto(window, logs.size(), noResult, dayCounts, top(trips), top(failed), topDest);
    }

    private static List<TripCount> top(Map<String, Agg> groups) {
        return groups.values().stream()
                .sorted(Comparator.comparingLong((Agg a) -> a.count).reversed().thenComparing(a -> a.last, Comparator.reverseOrder()))
                .limit(15)
                .map(Agg::toDto)
                .toList();
    }

    /** Named places group by name; dropped pins group by their rounded point. */
    private static String label(String name, Double lat, Double lng) {
        boolean pinLike = name == null || name.isBlank() || name.equals("نقطة الانطلاق") || name.equals("الوجهة");
        if (!pinLike) {
            return name;
        }
        return lat == null ? "?" : String.format(java.util.Locale.ROOT, "%.3f, %.3f", lat, lng);
    }

    private static Double round(Double v) {
        return v == null ? null : Math.round(v * 1000d) / 1000d;
    }

    private static final class Agg {
        final SearchLog sample;
        long count;
        Instant last;

        Agg(SearchLog sample) {
            this.sample = sample;
        }

        void add(SearchLog l) {
            count++;
            if (last == null || l.getAt().isAfter(last)) {
                last = l.getAt();
            }
        }

        TripCount toDto() {
            SearchLog s = sample;
            return new TripCount(
                    label(s.getOriginName(), s.getOriginLat(), s.getOriginLng()),
                    label(s.getDestinationName(), s.getDestinationLat(), s.getDestinationLng()),
                    count, last,
                    s.getOriginLat() == null ? null : new GeoPointDto(s.getOriginLat(), s.getOriginLng()),
                    s.getDestinationLat() == null ? null : new GeoPointDto(s.getDestinationLat(), s.getDestinationLng()));
        }
    }

    @PreDestroy
    void shutdown() {
        writer.shutdown();
    }
}
