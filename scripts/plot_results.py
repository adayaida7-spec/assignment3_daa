import csv
import math
from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
from matplotlib.ticker import FixedLocator, FuncFormatter


ROOT = Path(__file__).resolve().parents[1]
CSV_FILE = ROOT / "results" / "results.csv"
PLOTS_DIR = ROOT / "results" / "plots"

SIZES = [1000, 3162, 10000, 31623, 100000]

ALGORITHMS = [
    "Kruskal",
    "Prim",
    "Kosaraju",
    "Kahn",
    "DagShortestPath",
]

COLORS = {
    "Kruskal": "#1565c0",
    "Prim": "#d55e00",
    "Kosaraju": "#009e73",
    "Kahn": "#9c27b0",
    "DagShortestPath": "#555555",
}

MARKERS = {
    "Kruskal": "o",
    "Prim": "s",
    "Kosaraju": "^",
    "Kahn": "D",
    "DagShortestPath": "x",
}

DENSITIES = {
    "sparse": ("-", 4),
    "dense": ("--", 20),
}


REFERENCES = {
    (1000, 4000): (147970, 27, 396274),
    (1000, 20000): (30363, 1, 252411),
    (3162, 12648): (468911, 137, 1271911),
    (3162, 63240): (97589, 1, 723157),
    (10000, 40000): (1486139, 373, 3941828),
    (10000, 200000): (309550, 1, 2381342),
    (31623, 126492): (4715515, 1188, 12551891),
    (31623, 632460): (968517, 1, 7376612),
    (100000, 400000): (15012094, 3923, 40096539),
    (100000, 2000000): (3056681, 1, 23512087),
}

COLUMNS = [
    "algorithm",
    "graph",
    "n",
    "m",
    "time_ms",
    "dsu_ops",
    "edge_scans",
    "comparisons",
    "max_depth",
    "result",
]


def read_results():
    with CSV_FILE.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)

        if reader.fieldnames != COLUMNS:
            raise ValueError("Incorrect CSV columns")

        rows = list(reader)

    if len(rows) != 50:
        raise ValueError(f"Expected 50 rows, found {len(rows)}")

    seen = set()

    for row in rows:
        for column in COLUMNS[2:]:
            if column == "time_ms":
                row[column] = float(row[column])
            else:
                row[column] = int(row[column])

        algorithm = row["algorithm"]
        n = row["n"]
        m = row["m"]

        if algorithm not in ALGORITHMS:
            raise ValueError(f"Unknown algorithm: {algorithm}")

        if n not in SIZES:
            raise ValueError(f"Unexpected vertex count: {n}")

        if algorithm in ("Kruskal", "Prim"):
            kind = "undirected"
        elif algorithm == "Kosaraju":
            kind = "directed"
        else:
            kind = "dag"

        valid_graphs = {
            f"{kind}_{density}": factor
            for density, (_, factor) in DENSITIES.items()
        }

        if row["graph"] not in valid_graphs:
            raise ValueError(f"Incorrect graph type: {row}")

        if m != valid_graphs[row["graph"]] * n:
            raise ValueError(f"Incorrect density: {row}")

        key = (algorithm, row["graph"], n)

        if key in seen:
            raise ValueError(f"Duplicate row: {key}")

        seen.add(key)

        mst, scc, distance_sum = REFERENCES[(n, m)]

        expected_result = {
            "Kruskal": mst,
            "Prim": mst,
            "Kosaraju": scc,
            "Kahn": n,
            "DagShortestPath": distance_sum,
        }[algorithm]

        if row["result"] != expected_result:
            raise ValueError(f"Reference result mismatch: {key}")

        if (
            not math.isfinite(row["time_ms"])
            or row["time_ms"] < 0
        ):
            raise ValueError(f"Invalid time: {key}")

        for counter in (
            "dsu_ops",
            "edge_scans",
            "comparisons",
            "max_depth",
        ):
            if row[counter] < 0:
                raise ValueError(f"Negative counter: {key}")

        if algorithm == "Kruskal":
            if not n - 1 <= row["edge_scans"] <= m:
                raise ValueError(f"Invalid Kruskal scans: {key}")

            expected_dsu = 2 * row["edge_scans"] + 3 * (n - 1)

            if row["dsu_ops"] != expected_dsu:
                raise ValueError(f"Invalid DSU counter: {key}")
        else:
            multiplier = (
                2 if algorithm in ("Prim", "Kosaraju") else 1
            )

            if row["edge_scans"] != multiplier * m:
                raise ValueError(f"Incorrect edge scans: {key}")

            if row["dsu_ops"] != 0:
                raise ValueError(f"Unexpected DSU operations: {key}")

        if algorithm == "Kosaraju":
            if not 1 <= row["max_depth"] <= n:
                raise ValueError(f"Invalid stack depth: {key}")
        elif row["max_depth"] != 0:
            raise ValueError(f"Unexpected stack depth: {key}")

        if algorithm in ("Kosaraju", "Kahn", "DagShortestPath"):
            if row["comparisons"] != 0:
                raise ValueError(
                    f"Unexpected weight comparisons: {key}"
                )

    return rows


def select_rows(rows, algorithm, density):
    selected = [
        row
        for row in rows
        if row["algorithm"] == algorithm
        and row["graph"].endswith("_" + density)
    ]

    selected.sort(key=lambda row: row["n"])

    if [row["n"] for row in selected] != SIZES:
        raise ValueError(
            f"Missing or incorrect sizes: {algorithm}, {density}"
        )

    return selected


def operations(row):
    return (
        row["dsu_ops"]
        + row["edge_scans"]
        + row["comparisons"]
    )


def draw_plot(
    rows,
    algorithms,
    value_function,
    title,
    ylabel,
    filename,
    logarithmic_y=False,
):
    figure, axes = plt.subplots(
        figsize=(12, 6),
        layout="constrained",
    )

    for algorithm in algorithms:
        for density, (line_style, _) in DENSITIES.items():
            selected = select_rows(rows, algorithm, density)


            markevery = 1

            if (
                filename == "scans_per_edge.png"
                and algorithm != "Kruskal"
            ):
                markevery = (
                    0 if density == "sparse" else 1,
                    2,
                )

            axes.plot(
                [row["n"] for row in selected],
                [value_function(row) for row in selected],
                color=COLORS[algorithm],
                linestyle=line_style,
                marker=MARKERS[algorithm],
                markersize=5,
                markevery=markevery,
                label=f"{algorithm}, {density}",
            )

    axes.set_xscale("log")

    if logarithmic_y:
        axes.set_yscale("log")

    axes.xaxis.set_major_locator(FixedLocator(SIZES))
    axes.xaxis.set_major_formatter(
        FuncFormatter(
            lambda value, position: f"{int(value):,}".replace(",", " ")
        )
    )
    axes.xaxis.set_minor_formatter(plt.NullFormatter())

    axes.set_title(title)
    axes.set_xlabel("Vertices n")
    axes.set_ylabel(ylabel)

    axes.grid(True, which="both", alpha=0.25)

    if filename == "scans_per_edge.png":
        axes.set_ylim(0, 2.35)

        axes.text(
            0.02,
            0.97,
            "Overlapping curves, both densities:\n"
            "Prim = Kosaraju = 2\n"
            "Kahn = DAG relaxation = 1",
            transform=axes.transAxes,
            va="top",
            fontsize=9,
            bbox={
                "facecolor": "white",
                "edgecolor": "0.8",
                "alpha": 0.95,
            },
        )

    elif filename == "kruskal_share.png":
        axes.set_ylim(0, 1.05)

    axes.legend(
        loc="upper left",
        bbox_to_anchor=(1.02, 1),
        fontsize=8,
    )

    output = PLOTS_DIR / filename
    figure.savefig(output, dpi=180)
    plt.close(figure)

    print(f"Created: {output}")


def main():
    rows = read_results()
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    draw_plot(
        rows,
        ALGORITHMS,
        operations,
        "Counted operations vs graph size",
        "DSU operations + edge scans + comparisons",
        "operations.png",
        logarithmic_y=True,
    )

    draw_plot(
        rows,
        ALGORITHMS,
        lambda row: row["edge_scans"] / row["m"],
        "Edge scans per input edge",
        "edge_scans / m",
        "scans_per_edge.png",
    )

    draw_plot(
        rows,
        ["Kruskal"],
        lambda row: row["edge_scans"] / row["m"],
        "Share of edges examined by Kruskal",
        "edge_scans / m",
        "kruskal_share.png",
    )

    print("All 50 CSV rows validated successfully.")


if __name__ == "__main__":
    main()