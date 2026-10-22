package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DagShortestPath {

    private DagShortestPath() {}

    public record Result(
            long[] distances,
            int[] parent
    ) {
        public List<Integer> pathTo(int vertex) {
            if (vertex < 0 || vertex >= distances.length) {
                throw new IllegalArgumentException(
                        "Invalid vertex: " + vertex
                );
            }

            List<Integer> path = new ArrayList<>();

            for (int current = vertex;
                 current != -1;
                 current = parent[current]) {

                path.add(current);
            }

            Collections.reverse(path);

            return List.copyOf(path);
        }

        public long sumDistances() {
            long sum = 0;

            for (long distance : distances) {
                sum += distance;
            }

            return sum;
        }
    }

    public static Result run(
            Graph graph,
            int[] topologicalOrder,
            Metrics metrics
    ) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException(
                    "DAG shortest paths require a directed graph"
            );
        }

        if (!Kahn.isValidOrder(graph, topologicalOrder)) {
            throw new IllegalArgumentException(
                    "A valid topological order is required"
            );
        }

        int n = graph.vertexCount();

        long[] distances = new long[n];
        int[] parent = new int[n];

        Arrays.fill(distances, Long.MAX_VALUE);
        Arrays.fill(parent, -1);

        int[] indegrees = graph.indegrees();

        for (int vertex = 0; vertex < n; vertex++) {
            if (indegrees[vertex] == 0) {
                distances[vertex] = 0;
            }
        }

        for (int vertex : topologicalOrder) {
            for (Edge edge : graph.adjacent(vertex)) {
                metrics.edgeScans++;

                long candidate =
                        distances[vertex] + edge.w();

                if (candidate < distances[edge.v()]) {
                    distances[edge.v()] = candidate;
                    parent[edge.v()] = vertex;
                }
            }
        }

        return new Result(distances, parent);
    }
}
