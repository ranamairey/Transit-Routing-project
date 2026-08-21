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
 * Multi-leg transit search: ride edges (same route A→later stop) + walk/transfer edges.
 * State is (station, lastRoute) so continuing a line is cheaper than transferring.
 */
public class MultiLegRouter {

    public static final long WALK_ROUTE_ID = 0L;

    public List<FoundPath> findPaths(SearchGraph graph, long originId, long destId, int maxRideLegs, int kPaths) {
        List<FoundPath> results = new ArrayList<>();
        Map<Long, Double> routePenalty = new HashMap<>();

        for (int i = 0; i < kPaths; i++) {
            FoundPath path = dijkstra(graph, originId, destId, maxRideLegs, routePenalty);
            if (path == null) {
                break;
            }
            results.add(path);
            path.steps().stream()
                    .map(Step::routeId)
                    .filter(id -> id != WALK_ROUTE_ID)
                    .forEach(id -> routePenalty.merge(id, 2.5, (a, b) -> a * b));
        }
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
        record Item(Key key, int time, int rideLegs, BigDecimal fare, double walkMeters) {
        }

        Map<Key, Integer> bestTime = new HashMap<>();
        Map<Key, Item> parentItem = new HashMap<>();
        Map<Key, Step> viaStep = new HashMap<>();
        Map<Key, Key> parentKey = new HashMap<>();

        PriorityQueue<Item> queue = new PriorityQueue<>(Comparator.comparingInt(Item::time));
        Key start = new Key(originId, WALK_ROUTE_ID);
        Item startItem = new Item(start, 0, 0, BigDecimal.ZERO, 0);
        queue.add(startItem);
        bestTime.put(start, 0);

        Key bestDest = null;
        Item bestDestItem = null;

        while (!queue.isEmpty()) {
            Item current = queue.poll();
            Integer known = bestTime.get(current.key());
            if (known == null || current.time() > known) {
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
                int edgeTime = edge.timeSeconds();

                if (ride) {
                    double penalty = routePenalty.getOrDefault(edge.routeId(), 1.0);
                    edgeTime = (int) Math.round(edgeTime * penalty);
                    if (current.key().lastRouteId() != edge.routeId()) {
                        nextLegs++;
                        if (nextLegs > maxRideLegs) {
                            continue;
                        }
                        nextFare = nextFare.add(graph.fareOf(edge.routeId()));
                    }
                } else {
                    nextWalk += edge.distanceMeters();
                }

                long nextRoute = ride ? edge.routeId() : WALK_ROUTE_ID;
                Key nextKey = new Key(edge.toStationId(), nextRoute);
                int nextTime = current.time() + edgeTime;
                Integer previous = bestTime.get(nextKey);
                if (previous != null && previous <= nextTime) {
                    continue;
                }
                bestTime.put(nextKey, nextTime);
                Item nextItem = new Item(nextKey, nextTime, nextLegs, nextFare, nextWalk);
                parentItem.put(nextKey, current);
                parentKey.put(nextKey, current.key());
                viaStep.put(nextKey, new Step(
                        current.key().stationId(),
                        edge.toStationId(),
                        edge.routeId(),
                        edge.distanceMeters(),
                        edgeTime
                ));
                queue.add(nextItem);
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
        return new FoundPath(bestDestItem.fare(), bestDestItem.time(), bestDestItem.walkMeters(), steps);
    }

    public record Edge(long toStationId, long routeId, double distanceMeters, int timeSeconds) {
        public static Edge walk(long to, double meters, int seconds) {
            return new Edge(to, WALK_ROUTE_ID, meters, seconds);
        }

        public static Edge ride(long to, long routeId, double meters, int seconds) {
            return new Edge(to, routeId, meters, seconds);
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

        public void scaleRideTimes(double multiplier) {
            outgoing.replaceAll((from, edges) -> {
                List<Edge> scaled = new ArrayList<>(edges.size());
                for (Edge edge : edges) {
                    if (edge.routeId() == WALK_ROUTE_ID) {
                        scaled.add(edge);
                    } else {
                        scaled.add(new Edge(
                                edge.toStationId(),
                                edge.routeId(),
                                edge.distanceMeters(),
                                (int) Math.round(edge.timeSeconds() * multiplier)
                        ));
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
    }

    public static boolean samePath(FoundPath a, FoundPath b) {
        return Objects.equals(a.steps(), b.steps());
    }
}
