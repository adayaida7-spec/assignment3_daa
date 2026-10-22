package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {
    private Prim() {}

    public record Result(
            long totalWeight,
            int components,
            List<Edge> edges
    ) {}

    public static Result run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException(
                    "Prim requires an undirected graph"
            );
        }

        boolean[] visited = new boolean[graph.vertexCount()];
        List<Edge> selectedEdges = new ArrayList<>();

        PriorityQueue<Edge> queue = new PriorityQueue<>(
                (first, second) -> {
                    metrics.comparisons++;
                    return Integer.compare(first.w(), second.w());
                }
        );
        long totalWeight = 0;
        int components = 0;
        for (int root = 0; root < graph.vertexCount(); root++) {
            if (visited[root]) {
                continue;
            }

            components++;
            visited[root] = true;

            addCandidates(graph, root, visited, queue, metrics);

            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                int vertex = edge.v();
                if (visited[vertex]) {
                    continue;
                }

                visited[vertex] = true;
                selectedEdges.add(edge);
                totalWeight += edge.w();

                addCandidates(
                        graph, vertex, visited, queue, metrics
                );
            }
        }
        return new Result(
                totalWeight,
                components,
                List.copyOf(selectedEdges)
        );
    }
    private static void addCandidates(
            Graph graph,
            int vertex,
            boolean[] visited,
            PriorityQueue<Edge> queue,
            Metrics metrics
    ) {
        for (Edge edge : graph.adjacent(vertex)) {
            metrics.edgeScans++;

            if (!visited[edge.v()]) {
                queue.add(edge);
            }
        }
    }
}
