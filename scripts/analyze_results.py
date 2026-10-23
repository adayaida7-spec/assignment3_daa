import math

from plot_results import (
    ROOT,
    ALGORITHMS,
    read_results,
    select_rows,
    operations,
)


OUTPUT_FILE = ROOT / "results" / "analysis.md"


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
            selected = select_rows(rows, algorithm, density)

            if algorithm in ("Kruskal", "Prim"):
                formula = "comparisons / (m log2 m)"

                values = [
                    row["comparisons"]
                    / (row["m"] * math.log2(row["m"]))
                    for row in selected
                ]
            else:
                formula = "edge_scans / (n + m)"

                values = [
                    row["edge_scans"] / (row["n"] + row["m"])
                    for row in selected
                ]

            lines.append(
                f"| {algorithm} | {density} | {formula} "
                f"| {min(values):.4f} | {max(values):.4f} |"
            )

    lines.extend([
        "",
        "Prim comparisons include heap work. Its scan ratios alone "
        "describe only the linear adjacency traversal.",
        "",
        "## Kruskal DSU calls per input edge",
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
            f"| {density} "
            f"| {min(values):.4f} | {max(values):.4f} |"
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
            selected = select_rows(rows, algorithm, density)

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
        "Prim and Kosaraju scan 2m adjacency entries. "
        "Kahn and DAG relaxation scan m edges each.",
        "",
        "Kruskal sorts every edge even when its scan stops early. "
        "The sorting logarithm belongs to comparisons, not edge_scans. "
        "Its edge scan slope can differ from 1 because the examined "
        "fraction changes with graph size.",
        "",
        "DSU counters measure find calls and successful unions, "
        "not parent-pointer traversals. They do not independently "
        "verify the inverse-Ackermann bound.",
        "",
        "These finite measurements support the theoretical analysis; "
        "they do not prove asymptotic complexity.",
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