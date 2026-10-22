package daa.algorithms;

import daa.graph.Graph;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KahnTest {

    @Test
    void returnsValidOrderForKnownDag() {
        Graph graph = new Graph(6, true);

        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, 4);
        graph.addEdge(2, 3, 1);
        graph.addEdge(2, 4, 2);
        graph.addEdge(4, 5, 6);

        Metrics metrics = new Metrics();
        Kahn.Result result = Kahn.run(graph, metrics);

        assertFalse(result.cyclic());
        assertEquals(6, result.order().length);
        assertTrue(Kahn.isValidOrder(graph, result.order()));

        assertEquals(graph.edgeCount(), metrics.edgeScans);

        assertArrayEquals(
                new int[]{0, 0, 2, 1, 1, 1},
                graph.indegrees()
        );
    }

    @Test
    void returnsValidOrdersFor100RandomDags() {
        Random random = new Random(42);

        for (int test = 0; test < 100; test++) {
            int n = 2 + random.nextInt(100);
            Graph graph = new Graph(n, true);

            int[] permutation = new int[n];

            for (int i = 0; i < n; i++) {
                permutation[i] = i;
            }

            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);

                int temporary = permutation[i];
                permutation[i] = permutation[j];
                permutation[j] = temporary;
            }

            for (int i = 0; i < 4 * n; i++) {
                int first = random.nextInt(n);
                int second = random.nextInt(n);

                if (first == second) {
                    continue;
                }

                int earlier = Math.min(first, second);
                int later = Math.max(first, second);

                graph.addEdge(
                        permutation[earlier],
                        permutation[later],
                        1
                );
            }

            Metrics metrics = new Metrics();
            Kahn.Result result = Kahn.run(graph, metrics);

            assertFalse(
                    result.cyclic(),
                    "Cycle reported on DAG " + test
            );

            assertTrue(
                    Kahn.isValidOrder(graph, result.order()),
                    "Invalid order on DAG " + test
            );

            assertEquals(
                    graph.edgeCount(),
                    metrics.edgeScans
            );
        }
    }

    @Test
    void detectsCycleEvenWithAnIndependentSource() {
        Graph graph = new Graph(5, true);

        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);

        graph.addEdge(3, 4, 1);

        Kahn.Result result = Kahn.run(graph, new Metrics());

        assertTrue(result.cyclic());

        assertEquals(0, result.order().length);
    }

    @Test
    void detectsSelfLoop() {
        Graph graph = new Graph(1, true);
        graph.addEdge(0, 0, 7);

        Kahn.Result result = Kahn.run(graph, new Metrics());

        assertTrue(result.cyclic());
        assertEquals(0, result.order().length);
    }

    @Test
    void handlesParallelEdgesAndIsolatedVertex() {
        Graph graph = new Graph(3, true);

        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 1, 5);

        Metrics metrics = new Metrics();
        Kahn.Result result = Kahn.run(graph, metrics);

        assertFalse(result.cyclic());
        assertTrue(Kahn.isValidOrder(graph, result.order()));
        assertEquals(2, metrics.edgeScans);
    }

    @Test
    void handlesEmptyGraph() {
        Graph graph = new Graph(0, true);
        Metrics metrics = new Metrics();

        Kahn.Result result = Kahn.run(graph, metrics);

        assertFalse(result.cyclic());
        assertEquals(0, result.order().length);
        assertTrue(Kahn.isValidOrder(graph, result.order()));
        assertEquals(0, metrics.edgeScans);
    }

    @Test
    void rejectsInvalidOrders() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);

        assertTrue(
                Kahn.isValidOrder(graph, new int[]{0, 1, 2})
        );

        assertFalse(
                Kahn.isValidOrder(graph, new int[]{1, 0, 2})
        );

        assertFalse(
                Kahn.isValidOrder(graph, new int[]{0, 0, 2})
        );

        assertFalse(
                Kahn.isValidOrder(graph, new int[]{0, 1})
        );

        assertFalse(
                Kahn.isValidOrder(graph, new int[]{0, 1, 3})
        );

        assertFalse(
                Kahn.isValidOrder(graph, new int[]{-1, 0, 1})
        );
    }

    @Test
    void rejectsUndirectedGraph() {
        Graph graph = new Graph(2, false);

        assertThrows(
                IllegalArgumentException.class,
                () -> Kahn.run(graph, new Metrics())
        );

        assertFalse(
                Kahn.isValidOrder(graph, new int[]{0, 1})
        );
    }
}
