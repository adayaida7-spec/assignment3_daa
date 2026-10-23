import csv
from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parents[1]
CSV_FILE = ROOT / "results" / "results.csv"
PLOTS_DIR = ROOT / "results" / "plots"

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

DENSITIES = {
    "sparse": "-",
    "dense": "--",
}


def read_results():
    with CSV_FILE.open(encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file))

    if len(rows) != 50:
        raise ValueError(
            f"Expected 50 data rows, found {len(rows)}"
        )

    integer_columns = [
        "n",
        "m",
        "dsu_ops",
        "edge_scans",
        "comparisons",
        "max_depth",
        "result",
    ]

    for row in rows:
        for column in integer_columns:
            row[column] = int(row[column])

        row["time_ms"] = float(row["time_ms"])

    return rows


def select_rows(rows, algorithm, density):
    selected = [
        row
        for row in rows
        if row["algorithm"] == algorithm
        and row["graph"].endswith("_" + density)
    ]

    selected.sort(key=lambda row: row["n"])

    if len(selected) != 5:
        raise ValueError(
            f"Expected five sizes for {algorithm}, {density}"
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
        figsize=(11, 6),
        layout="constrained",
    )

    for algorithm in algorithms:
        for density, line_style in DENSITIES.items():
            selected = select_rows(
                rows, algorithm, density
            )

            x_values = [
                row["n"] for row in selected
            ]

            y_values = [
                value_function(row) for row in selected
            ]

            axes.plot(
                x_values,
                y_values,
                linestyle=line_style,
                marker="o",
                color=COLORS[algorithm],
                label=f"{algorithm}, {density}",
            )

    axes.set_xscale("log")

    if logarithmic_y:
        axes.set_yscale("log")

    axes.set_title(title)
    axes.set_xlabel("Vertices n")
    axes.set_ylabel(ylabel)

    axes.grid(
        True,
        which="both",
        alpha=0.25,
    )

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

    PLOTS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    draw_plot(
        rows=rows,
        algorithms=ALGORITHMS,
        value_function=operations,
        title="Counted operations vs graph size",
        ylabel="DSU operations + edge scans + comparisons",
        filename="operations.png",
        logarithmic_y=True,
    )

    draw_plot(
        rows=rows,
        algorithms=ALGORITHMS,
        value_function=lambda row: (
            row["edge_scans"] / row["m"]
        ),
        title="Edge scans per input edge",
        ylabel="edge_scans / m",
        filename="scans_per_edge.png",
    )

    draw_plot(
        rows=rows,
        algorithms=["Kruskal"],
        value_function=lambda row: (
            row["edge_scans"] / row["m"]
        ),
        title="Share of edges examined by Kruskal",
        ylabel="edge_scans / m",
        filename="kruskal_share.png",
    )


if __name__ == "__main__":
    main()