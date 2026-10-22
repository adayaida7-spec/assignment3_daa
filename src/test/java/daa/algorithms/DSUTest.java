package daa.algorithms;

import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DSUTest {

    @Test
    void initiallyEveryElementIsSeparate() {
        Metrics metrics = new Metrics();
        DSU dsu = new DSU(5, metrics);

        assertEquals(5, dsu.components());

        for (int i = 0; i < 5; i++) {
            assertEquals(i, dsu.find(i));
        }

        assertEquals(5, metrics.findCalls);
        assertEquals(0, metrics.successfulUnions);
    }

    @Test
    void unionConnectsElementsAndCountsSuccessfulMerges() {
        Metrics metrics = new Metrics();
        DSU dsu = new DSU(4, metrics);

        assertTrue(dsu.union(0, 1));
        assertTrue(dsu.union(2, 3));
        assertTrue(dsu.union(1, 3));

        assertEquals(1, dsu.components());
        assertEquals(3, metrics.successfulUnions);

        assertEquals(6, metrics.findCalls);
        assertEquals(9, metrics.dsuOps());

        assertFalse(dsu.union(0, 3));
        assertFalse(dsu.union(2, 2));

        assertEquals(1, dsu.components());
        assertEquals(3, metrics.successfulUnions);
        assertEquals(10, metrics.findCalls);

        assertEquals(dsu.find(0), dsu.find(3));
    }

    @Test
    void randomUnionsAgreeWithSimpleReference() {
        int n = 100;
        Random random = new Random(42);

        DSU dsu = new DSU(n, new Metrics());
        int[] labels = new int[n];

        for (int i = 0; i < n; i++) {
            labels[i] = i;
        }

        for (int step = 0; step < 1000; step++) {
            int x = random.nextInt(n);
            int y = random.nextInt(n);

            int oldLabel = labels[y];
            int newLabel = labels[x];

            boolean expectedMerge = oldLabel != newLabel;

            assertEquals(expectedMerge, dsu.union(x, y));

            if (expectedMerge) {
                for (int i = 0; i < n; i++) {
                    if (labels[i] == oldLabel) {
                        labels[i] = newLabel;
                    }
                }
            }

            long expectedComponents = Arrays.stream(labels)
                    .distinct()
                    .count();

            assertEquals(expectedComponents, dsu.components());

            for (int i = 0; i < n; i++) {
                assertEquals(
                        labels[x] == labels[i],
                        dsu.find(x) == dsu.find(i)
                );
            }
        }
    }

    @Test
    void makeSetResetsThePartition() {
        DSU dsu = new DSU(3, new Metrics());

        dsu.union(0, 1);
        assertEquals(2, dsu.components());

        dsu.makeSet(3);

        assertEquals(3, dsu.components());
        assertNotEquals(dsu.find(0), dsu.find(1));
    }

    @Test
    void handlesEmptySetAndInvalidElements() {
        DSU empty = new DSU(0, new Metrics());

        assertEquals(0, empty.components());

        assertThrows(
                IllegalArgumentException.class,
                () -> empty.find(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new DSU(-1, new Metrics())
        );

        DSU dsu = new DSU(3, new Metrics());

        assertThrows(
                IllegalArgumentException.class,
                () -> dsu.find(-1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> dsu.find(3)
        );
    }
}