package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;

import java.util.ArrayList;
import java.util.List;

public final class Kruskal {

    private Kruskal() {}

    public record Result(
            long totalWeight,
            int components,
            List<Edge> edges
    ) {}

    public static Result run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException(
                    "Kruskal requires an undirected graph"
            );
        }

        List<Edge> sortedEdges = new ArrayList<>(graph.edges());

        sortedEdges.sort((first, second) -> {
            metrics.comparisons++;

            return Integer.compare(first.w(), second.w());
        });

        DSU dsu = new DSU(graph.vertexCount(), metrics);
        List<Edge> selectedEdges = new ArrayList<>();

        long totalWeight = 0;

        for (Edge edge : sortedEdges) {
            if (dsu.components() <= 1) {
                break;
            }

            metrics.edgeScans++;

            int rootU = dsu.find(edge.u());
            int rootV = dsu.find(edge.v());

            if (rootU != rootV) {
                dsu.union(edge.u(), edge.v());

                selectedEdges.add(edge);
                totalWeight += edge.w();
            }
        }

        return new Result(
                totalWeight,
                dsu.components(),
                List.copyOf(selectedEdges)
        );
    }
}