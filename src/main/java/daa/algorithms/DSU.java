package daa.algorithms;

import daa.metrics.Metrics;

public final class DSU {

    private int[] parent;
    private int[] size;
    private int componentCount;

    private final Metrics metrics;

    public DSU(int n, Metrics metrics) {
        this.metrics = metrics;
        makeSet(n);
    }

    public void makeSet(int n) {
        if (n < 0) {
            throw new IllegalArgumentException(
                    "Number of elements cannot be negative"
            );
        }
        parent = new int[n];
        size = new int[n];
        componentCount = n;

        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }
    public int find(int x) {
        checkElement(x);
        metrics.findCalls++;

        int root = x;

        while (root != parent[root]) {
            root = parent[root];
        }
        while (x != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }

        return root;
    }

    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX == rootY) {
            return false;
        }
        if (size[rootX] < size[rootY]) {
            int temporary = rootX;
            rootX = rootY;
            rootY = temporary;
        }

        parent[rootY] = rootX;
        size[rootX] += size[rootY];

        componentCount--;
        metrics.successfulUnions++;

        return true;
    }

    public int components() {
        return componentCount;
    }

    private void checkElement(int x) {
        if (x < 0 || x >= parent.length) {
            throw new IllegalArgumentException(
                    "Invalid element: " + x
            );
        }
    }
}
