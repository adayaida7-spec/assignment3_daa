# Analysis of measured counters

## Normalized counters

| Algorithm | Density | Ratio | Minimum | Maximum |
| --- | --- | --- | --- | --- |
| Kruskal | sparse | comparisons / (m log2 m) | 0.7447 | 0.9069 |
| Kruskal | dense | comparisons / (m log2 m) | 0.6662 | 0.9027 |
| Prim | sparse | edge_scans / (n + m) | 1.6000 | 1.6000 |
| Prim | dense | edge_scans / (n + m) | 1.9048 | 1.9048 |
| Kosaraju | sparse | edge_scans / (n + m) | 1.6000 | 1.6000 |
| Kosaraju | dense | edge_scans / (n + m) | 1.9048 | 1.9048 |
| Kahn | sparse | edge_scans / (n + m) | 0.8000 | 0.8000 |
| Kahn | dense | edge_scans / (n + m) | 0.9524 | 0.9524 |
| DagShortestPath | sparse | edge_scans / (n + m) | 0.8000 | 0.8000 |
| DagShortestPath | dense | edge_scans / (n + m) | 0.9524 | 0.9524 |

## Kruskal DSU calls per edge

| Density | Minimum dsu_ops / m | Maximum dsu_ops / m |
| --- | --- | --- |
| sparse | 2.2902 | 2.7483 |
| dense | 0.5647 | 0.8936 |

## Slopes on log-log axes

| Algorithm | Density | Edge scan slope | Combined operation slope |
| --- | --- | --- | --- |
| Kruskal | sparse | 1.0550 | 1.0520 |
| Kruskal | dense | 1.1194 | 1.0218 |
| Prim | sparse | 1.0000 | 1.1007 |
| Prim | dense | 1.0000 | 1.0850 |
| Kosaraju | sparse | 1.0000 | 1.0000 |
| Kosaraju | dense | 1.0000 | 1.0000 |
| Kahn | sparse | 1.0000 | 1.0000 |
| Kahn | dense | 1.0000 | 1.0000 |
| DagShortestPath | sparse | 1.0000 | 1.0000 |
| DagShortestPath | dense | 1.0000 | 1.0000 |

Combined operations = dsu_ops + edge_scans + comparisons.

Kruskal sorts every edge even when its scan stops early. Its sorting factor belongs to comparisons, not to the edge scan counter.

DSU call counts measure invocation frequency; they do not measure parent-pointer traversals.
