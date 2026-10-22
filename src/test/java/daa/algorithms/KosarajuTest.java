package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KosarajuTest {

    @Test
    void findsKnownComponents() {
        Graph graph = new Graph(6, true);

        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);

        graph.addEdge(2, 3, 1);

        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);

        Metrics metrics = new Metrics();
        Kosaraju.Result result = Kosaraju.run(graph, metrics);

        assertEquals(3, result.componentCount());

        int[] component = result.component();

        assertEquals(component[0], component[1]);
        assertEquals(component[1], component[2]);

        assertEquals(component[3], component[4]);

        assertNotEquals(component[0], component[3]);
        assertNotEquals(component[0], component[5]);
        assertNotEquals(component[3], component[5]);

        assertEquals(2L * graph.edgeCount(), metrics.edgeScans);
    }

    @Test
    void agreesWithBruteForceOnRandomGraphs() {
        Random random = new Random(42);

        for (int test = 0; test < 40; test++) {
            int n = 1 + random.nextInt(200);
            Graph graph = new Graph(n, true);

            for (int i = 0; i < 4 * n; i++) {
                int u = random.nextInt(n);
                int v = random.nextInt(n);

                graph.addEdge(u, v, 1);
            }

            boolean[][] forward = reachability(graph);
            boolean[][] backward = reachability(
                    graph.reversed()
            );

            Metrics metrics = new Metrics();

            Kosaraju.Result result = Kosaraju.run(
                    graph, metrics
            );

            int[] component = result.component();

            for (int u = 0; u < n; u++) {
                assertTrue(component[u] >= 0);
                assertTrue(
                        component[u] < result.componentCount()
                );

                for (int v = 0; v < n; v++) {
                    boolean expectedSameComponent =
                            forward[u][v] && backward[u][v];

                    assertEquals(
                            expectedSameComponent,
                            component[u] == component[v],
                            "Incorrect SCC membership: "
                                    + u + ", " + v
                    );
                }
            }

            long distinctComponents = Arrays.stream(component)
                    .distinct()
                    .count();

            assertEquals(
                    distinctComponents,
                    result.componentCount()
            );

            assertEquals(
                    2L * graph.edgeCount(),
                    metrics.edgeScans
            );
        }
    }

    @Test
    void handlesPathOf100000VerticesWithoutRecursion() {
        int n = 100_000;
        Graph graph = new Graph(n, true);

        for (int vertex = 0; vertex < n - 1; vertex++) {
            graph.addEdge(vertex, vertex + 1, 1);
        }

        Metrics metrics = new Metrics();

        Kosaraju.Result result = assertDoesNotThrow(
                () -> Kosaraju.run(graph, metrics)
        );

        assertEquals(n, result.componentCount());

        assertEquals(
                (long) n,
                Arrays.stream(result.component())
                        .distinct()
                        .count()
        );

        assertEquals(n, metrics.maxDepth);

        assertEquals(
                2L * (n - 1),
                metrics.edgeScans
        );
    }

    @Test
    void handlesLoopsParallelEdgesAndIsolatedVertex() {
        Graph graph = new Graph(3, true);

        graph.addEdge(0, 0, 5);
        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 1, 7);
        graph.addEdge(1, 0, 3);

        Metrics metrics = new Metrics();

        Kosaraju.Result result = Kosaraju.run(
                graph, metrics
        );

        assertEquals(2, result.componentCount());

        assertEquals(
                result.component()[0],
                result.component()[1]
        );

        assertNotEquals(
                result.component()[0],
                result.component()[2]
        );

        assertEquals(8, metrics.edgeScans);
    }

    @Test
    void handlesEmptyGraph() {
        Graph graph = new Graph(0, true);
        Metrics metrics = new Metrics();

        Kosaraju.Result result = Kosaraju.run(
                graph, metrics
        );

        assertEquals(0, result.componentCount());
        assertEquals(0, result.component().length);
        assertEquals(0, metrics.edgeScans);
        assertEquals(0, metrics.maxDepth);
    }

    @Test
    void rejectsUndirectedGraph() {
        Graph graph = new Graph(3, false);

        assertThrows(
                IllegalArgumentException.class,
                () -> Kosaraju.run(graph, new Metrics())
        );
    }

    private static boolean[][] reachability(Graph graph) {
        int n = graph.vertexCount();
        boolean[][] reachable = new boolean[n][n];

        for (int start = 0; start < n; start++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();

            reachable[start][start] = true;
            queue.addLast(start);

            while (!queue.isEmpty()) {
                int vertex = queue.removeFirst();

                for (Edge edge : graph.adjacent(vertex)) {
                    int neighbor = edge.v();

                    if (!reachable[start][neighbor]) {
                        reachable[start][neighbor] = true;
                        queue.addLast(neighbor);
                    }
                }
            }
        }

        return reachable;
    }
}