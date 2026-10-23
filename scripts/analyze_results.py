import csv
import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CSV_FILE = ROOT / "results" / "results.csv"
OUTPUT_FILE = ROOT / "results" / "analysis.md"

ALGORITHMS = [
    "Kruskal",
    "Prim",
    "Kosaraju",
    "Kahn",
    "DagShortestPath",
]

def read_results():
    with CSV_FILE.open(encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file))

    if len(rows) != 50:
        raise ValueError("Expected 50 data rows")

    for row in rows:
        for column in (
            "n",
            "m",
            "dsu_ops",
            "edge_scans",
            "comparisons",
        ):
            row[column] = int(row[column])

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
            f"Expected five sizes: {algorithm}, {density}"
        )

    return selected


def operations(row):
    return (
        row["dsu_ops"]
        + row["edge_scans"]
        + row["comparisons"]
    )


def fitted_slope(rows, counter):
    x = [math.log(row["n"]) for row in rows]
    y = [math.log(counter(row)) for row in rows]

    mean_x = sum(x) / len(x)
    mean_y = sum(y) / len(y)

    numerator = sum(
        (a - mean_x) * (b - mean_y)
        for a, b in zip(x, y)
    )

    denominator = sum(
        (a - mean_x) ** 2
        for a in x
    )

    return numerator / denominator


def main():
    rows = read_results()

    lines = [
        "# Analysis of measured counters",
        "",
        "## Normalized counters",
        "",
        "| Algorithm | Density | Ratio | Minimum | Maximum |",
        "| --- | --- | --- | --- | --- |",
    ]

    for algorithm in ALGORITHMS:
        for density in ("sparse", "dense"):
            selected = select_rows(
                rows, algorithm, density
            )

            if algorithm == "Kruskal":
                formula = "comparisons / (m log2 m)"

                values = [
                    row["comparisons"]
                    / (row["m"] * math.log2(row["m"]))
                    for row in selected
                ]
            else:
                formula = "edge_scans / (n + m)"

                values = [
                    row["edge_scans"]
                    / (row["n"] + row["m"])
                    for row in selected
                ]

            lines.append(
                f"| {algorithm} | {density} | {formula} "
                f"| {min(values):.4f} | {max(values):.4f} |"
            )

    lines.extend([
        "",
        "## Kruskal DSU calls per edge",
        "",
        "| Density | Minimum dsu_ops / m | Maximum dsu_ops / m |",
        "| --- | --- | --- |",
    ])

    for density in ("sparse", "dense"):
        selected = select_rows(rows, "Kruskal", density)

        values = [
            row["dsu_ops"] / row["m"]
            for row in selected
        ]

        lines.append(
            f"| {density} | {min(values):.4f} "
            f"| {max(values):.4f} |"
        )

    lines.extend([
        "",
        "## Slopes on log-log axes",
        "",
        "| Algorithm | Density | Edge scan slope "
        "| Combined operation slope |",
        "| --- | --- | --- | --- |",
    ])

    for algorithm in ALGORITHMS:
        for density in ("sparse", "dense"):
            selected = select_rows(
                rows, algorithm, density
            )

            scan_slope = fitted_slope(
                selected,
                lambda row: row["edge_scans"],
            )

            operation_slope = fitted_slope(
                selected,
                operations,
            )

            lines.append(
                f"| {algorithm} | {density} "
                f"| {scan_slope:.4f} "
                f"| {operation_slope:.4f} |"
            )

    lines.extend([
        "",
        "Combined operations = dsu_ops + edge_scans + comparisons.",
        "",
        "Kruskal sorts every edge even when its scan stops early. "
        "Its sorting factor belongs to comparisons, "
        "not to the edge scan counter.",
        "",
        "DSU call counts measure invocation frequency; "
        "they do not measure parent-pointer traversals.",
        "",
    ])

    OUTPUT_FILE.write_text(
        "\n".join(lines),
        encoding="utf-8",
    )

    print("\n".join(lines))
    print(f"Saved: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()