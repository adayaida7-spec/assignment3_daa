package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;

import java.util.ArrayDeque;
import java.util.Arrays;

public final class Kahn {

    private Kahn() {}

    public record Result(
            boolean cyclic,
            int[] order
    ) {}

    public static Result run(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException(
                    "Kahn requires a directed graph"
            );
        }

        int n = graph.vertexCount();
        int[] indegrees = graph.indegrees();
        int[] order = new int[n];

        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int vertex = 0; vertex < n; vertex++) {
            if (indegrees[vertex] == 0) {
                queue.addLast(vertex);
            }
        }

        int processed = 0;

        while (!queue.isEmpty()) {
            int vertex = queue.removeFirst();
            order[processed++] = vertex;

            for (Edge edge : graph.adjacent(vertex)) {
                metrics.edgeScans++;

                int neighbor = edge.v();
                indegrees[neighbor]--;

                if (indegrees[neighbor] == 0) {
                    queue.addLast(neighbor);
                }
            }
        }

        if (processed != n) {
            return new Result(true, new int[0]);
        }

        return new Result(false, order);
    }

    public static boolean isValidOrder(
            Graph graph,
            int[] order
    ) {
        if (!graph.isDirected()
                || order.length != graph.vertexCount()) {
            return false;
        }

        int n = graph.vertexCount();
        int[] position = new int[n];
        Arrays.fill(position, -1);

        for (int i = 0; i < n; i++) {
            int vertex = order[i];

            if (vertex < 0 || vertex >= n) {
                return false;
            }

            if (position[vertex] != -1) {
                return false;
            }

            position[vertex] = i;
        }

        for (Edge edge : graph.edges()) {
            if (position[edge.u()] >= position[edge.v()]) {
                return false;
            }
        }

        return true;
    }
}
