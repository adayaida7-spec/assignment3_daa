package daa.benchmark;

import daa.algorithms.DagShortestPath;
import daa.algorithms.Kahn;
import daa.algorithms.Kosaraju;
import daa.algorithms.Kruskal;
import daa.algorithms.Prim;
import daa.graph.Graph;
import daa.metrics.Metrics;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

public final class Benchmark {

    private static final int[] SIZES = {
            1_000, 3_162, 10_000, 31_623, 100_000
    };

    private static final int[] DENSITIES = {4, 20};

    private static final long[][] REFERENCES = {
            {147_970, 27, 396_274},
            {30_363, 1, 252_411},

            {468_911, 137, 1_271_911},
            {97_589, 1, 723_157},

            {1_486_139, 373, 3_941_828},
            {309_550, 1, 2_381_342},

            {4_715_515, 1_188, 12_551_891},
            {968_517, 1, 7_376_612},

            {15_012_094, 3_923, 40_096_539},
            {3_056_681, 1, 23_512_087}
    };

    private Benchmark() {}

    private record Measurement<T>(
            T result,
            Metrics metrics,
            double timeMs
    ) {}

    public static void main(String[] args) throws Exception {
        Path output = Path.of("results", "results.csv");
        Files.createDirectories(output.getParent());

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(output)
        )) {
            writer.println(
                    "algorithm,graph,n,m,time_ms,dsu_ops,"
                            + "edge_scans,comparisons,max_depth,result"
            );

            int referenceIndex = 0;

            for (int n : SIZES) {
                for (int density : DENSITIES) {
                    int m = n * density;

                    String label = density == 4
                            ? "sparse"
                            : "dense";

                    long[] expected = REFERENCES[referenceIndex++];

                    long mstWeight = benchmarkMst(
                            writer, n, m, label, expected[0]
                    );

                    int sccCount = benchmarkScc(
                            writer, n, m, label, expected[1]
                    );

                    long distanceSum = benchmarkDag(
                            writer, n, m, label, expected[2]
                    );

                    System.out.printf(
                            Locale.ROOT,
                            "%d / %d: MST=%d SCC=%d DAG=%d OK%n",
                            n, m, mstWeight, sccCount, distanceSum
                    );
                }
            }

            check(!writer.checkError(), "Failed to write CSV");
        }

        System.out.println(
                "Finished: 50 rows written to "
                        + output.toAbsolutePath()
        );
    }

    private static long benchmarkMst(
            PrintWriter writer,
            int n,
            int m,
            String label,
            long expectedWeight
    ) {
        Graph graph = GraphGenerator.undirected(n, m);

        Measurement<Kruskal.Result> kruskal = measure(
                metrics -> Kruskal.run(graph, metrics)
        );

        Measurement<Prim.Result> prim = measure(
                metrics -> Prim.run(graph, metrics)
        );

        Kruskal.Result k = kruskal.result();
        Prim.Result p = prim.result();

        check(
                k.totalWeight() == p.totalWeight(),
                "Kruskal and Prim disagree for " + n + "/" + m
        );

        check(
                k.totalWeight() == expectedWeight,
                "MST reference mismatch for " + n + "/" + m
                        + ": expected " + expectedWeight
                        + ", got " + k.totalWeight()
        );

        check(
                k.components() == 1 && p.components() == 1,
                "Generated MST graph should be connected"
        );

        check(
                k.edges().size() == n - 1
                        && p.edges().size() == n - 1,
                "Invalid MST edge count"
        );

        writeRow(
                writer,
                "Kruskal",
                "undirected_" + label,
                graph,
                kruskal,
                k.totalWeight()
        );

        writeRow(
                writer,
                "Prim",
                "undirected_" + label,
                graph,
                prim,
                p.totalWeight()
        );

        return k.totalWeight();
    }

    private static int benchmarkScc(
            PrintWriter writer,
            int n,
            int m,
            String label,
            long expectedCount
    ) {
        Graph graph = GraphGenerator.directed(n, m);

        Measurement<Kosaraju.Result> measurement = measure(
                metrics -> Kosaraju.run(graph, metrics)
        );

        int count = measurement.result().componentCount();

        check(
                count == expectedCount,
                "SCC reference mismatch for " + n + "/" + m
                        + ": expected " + expectedCount
                        + ", got " + count
        );

        check(
                measurement.metrics().edgeScans == 2L * m,
                "Kosaraju must scan exactly 2m edges"
        );

        writeRow(
                writer,
                "Kosaraju",
                "directed_" + label,
                graph,
                measurement,
                count
        );

        return count;
    }

    private static long benchmarkDag(
            PrintWriter writer,
            int n,
            int m,
            String label,
            long expectedSum
    ) {
        Graph graph = GraphGenerator.dag(n, m);

        Measurement<Kahn.Result> kahn = measure(
                metrics -> Kahn.run(graph, metrics)
        );

        Kahn.Result topology = kahn.result();

        check(
                !topology.cyclic(),
                "Generated DAG contains a cycle"
        );

        check(
                Kahn.isValidOrder(graph, topology.order()),
                "Invalid topological order"
        );

        check(
                kahn.metrics().edgeScans == m,
                "Kahn must scan exactly m edges on a DAG"
        );

        Measurement<DagShortestPath.Result> paths = measure(
                metrics -> DagShortestPath.run(
                        graph,
                        topology.order(),
                        metrics
                )
        );

        long sum = paths.result().sumDistances();

        check(
                sum == expectedSum,
                "DAG reference mismatch for " + n + "/" + m
                        + ": expected " + expectedSum
                        + ", got " + sum
        );

        check(
                paths.metrics().edgeScans == m,
                "DAG relaxation must scan exactly m edges"
        );

        writeRow(
                writer,
                "Kahn",
                "dag_" + label,
                graph,
                kahn,
                topology.order().length
        );

        writeRow(
                writer,
                "DagShortestPath",
                "dag_" + label,
                graph,
                paths,
                sum
        );

        return sum;
    }

    private static <T> Measurement<T> measure(
            Function<Metrics, T> algorithm
    ) {

        T lastResult = null;
        Metrics firstMetrics = null;
        double[] times = new double[5];

        for (int run = 0; run < 5; run++) {
            Metrics metrics = new Metrics();

            long start = System.nanoTime();
            T result = algorithm.apply(metrics);
            long elapsed = System.nanoTime() - start;

            times[run] = elapsed / 1_000_000.0;

            if (firstMetrics == null) {
                firstMetrics = metrics;
            } else {
                checkSameCounters(firstMetrics, metrics);
            }

            lastResult = result;
        }

        Arrays.sort(times);

        return new Measurement<>(
                lastResult,
                firstMetrics,
                times[2]
        );
    }

    private static void checkSameCounters(
            Metrics first,
            Metrics next
    ) {
        check(
                first.findCalls == next.findCalls
                        && first.successfulUnions
                        == next.successfulUnions
                        && first.edgeScans == next.edgeScans
                        && first.comparisons == next.comparisons
                        && first.maxDepth == next.maxDepth,
                "Operation counters changed between repeated runs"
        );
    }

    private static void writeRow(
            PrintWriter writer,
            String algorithm,
            String graphName,
            Graph graph,
            Measurement<?> measurement,
            long result
    ) {
        Metrics metrics = measurement.metrics();

        writer.printf(
                Locale.ROOT,
                "%s,%s,%d,%d,%.6f,%d,%d,%d,%d,%d%n",
                algorithm,
                graphName,
                graph.vertexCount(),
                graph.edgeCount(),
                measurement.timeMs(),
                metrics.dsuOps(),
                metrics.edgeScans,
                metrics.comparisons,
                metrics.maxDepth,
                result
        );

        writer.flush();
    }

    private static void check(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}