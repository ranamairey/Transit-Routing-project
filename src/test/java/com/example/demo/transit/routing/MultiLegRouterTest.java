package com.example.demo.transit.routing;

import com.example.demo.transit.routing.MultiLegRouter.Edge;
import com.example.demo.transit.routing.MultiLegRouter.FoundPath;
import com.example.demo.transit.routing.MultiLegRouter.SearchGraph;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(path.steps().stream().anyMatch(s -> s.routeId() == 1L));
        assertTrue(path.steps().stream().anyMatch(s -> s.routeId() == 2L));
    }
}
