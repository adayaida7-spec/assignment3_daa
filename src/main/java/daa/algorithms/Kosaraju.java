package daa.algorithms;

import daa.graph.Graph;
import daa.graph.Graph.Edge;
import daa.metrics.Metrics;

import java.util.Arrays;
import java.util.List;

public final class Kosaraju {

    private Kosaraju() {}

    public record Result(
            int componentCount,
            int[] component
    ) {}

    public static Result run(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException(
                    "Kosaraju requires a directed graph"
            );
        }

        int n = graph.vertexCount();

        boolean[] visited = new boolean[n];
        int[] stack = new int[n];
        int[] nextNeighbor = new int[n];
        int[] finishOrder = new int[n];

        int finished = 0;

        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }

            int top = 0;
            stack[top] = start;
            visited[start] = true;

            metrics.maxDepth = Math.max(metrics.maxDepth, 1);

            while (top >= 0) {
                int vertex = stack[top];
                List<Edge> neighbors = graph.adjacent(vertex);

                if (nextNeighbor[vertex] < neighbors.size()) {
                    Edge edge = neighbors.get(
                            nextNeighbor[vertex]++
                    );

                    metrics.edgeScans++;

                    int neighbor = edge.v();

                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        stack[++top] = neighbor;

                        metrics.maxDepth = Math.max(
                                metrics.maxDepth,
                                top + 1
                        );
                    }
                } else {
                    finishOrder[finished++] = vertex;
                    top--;
                }
            }
        }

        Graph reversed = graph.reversed();

        int[] component = new int[n];
        Arrays.fill(component, -1);

        Arrays.fill(nextNeighbor, 0);

        int componentCount = 0;

        for (int i = finished - 1; i >= 0; i--) {
            int start = finishOrder[i];

            if (component[start] != -1) {
                continue;
            }

            int top = 0;
            stack[top] = start;
            component[start] = componentCount;

            metrics.maxDepth = Math.max(metrics.maxDepth, 1);

            while (top >= 0) {
                int vertex = stack[top];
                List<Edge> neighbors = reversed.adjacent(vertex);

                if (nextNeighbor[vertex] < neighbors.size()) {
                    Edge edge = neighbors.get(
                            nextNeighbor[vertex]++
                    );

                    metrics.edgeScans++;

                    int neighbor = edge.v();

                    if (component[neighbor] == -1) {
                        component[neighbor] = componentCount;
                        stack[++top] = neighbor;

                        metrics.maxDepth = Math.max(
                                metrics.maxDepth,
                                top + 1
                        );
                    }
                } else {
                    top--;
                }
            }

            componentCount++;
        }

        return new Result(componentCount, component);
    }
}