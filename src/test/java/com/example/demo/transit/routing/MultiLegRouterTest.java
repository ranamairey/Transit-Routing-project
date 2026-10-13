package com.example.demo.transit.routing;

import com.example.demo.transit.routing.MultiLegRouter.Edge;
import com.example.demo.transit.routing.MultiLegRouter.FoundPath;
import com.example.demo.transit.routing.MultiLegRouter.SearchGraph;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiLegRouterTest {

    @Test
    void findsTransferBetweenTwoServisLines() {
        SearchGraph graph = new SearchGraph();
        graph.setFare(1L, new BigDecimal("1500"));
        graph.setFare(2L, new BigDecimal("2000"));

        // Line 1: mezzeh(10) -> jumhuriyya bridge(20)
        graph.add(10L, Edge.ride(20L, 1L, 3000, 480));
        // Line 2: jumhuriyya(20) -> muhajirin(30)
        graph.add(20L, Edge.ride(30L, 2L, 2500, 420));
        graph.add(-1L, Edge.walk(10L, 120, 90));
        graph.add(30L, Edge.walk(-2L, 80, 60));

        MultiLegRouter router = new MultiLegRouter();
        List<FoundPath> paths = router.findPaths(graph, -1L, -2L, 3, 1);

        assertFalse(paths.isEmpty());
        FoundPath path = paths.get(0);
        assertEquals(new BigDecimal("3500"), path.totalFare());
        assertEquals(90 + 480 + 420 + 60, path.totalDurationSeconds());
        assertEquals(200.0, path.walkingMeters());
        assertEquals(1, path.rideLegs() - 1, "one transfer");
        assertTrue(path.steps().stream().anyMatch(s -> s.routeId() == 1L));
        assertTrue(path.steps().stream().anyMatch(s -> s.routeId() == 2L));
    }

    @Test
    void alternativePathsKeepRealDurations() {
        // Two parallel lines between A(1) and B(2): fast line 1 (600s) and slower line 2 (900s).
        SearchGraph graph = new SearchGraph();
        graph.setFare(1L, new BigDecimal("1000"));
        graph.setFare(2L, new BigDecimal("800"));
        graph.add(1L, Edge.ride(2L, 1L, 5000, 600));
        graph.add(1L, Edge.ride(2L, 2L, 5000, 900));

        List<FoundPath> paths = new MultiLegRouter().findPaths(graph, 1L, 2L, 3, 3);

        assertEquals(2, paths.size(), "duplicates are not returned");
        assertEquals(600, paths.get(0).totalDurationSeconds());
        assertEquals(900, paths.get(1).totalDurationSeconds(), "penalty must not leak into the reported time");
        assertNotEquals(paths.get(0).steps(), paths.get(1).steps());
    }

    @Test
    void respectsMaxRideLegs() {
        SearchGraph graph = new SearchGraph();
        graph.add(1L, Edge.ride(2L, 1L, 1000, 100));
        graph.add(2L, Edge.ride(3L, 2L, 1000, 100));
        graph.add(3L, Edge.ride(4L, 3L, 1000, 100));

        assertTrue(new MultiLegRouter().findPaths(graph, 1L, 4L, 2, 1).isEmpty());
        assertEquals(1, new MultiLegRouter().findPaths(graph, 1L, 4L, 3, 1).size());
    }

    @Test
    void copyIsIndependentOfOriginal() {
        SearchGraph graph = new SearchGraph();
        graph.add(1L, Edge.ride(2L, 1L, 1000, 100));
        SearchGraph copy = graph.copy();
        copy.scaleRideTimes(2.0);
        copy.add(2L, Edge.walk(3L, 10, 5));

        assertEquals(100, graph.outgoing(1L).get(0).timeSeconds());
        assertEquals(200, copy.outgoing(1L).get(0).timeSeconds());
        assertTrue(graph.outgoing(2L).isEmpty());
    }

    @Test
    void objectivesPickDifferentTrips() {
        // 1 -> 3 either directly on slow line 9 (1800 s) or via 2 on two fast lines (300 + 300 s).
        SearchGraph graph = new SearchGraph();
        graph.setFare(7L, new BigDecimal("40"));
        graph.setFare(8L, new BigDecimal("40"));
        graph.setFare(9L, new BigDecimal("50"));
        graph.add(1L, Edge.ride(2L, 7L, 2000, 300));
        graph.add(2L, Edge.ride(3L, 8L, 2000, 300));
        graph.add(1L, Edge.ride(3L, 9L, 6000, 1800));

        MultiLegRouter router = new MultiLegRouter();
        FoundPath fastest = router.findPaths(graph, 1L, 3L, 4, 1, MultiLegRouter.Weights.FASTEST).get(0);
        FoundPath fewest = router.findPaths(graph, 1L, 3L, 4, 1, MultiLegRouter.Weights.FEWEST_TRANSFERS).get(0);
        FoundPath cheapest = router.findPaths(graph, 1L, 3L, 4, 1, MultiLegRouter.Weights.CHEAPEST).get(0);

        assertEquals(2, fastest.rideLegs());
        assertEquals(600, fastest.totalDurationSeconds());
        assertEquals(1, fewest.rideLegs());
        assertEquals(new BigDecimal("50"), cheapest.totalFare());
        assertNotEquals(MultiLegRouter.signature(fastest), MultiLegRouter.signature(fewest));
    }

    @Test
    void totalWalkingIsCapped() {
        SearchGraph graph = new SearchGraph();
        graph.add(1L, Edge.walk(2L, 900, 650));
        graph.add(2L, Edge.walk(3L, 900, 650));

        assertTrue(new MultiLegRouter(1500).findPaths(graph, 1L, 3L, 3, 1).isEmpty());
        assertEquals(1, new MultiLegRouter(2000).findPaths(graph, 1L, 3L, 3, 1).size());
    }
}
