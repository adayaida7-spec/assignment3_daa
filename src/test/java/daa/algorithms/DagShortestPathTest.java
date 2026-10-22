package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DagShortestPathTest {

    @Test
    void findsKnownDistancesAndRestoresPath() {
        Graph graph = new Graph(5, true);

        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, -2);
        graph.addEdge(2, 3, 4);
        graph.addEdge(1, 4, 7);
        graph.addEdge(3, 4, -3);

        int[] order = Kahn.run(
                graph, new Metrics()
        ).order();

        Metrics metrics = new Metrics();

        DagShortestPath.Result result = DagShortestPath.run(
                graph, order, metrics
        );

        assertArrayEquals(
                new long[]{0, 0, -2, 2, -1},
                result.distances()
        );

        assertEquals(
                List.of(1, 2, 3, 4),
                result.pathTo(4)
        );

        assertEquals(List.of(0), result.pathTo(0));
        assertEquals(List.of(1), result.pathTo(1));

        assertEquals(-1, result.sumDistances());
        assertEquals(graph.edgeCount(), metrics.edgeScans);
    }

    @Test
    void agreesWithBellmanFordOn100RandomDags() {
        Random random = new Random(42);

        for (int test = 0; test < 100; test++) {
            int n = 2 + random.nextInt(40);
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
                int a = random.nextInt(n);
                int b = random.nextInt(n);

                if (a == b) {
                    continue;
                }

                int earlier = Math.min(a, b);
                int later = Math.max(a, b);
                int weight = random.nextInt(41) - 20;

                graph.addEdge(
                        permutation[earlier],
                        permutation[later],
                        weight
                );
            }

            Kahn.Result topology = Kahn.run(
                    graph, new Metrics()
            );

            assertFalse(topology.cyclic());

            Metrics metrics = new Metrics();

            DagShortestPath.Result result =
                    DagShortestPath.run(
                            graph,
                            topology.order(),
                            metrics
                    );

            assertArrayEquals(
                    bellmanFord(graph),
                    result.distances(),
                    "Distances differ on random DAG " + test
            );

            assertEquals(
                    graph.edgeCount(),
                    metrics.edgeScans
            );

            assertRestoredPaths(graph, result);
        }
    }

    @Test
    void initializesEverySourceIncludingIsolatedVertices() {
        Graph graph = new Graph(6, true);

        graph.addEdge(0, 2, 5);
        graph.addEdge(1, 2, 2);
        graph.addEdge(3, 4, -7);

        int[] order = Kahn.run(
                graph, new Metrics()
        ).order();

        DagShortestPath.Result result = DagShortestPath.run(
                graph, order, new Metrics()
        );

        assertArrayEquals(
                new long[]{0, 0, 2, 0, -7, 0},
                result.distances()
        );

        assertEquals(List.of(1, 2), result.pathTo(2));
        assertEquals(List.of(3, 4), result.pathTo(4));
        assertEquals(List.of(5), result.pathTo(5));
    }

    @Test
    void usesLongForLargeDistances() {
        Graph graph = new Graph(3, true);

        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);

        DagShortestPath.Result result = DagShortestPath.run(
                graph,
                new int[]{0, 1, 2},
                new Metrics()
        );

        assertEquals(
                2L * Integer.MAX_VALUE,
                result.distances()[2]
        );

        assertEquals(
                3L * Integer.MAX_VALUE,
                result.sumDistances()
        );
    }

    @Test
    void handlesEmptyGraph() {
        Graph graph = new Graph(0, true);
        Metrics metrics = new Metrics();

        DagShortestPath.Result result = DagShortestPath.run(
                graph, new int[0], metrics
        );

        assertEquals(0, result.distances().length);
        assertEquals(0, result.parent().length);
        assertEquals(0, result.sumDistances());
        assertEquals(0, metrics.edgeScans);

        assertThrows(
                IllegalArgumentException.class,
                () -> result.pathTo(0)
        );
    }

    @Test
    void rejectsCyclesInvalidOrdersAndUndirectedGraphs() {
        Graph cyclic = new Graph(2, true);

        cyclic.addEdge(0, 1, 1);
        cyclic.addEdge(1, 0, -2);

        assertThrows(
                IllegalArgumentException.class,
                () -> DagShortestPath.run(
                        cyclic,
                        new int[]{0, 1},
                        new Metrics()
                )
        );

        Graph dag = new Graph(2, true);
        dag.addEdge(0, 1, 3);

        assertThrows(
                IllegalArgumentException.class,
                () -> DagShortestPath.run(
                        dag,
                        new int[]{1, 0},
                        new Metrics()
                )
        );

        Graph undirected = new Graph(2, false);

        assertThrows(
                IllegalArgumentException.class,
                () -> DagShortestPath.run(
                        undirected,
                        new int[]{0, 1},
                        new Metrics()
                )
        );
    }

    private static long[] bellmanFord(Graph graph) {
        int n = graph.vertexCount();

        long[] distances = new long[n];
        Arrays.fill(distances, Long.MAX_VALUE);

        int[] indegrees = graph.indegrees();

        for (int vertex = 0; vertex < n; vertex++) {
            if (indegrees[vertex] == 0) {
                distances[vertex] = 0;
            }
        }

        for (int pass = 0; pass < n - 1; pass++) {
            boolean changed = false;

            for (Edge edge : graph.edges()) {
                if (distances[edge.u()] == Long.MAX_VALUE) {
                    continue;
                }

                long candidate =
                        distances[edge.u()] + edge.w();

                if (candidate < distances[edge.v()]) {
                    distances[edge.v()] = candidate;
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        return distances;
    }

    private static void assertRestoredPaths(
            Graph graph,
            DagShortestPath.Result result
    ) {
        int[] indegrees = graph.indegrees();

        for (int vertex = 0;
             vertex < graph.vertexCount();
             vertex++) {

            List<Integer> path = result.pathTo(vertex);

            assertFalse(path.isEmpty());
            assertTrue(path.size() <= graph.vertexCount());

            assertEquals(
                    vertex,
                    path.get(path.size() - 1).intValue()
            );

            assertEquals(0, indegrees[path.get(0)]);

            long pathWeight = 0;

            for (int i = 1; i < path.size(); i++) {
                int from = path.get(i - 1);
                int to = path.get(i);

                boolean found = false;

                for (Edge edge : graph.adjacent(from)) {
                    if (edge.v() == to
                            && result.distances()[from]
                            + edge.w()
                            == result.distances()[to]) {

                        pathWeight += edge.w();
                        found = true;
                        break;
                    }
                }

                assertTrue(
                        found,
                        "Missing shortest-path edge: "
                                + from + " -> " + to
                );
            }

            assertEquals(
                    result.distances()[vertex],
                    pathWeight
            );
        }
    }
}
