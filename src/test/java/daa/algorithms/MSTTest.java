package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MSTTest {

    @Test
    void findsKnownMinimumSpanningTree() {
        Graph graph = new Graph(4, false);

        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 2);
        graph.addEdge(2, 3, 3);
        graph.addEdge(0, 3, 10);
        graph.addEdge(0, 2, 6);

        Kruskal.Result kruskal = Kruskal.run(
                graph, new Metrics()
        );

        Prim.Result prim = Prim.run(
                graph, new Metrics()
        );

        assertEquals(9, kruskal.totalWeight());
        assertEquals(9, prim.totalWeight());

        assertEquals(1, kruskal.components());
        assertEquals(1, prim.components());

        assertValidForest(graph, kruskal.edges(), 1, 9);
        assertValidForest(graph, prim.edges(), 1, 9);
    }

    @Test
    void algorithmsAgreeOn100RandomConnectedGraphs() {
        Random random = new Random(42);

        for (int test = 0; test < 100; test++) {
            int n = 2 + random.nextInt(60);
            Graph graph = new Graph(n, false);

            for (int vertex = 1; vertex < n; vertex++) {
                int parent = random.nextInt(vertex);
                int weight = random.nextInt(101) - 50;

                graph.addEdge(parent, vertex, weight);
            }

            for (int i = 0; i < 4 * n; i++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n);
                int weight = random.nextInt(101) - 50;

                graph.addEdge(u, v, weight);
            }

            Kruskal.Result kruskal = Kruskal.run(
                    graph, new Metrics()
            );

            Prim.Result prim = Prim.run(
                    graph, new Metrics()
            );

            assertEquals(
                    kruskal.totalWeight(),
                    prim.totalWeight(),
                    "MST weights differ on random graph " + test
            );

            assertEquals(1, kruskal.components());
            assertEquals(1, prim.components());

            assertValidForest(
                    graph, kruskal.edges(), 1,
                    kruskal.totalWeight()
            );

            assertValidForest(
                    graph, prim.edges(), 1,
                    prim.totalWeight()
            );
        }
    }

    @Test
    void returnsForestForDisconnectedGraph() {
        Graph graph = new Graph(5, false);

        graph.addEdge(0, 1, 3);
        graph.addEdge(0, 1, 1);
        graph.addEdge(2, 3, -2);
        graph.addEdge(3, 3, -100);

        Kruskal.Result kruskal = Kruskal.run(
                graph, new Metrics()
        );

        Prim.Result prim = Prim.run(
                graph, new Metrics()
        );

        assertEquals(-1, kruskal.totalWeight());
        assertEquals(-1, prim.totalWeight());

        assertEquals(3, kruskal.components());
        assertEquals(3, prim.components());

        assertValidForest(graph, kruskal.edges(), 3, -1);
        assertValidForest(graph, prim.edges(), 3, -1);
    }

    @Test
    void handlesEmptyGraphAndIsolatedVertices() {
        Graph empty = new Graph(0, false);

        Kruskal.Result emptyKruskal = Kruskal.run(
                empty, new Metrics()
        );

        Prim.Result emptyPrim = Prim.run(
                empty, new Metrics()
        );

        assertEquals(0, emptyKruskal.totalWeight());
        assertEquals(0, emptyPrim.totalWeight());
        assertEquals(0, emptyKruskal.components());
        assertEquals(0, emptyPrim.components());
        assertTrue(emptyKruskal.edges().isEmpty());
        assertTrue(emptyPrim.edges().isEmpty());

        Graph isolated = new Graph(3, false);

        assertEquals(
                3,
                Kruskal.run(isolated, new Metrics()).components()
        );

        assertEquals(
                3,
                Prim.run(isolated, new Metrics()).components()
        );
    }

    @Test
    void rejectsDirectedGraphs() {
        Graph directed = new Graph(2, true);
        directed.addEdge(0, 1, 5);

        assertThrows(
                IllegalArgumentException.class,
                () -> Kruskal.run(directed, new Metrics())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Prim.run(directed, new Metrics())
        );
    }

    private static void assertValidForest(
            Graph graph,
            List<Edge> selected,
            int expectedComponents,
            long expectedWeight
    ) {
        assertEquals(
                graph.vertexCount() - expectedComponents,
                selected.size()
        );

        DSU forest = new DSU(
                graph.vertexCount(), new Metrics()
        );

        long actualWeight = 0;

        for (Edge edge : selected) {
            assertTrue(
                    belongsToGraph(graph, edge),
                    "Selected edge does not belong to the graph"
            );

            assertTrue(
                    forest.union(edge.u(), edge.v()),
                    "Selected edges contain a cycle"
            );

            actualWeight += edge.w();
        }

        assertEquals(expectedComponents, forest.components());
        assertEquals(expectedWeight, actualWeight);

        for (Edge edge : graph.edges()) {
            assertEquals(
                    forest.find(edge.u()),
                    forest.find(edge.v()),
                    "The forest does not span an input component"
            );
        }
    }

    private static boolean belongsToGraph(
            Graph graph,
            Edge selected
    ) {
        for (Edge edge : graph.edges()) {
            boolean sameEndpoints =
                    edge.u() == selected.u()
                            && edge.v() == selected.v();

            boolean reversedEndpoints =
                    edge.u() == selected.v()
                            && edge.v() == selected.u();

            if ((sameEndpoints || reversedEndpoints)
                    && edge.w() == selected.w()) {
                return true;
            }
        }

        return false;
    }
}