package com.example.demo.transit.routing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Multi-leg transit search (FR-1.2): ride edges (same route A→later stop) + walk/transfer edges.
 * State is (station, lastRoute) so continuing a line is cheaper than transferring.
 *
 * <p>Alternative paths are found by re-running Dijkstra with a ranking penalty on routes already
 * used. The penalty only influences the search order; the durations reported in the result are
 * always the real travel times.
 */
public class MultiLegRouter {

    public static final long WALK_ROUTE_ID = 0L;

    /** Seconds of penalty added to each transfer so the search prefers fewer changes. */
    public static final int TRANSFER_PENALTY_SECONDS = 180;

    private static final double REUSED_ROUTE_PENALTY = 2.5;

    public List<FoundPath> findPaths(SearchGraph graph, long originId, long destId, int maxRideLegs, int kPaths) {
        List<FoundPath> results = new ArrayList<>();
        Map<Long, Double> routePenalty = new HashMap<>();

        for (int i = 0; i < kPaths; i++) {
            FoundPath path = dijkstra(graph, originId, destId, maxRideLegs, routePenalty);
            if (path == null) {
                break;
            }
            boolean duplicate = results.stream().anyMatch(existing -> samePath(existing, path));
            if (duplicate) {
                break;
            }
            results.add(path);
            path.steps().stream()
                    .map(Step::routeId)
                    .filter(id -> id != WALK_ROUTE_ID)
                    .distinct()
                    .forEach(id -> routePenalty.merge(id, REUSED_ROUTE_PENALTY, (a, b) -> a * b));
        }
        results.sort(Comparator.comparingInt(FoundPath::totalDurationSeconds));
        return results;
    }

    private FoundPath dijkstra(
            SearchGraph graph,
            long originId,
            long destId,
            int maxRideLegs,
            Map<Long, Double> routePenalty
    ) {
        record Key(long stationId, long lastRouteId) {
        }
        record Item(Key key, long cost, int realTime, int rideLegs, BigDecimal fare, double walkMeters) {
        }

        Map<Key, Long> bestCost = new HashMap<>();
        Map<Key, Step> viaStep = new HashMap<>();
        Map<Key, Key> parentKey = new HashMap<>();

        PriorityQueue<Item> queue = new PriorityQueue<>(Comparator.comparingLong(Item::cost));
        Key start = new Key(originId, WALK_ROUTE_ID);
        queue.add(new Item(start, 0, 0, 0, BigDecimal.ZERO, 0));
        bestCost.put(start, 0L);

        Key bestDest = null;
        Item bestDestItem = null;

        while (!queue.isEmpty()) {
            Item current = queue.poll();
            Long known = bestCost.get(current.key());
            if (known == null || current.cost() > known) {
                continue;
            }
            if (current.key().stationId() == destId) {
                bestDest = current.key();
                bestDestItem = current;
                break;
            }

            for (Edge edge : graph.outgoing(current.key().stationId())) {
                boolean ride = edge.routeId() != WALK_ROUTE_ID;
                int nextLegs = current.rideLegs();
                BigDecimal nextFare = current.fare();
                double nextWalk = current.walkMeters();
                long edgeCost = edge.timeSeconds();

                if (ride) {
                    double penalty = routePenalty.getOrDefault(edge.routeId(), 1.0);
                    edgeCost = Math.round(edgeCost * penalty);
                    if (current.key().lastRouteId() != edge.routeId()) {
                        nextLegs++;
                        if (nextLegs > maxRideLegs) {
                            continue;
                        }
                        nextFare = nextFare.add(graph.fareOf(edge.routeId()));
                        if (nextLegs > 1) {
                            edgeCost += TRANSFER_PENALTY_SECONDS;
                        }
                    }
                } else {
                    nextWalk += edge.distanceMeters();
                }

                long nextRoute = ride ? edge.routeId() : WALK_ROUTE_ID;
                Key nextKey = new Key(edge.toStationId(), nextRoute);
                long nextCost = current.cost() + edgeCost;
                Long previous = bestCost.get(nextKey);
                if (previous != null && previous <= nextCost) {
                    continue;
                }
                bestCost.put(nextKey, nextCost);
                int nextRealTime = current.realTime() + edge.timeSeconds();
                parentKey.put(nextKey, current.key());
                viaStep.put(nextKey, new Step(
                        current.key().stationId(),
                        edge.toStationId(),
                        edge.routeId(),
                        edge.distanceMeters(),
                        edge.timeSeconds()
                ));
                queue.add(new Item(nextKey, nextCost, nextRealTime, nextLegs, nextFare, nextWalk));
            }
        }

        if (bestDest == null) {
            return null;
        }

        List<Step> steps = new ArrayList<>();
        Key cursor = bestDest;
        while (viaStep.containsKey(cursor)) {
            steps.add(0, viaStep.get(cursor));
            cursor = parentKey.get(cursor);
        }
        return new FoundPath(bestDestItem.fare(), bestDestItem.realTime(), bestDestItem.walkMeters(), steps);
    }

    public record Edge(long toStationId, long routeId, double distanceMeters, int timeSeconds) {
        public static Edge walk(long to, double meters, int seconds) {
            return new Edge(to, WALK_ROUTE_ID, meters, seconds);
        }

        public static Edge ride(long to, long routeId, double meters, int seconds) {
            return new Edge(to, routeId, meters, seconds);
        }

        public Edge scaled(double multiplier) {
            return new Edge(toStationId, routeId, distanceMeters, (int) Math.max(1, Math.round(timeSeconds * multiplier)));
        }
    }

    public static final class SearchGraph {
        private final Map<Long, List<Edge>> outgoing = new HashMap<>();
        private final Map<Long, BigDecimal> fares = new HashMap<>();

        public void setFare(long routeId, BigDecimal fare) {
            fares.put(routeId, fare != null ? fare : BigDecimal.ZERO);
        }

        public BigDecimal fareOf(long routeId) {
            return fares.getOrDefault(routeId, BigDecimal.ZERO);
        }

        public void add(long from, Edge edge) {
            outgoing.computeIfAbsent(from, id -> new ArrayList<>()).add(edge);
        }

        public List<Edge> outgoing(long from) {
            return outgoing.getOrDefault(from, List.of());
        }

        /** Deep copy so a cached base graph can be customised per request. */
        public SearchGraph copy() {
            SearchGraph copy = new SearchGraph();
            outgoing.forEach((from, edges) -> copy.outgoing.put(from, new ArrayList<>(edges)));
            copy.fares.putAll(fares);
            return copy;
        }

        /** Multiplies the time of every ride edge (peak hours). */
        public void scaleRideTimes(double multiplier) {
            scaleRideTimes(id -> multiplier);
        }

        /** Multiplies the time of ride edges per route (crowding / diversion reports). */
        public void scaleRideTimes(java.util.function.LongToDoubleFunction multiplierForRoute) {
            outgoing.replaceAll((from, edges) -> {
                List<Edge> scaled = new ArrayList<>(edges.size());
                for (Edge edge : edges) {
                    if (edge.routeId() == WALK_ROUTE_ID) {
                        scaled.add(edge);
                    } else {
                        double m = multiplierForRoute.applyAsDouble(edge.routeId());
                        scaled.add(m == 1.0 ? edge : edge.scaled(m));
                    }
                }
                return scaled;
            });
        }
    }

    public record Step(long fromStationId, long toStationId, long routeId, double distanceMeters, int timeSeconds) {
    }

    public record FoundPath(BigDecimal totalFare, int totalDurationSeconds, double walkingMeters, List<Step> steps) {
        public FoundPath {
            steps = List.copyOf(steps);
        }

        public int rideLegs() {
            int legs = 0;
            long last = WALK_ROUTE_ID;
            for (Step step : steps) {
                if (step.routeId() != WALK_ROUTE_ID && step.routeId() != last) {
                    legs++;
                }
                last = step.routeId();
            }
            return legs;
        }
    }

    public static boolean samePath(FoundPath a, FoundPath b) {
        return Objects.equals(a.steps(), b.steps());
    }
}
