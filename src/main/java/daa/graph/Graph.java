package daa.graph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public final class Graph {

    public record Edge(int u, int v, int w) {}

    private final int n;
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges;
    private final int[] indegrees;

    public Graph(int n, boolean directed) {
        if (n < 0) {
            throw new IllegalArgumentException(
                    "Number of vertices cannot be negative"
            );
        }

        this.n = n;
        this.directed = directed;
        this.adjacency = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.indegrees = new int[n];

        for (int i = 0; i < n; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public int vertexCount() {
        return n;
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean isDirected() {
        return directed;
    }

    public void addEdge(int u, int v, int w) {
        checkVertex(u);
        checkVertex(v);

        Edge edge = new Edge(u, v, w);

        edges.add(edge);
        adjacency.get(u).add(edge);
        indegrees[v]++;

        if (!directed) {
            adjacency.get(v).add(new Edge(v, u, w));
            indegrees[u]++;
        }
    }

    public List<Edge> adjacent(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public int[] indegrees() {
        return indegrees.clone();
    }

    public Graph reversed() {
        if (!directed) {
            throw new IllegalArgumentException(
                    "Reversing requires a directed graph"
            );
        }

        Graph reversed = new Graph(n, true);

        for (Edge edge : edges) {
            reversed.addEdge(edge.v(), edge.u(), edge.w());
        }

        return reversed;
    }

    public static Graph read(Path path) throws IOException {
        try (Scanner scanner = new Scanner(path)) {
            int n = scanner.nextInt();
            int m = scanner.nextInt();
            String type = scanner.next();

            if (m < 0) {
                throw new IllegalArgumentException(
                        "Number of edges cannot be negative"
                );
            }

            if (!type.equals("directed")
                    && !type.equals("undirected")) {
                throw new IllegalArgumentException(
                        "Expected directed or undirected"
                );
            }

            Graph graph = new Graph(n, type.equals("directed"));

            for (int i = 0; i < m; i++) {
                int u = scanner.nextInt();
                int v = scanner.nextInt();
                int w = scanner.nextInt();

                graph.addEdge(u, v, w);
            }

            if (scanner.hasNext()) {
                throw new IllegalArgumentException(
                        "Unexpected data after the last edge"
                );
            }

            return graph;
        }
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= n) {
            throw new IllegalArgumentException(
                    "Invalid vertex: " + vertex
            );
        }
    }
}
