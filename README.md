# DAA Assignment 3 — Graph Algorithms

This project implements graph algorithms in Java.

## Algorithms

- DSU: keeps track of groups of connected vertices.
- Kruskal: builds a minimum spanning tree using sorted edges.
- Prim: builds a minimum spanning tree using a priority queue.
- Kosaraju: finds strongly connected components.
- Kahn: finds a topological order or reports a cycle.
- DAG shortest paths: finds shortest paths, including negative weights.

For disconnected graphs, Kruskal and Prim return a minimum spanning
forest. Kosaraju uses loops and explicit stacks instead of recursion.

## Requirements

- Java 17 or newer
- Maven
- Python 3 and matplotlib for the plots

## Run the Tests

Open a terminal in the project folder and run:

```bash
mvn test
```

The tests check random graphs, negative weights, disconnected graphs
and other edge cases. They also check Kosaraju on 100000 vertices.

## Run the Benchmark

First compile the project and run the tests:

```bash
mvn test
```

Then run:

```bash
java -Xmx3g -cp target/classes daa.benchmark.Benchmark
```

The benchmark tests five graph sizes with two edge counts each.
It runs every algorithm five times and saves the median time.

It also checks the results against the reference values from the
assignment. If a result is wrong, the program stops with an error.

The output is saved to:

```text
results/results.csv
```

The CSV contains 50 rows of results.

## Create the Plots

Install matplotlib:

```bash
python3 -m pip install matplotlib
```

Run:

```bash
python3 scripts/plot_results.py
```

The three plots are saved in:

```text
results/plots/
```

To calculate the ratios and slopes used in the report, run:

```bash
python3 scripts/analyze_results.py
```

The analysis is saved to:

```text
results/analysis.md
```

## Project Files

- src/main/java/daa/graph — graph storage and file reading
- src/main/java/daa/algorithms — graph algorithms and DSU
- src/main/java/daa/metrics — operation counters
- src/main/java/daa/benchmark — graph generator and benchmark
- src/test/java — JUnit tests
- data — small example graphs
- scripts — Python scripts for plots and analysis
- results — CSV, analysis and plots
- REPORT.md — explanations and measured results

## Input Format

The first line contains the number of vertices, the number of edges
and the graph type. Each following line contains an edge: u v weight.

Example:

```text
3 2 directed
0 1 5
1 2 -3
```

Vertices are numbered from 0 to n - 1.

## Operation Counters

- dsu_ops: find calls plus successful unions
- edge_scans: edges examined by an algorithm
- comparisons: weight comparisons
- max_depth: maximum depth of the explicit DFS stack

Kosaraju counts edge visits in its two DFS passes.
DAG shortest paths counts relaxation scans; checking the supplied
topological order is separate from this counter.

## GitHub

https://github.com/adayaida7-spec/assignment3_daa
