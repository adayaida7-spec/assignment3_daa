package daa.graph;

import daa.algorithms.DagShortestPath;
import daa.algorithms.Kahn;
import daa.algorithms.Kosaraju;
import daa.algorithms.Kruskal;
import daa.algorithms.Prim;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {

    @Test
    void readsConnectedGraphAndBuildsMst() throws IOException {
        Graph graph = Graph.read(
                Path.of("data/connected.txt")
        );

        assertEquals(4, graph.vertexCount());
        assertEquals(5, graph.edgeCount());
        assertFalse(graph.isDirected());

        assertEquals(
                9,
                Kruskal.run(graph, new Metrics()).totalWeight()
        );

        assertEquals(
                9,
                Prim.run(graph, new Metrics()).totalWeight()
        );
    }

    @Test
    void readsDisconnectedGraphAndBuildsForest()
            throws IOException {

        Graph graph = Graph.read(
                Path.of("data/disconnected.txt")
        );

        assertEquals(5, graph.vertexCount());
        assertEquals(4, graph.edgeCount());

        Kruskal.Result kruskal = Kruskal.run(
                graph, new Metrics()
        );

        Prim.Result prim = Prim.run(
                graph, new Metrics()
        );

        assertEquals(-1, kruskal.totalWeight());
        assertEquals(-1, prim.totalWeight());

        assertEquals(3, kruskal.components());
        assertEquals(3, prim.components());

        assertEquals(2, kruskal.edges().size());
        assertEquals(2, prim.edges().size());

        assertTrue(graph.adjacent(4).isEmpty());
    }

    @Test
    void readsCyclicGraphAndDetectsCycle() throws IOException {
        Graph graph = Graph.read(
                Path.of("data/cyclic.txt")
        );

        assertTrue(graph.isDirected());
        assertEquals(4, graph.vertexCount());
        assertEquals(4, graph.edgeCount());

        Kosaraju.Result components = Kosaraju.run(
                graph, new Metrics()
        );

        assertEquals(2, components.componentCount());

        assertEquals(
                components.component()[0],
                components.component()[1]
        );

        assertEquals(
                components.component()[1],
                components.component()[2]
        );

        assertNotEquals(
                components.component()[0],
                components.component()[3]
        );

        assertTrue(
                Kahn.run(graph, new Metrics()).cyclic()
        );
    }

    @Test
    void readsDagAndFindsShortestPaths() throws IOException {
        Graph graph = Graph.read(
                Path.of("data/dag.txt")
        );

        Kahn.Result topology = Kahn.run(
                graph, new Metrics()
        );

        assertFalse(topology.cyclic());

        assertTrue(
                Kahn.isValidOrder(graph, topology.order())
        );

        DagShortestPath.Result paths = DagShortestPath.run(
                graph,
                topology.order(),
                new Metrics()
        );

        assertArrayEquals(
                new long[]{0, 0, -2, 2, -1},
                paths.distances()
        );

        assertEquals(
                List.of(1, 2, 3, 4),
                paths.pathTo(4)
        );
    }

    @Test
    void reversesEdgesAndPreservesWeights() {
        Graph graph = new Graph(3, true);

        graph.addEdge(0, 1, -5);
        graph.addEdge(0, 1, 7);
        graph.addEdge(1, 2, 4);
        graph.addEdge(2, 2, 9);

        Graph reversed = graph.reversed();

        assertTrue(reversed.isDirected());
        assertEquals(3, reversed.vertexCount());
        assertEquals(4, reversed.edgeCount());

        assertEquals(
                List.of(
                        new Graph.Edge(1, 0, -5),
                        new Graph.Edge(1, 0, 7),
                        new Graph.Edge(2, 1, 4),
                        new Graph.Edge(2, 2, 9)
                ),
                reversed.edges()
        );

        assertEquals(
                graph.edges(),
                reversed.reversed().edges()
        );
    }

    @Test
    void storesUndirectedEdgeInBothAdjacencyLists() {
        Graph graph = new Graph(2, false);

        graph.addEdge(0, 1, 8);

        assertEquals(1, graph.edgeCount());

        assertEquals(
                List.of(new Graph.Edge(0, 1, 8)),
                graph.adjacent(0)
        );

        assertEquals(
                List.of(new Graph.Edge(1, 0, 8)),
                graph.adjacent(1)
        );
    }

    @Test
    void returnsIndependentCopyOfIndegrees() {
        Graph graph = new Graph(2, true);

        graph.addEdge(0, 1, 5);

        int[] copy = graph.indegrees();
        copy[1] = 100;

        assertArrayEquals(
                new int[]{0, 1},
                graph.indegrees()
        );
    }

    @Test
    void rejectsInvalidVerticesAndNegativeSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Graph(-1, true)
        );

        Graph graph = new Graph(2, true);

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addEdge(-1, 0, 1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addEdge(0, 2, 1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.adjacent(2)
        );
    }
}