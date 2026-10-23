package daa.benchmark;

import daa.graph.Graph;

import java.util.Random;

public final class GraphGenerator {

    private GraphGenerator() {}

    public static Graph undirected(int n, int m) {
        validate(n, m);

        if (m < n - 1) {
            throw new IllegalArgumentException(
                    "A connected graph requires at least n - 1 edges"
            );
        }

        Random random = new Random(42);
        int[] permutation = shuffledVertices(n, random);

        Graph graph = new Graph(n, false);

        for (int i = 1; i < n; i++) {
            int parent = permutation[random.nextInt(i)];
            int vertex = permutation[i];
            int weight = 1 + random.nextInt(1000);

            graph.addEdge(parent, vertex, weight);
        }

        while (graph.edgeCount() < m) {
            int a = random.nextInt(n);
            int b = differentVertex(n, a, random);
            int weight = 1 + random.nextInt(1000);

            graph.addEdge(a, b, weight);
        }

        return graph;
    }

    public static Graph directed(int n, int m) {
        validate(n, m);

        Random random = new Random(42);
        Graph graph = new Graph(n, true);

        for (int i = 0; i < m; i++) {
            int a = random.nextInt(n);
            int b = differentVertex(n, a, random);
            int weight = 1 + random.nextInt(1000);

            graph.addEdge(a, b, weight);
        }

        return graph;
    }

    public static Graph dag(int n, int m) {
        validate(n, m);

        Random random = new Random(42);
        int[] rank = shuffledVertices(n, random);

        Graph graph = new Graph(n, true);

        for (int i = 0; i < m; i++) {
            int a = random.nextInt(n);
            int b = differentVertex(n, a, random);

            if (rank[a] > rank[b]) {
                int temporary = a;
                a = b;
                b = temporary;
            }

            int weight = 1 + random.nextInt(1000);

            graph.addEdge(a, b, weight);
        }

        return graph;
    }

    private static int[] shuffledVertices(
            int n,
            Random random
    ) {
        int[] vertices = new int[n];

        for (int i = 0; i < n; i++) {
            vertices[i] = i;
        }

        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);

            int temporary = vertices[i];
            vertices[i] = vertices[j];
            vertices[j] = temporary;
        }

        return vertices;
    }

    private static int differentVertex(
            int n,
            int a,
            Random random
    ) {
        int b = random.nextInt(n);

        while (b == a) {
            b = random.nextInt(n);
        }

        return b;
    }

    private static void validate(int n, int m) {
        if (n < 0 || m < 0) {
            throw new IllegalArgumentException(
                    "Vertex and edge counts cannot be negative"
            );
        }

        if (n < 2 && m > 0) {
            throw new IllegalArgumentException(
                    "At least two vertices are required for non-loop edges"
            );
        }
    }
}