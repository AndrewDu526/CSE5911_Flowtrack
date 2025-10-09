#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
traffic_spaghetti.py
Command-line tool to generate a spaghetti diagram (room-to-room flow visualization)
directly from a bundle JSON (no precomputed matrix required).

Usage:
    python traffic_spaghetti.py <map_json> <bundle_json> <output_dir>

Example:
    python traffic_spaghetti.py test_map_001.json test_output_bundles.json ./generated
"""

import json
import os
import sys
import matplotlib.pyplot as plt
from matplotlib.patches import Polygon
from matplotlib.collections import PatchCollection
import numpy as np
import random
import matplotlib.colors as mcolors
import math


# =====================================================
# 1. Utilities
# =====================================================

def load_json(file_path: str):
    with open(file_path, "r", encoding="utf-8") as f:
        return json.load(f)


def get_room_centroid(vertices):
    xs = [v["x"] for v in vertices]
    ys = [v["y"] for v in vertices]
    return np.mean(xs), np.mean(ys)


# =====================================================
# 2. Matrix construction from bundle
# =====================================================

def build_transition_matrix_from_bundle(bundle: dict, room_order: list[str]) -> list[list[int]]:
    """Build integer transition-count matrix from bundle.
       Priority: rs_aggregated -> r_transitions -> empty matrix."""
    n = len(room_order)
    idx = {r: i for i, r in enumerate(room_order)}
    M = [[0 for _ in range(n)] for _ in range(n)]

    # 1) Prefer aggregated data if available
    if "rs_aggregated" in bundle and bundle["rs_aggregated"]:
        for t in bundle["rs_aggregated"]:
            frm, to = t["from"], t["to"]
            if frm in idx and to in idx:
                M[idx[frm]][idx[to]] += int(t.get("count_trips", 0))
        return M

    # 2) Fallback to raw transitions
    if "r_transitions" in bundle and bundle["r_transitions"]:
        for t in bundle["r_transitions"]:
            frm, to = t["from"], t["to"]
            if frm in idx and to in idx:
                M[idx[frm]][idx[to]] += 1
        return M

    # 3) No usable data
    print("Warning: bundle contains no rs_aggregated or r_transitions data.")
    return M


# =====================================================
# 3. Drawing helpers
# =====================================================

def draw_rooms(ax, rooms):
    patches = []
    centroids = {}

    for room in rooms:
        verts = [(v["x"], v["y"]) for v in room["vertices"]]
        polygon = Polygon(verts, closed=True)
        patches.append(polygon)

        cx, cy = get_room_centroid(room["vertices"])
        centroids[room["id"]] = (cx, cy)
        ax.text(cx, cy + 1, room["id"], ha="center", va="center",
                fontsize=9, color="black", weight="bold")

    p = PatchCollection(patches, alpha=0.5, facecolor="skyblue",
                        edgecolor="black", linewidth=1.2)
    ax.add_collection(p)
    return centroids


def draw_arrows(ax, centroids, traffic_matrix, room_order):
    n = len(room_order)
    colors = list(mcolors.TABLEAU_COLORS.values())
    arrow_counts = {}

    for i in range(n):
        for j in range(n):
            count = traffic_matrix[i][j]
            if count > 0:
                start = centroids[room_order[i]]
                end = centroids[room_order[j]]
                if start == end:
                    continue

                pair_key = tuple(sorted([room_order[i], room_order[j]]))
                k = arrow_counts.get(pair_key, 0)
                arrow_counts[pair_key] = k + 1

                dx, dy = end[0] - start[0], end[1] - start[1]
                dist = math.sqrt(dx ** 2 + dy ** 2)
                if dist != 0:
                    shift_mag = 0.2 * k
                    shift_x = -dy / dist * shift_mag
                    shift_y = dx / dist * shift_mag
                else:
                    shift_x = shift_y = 0

                start_shifted = (start[0] + shift_x, start[1] + shift_y)
                end_shifted = (end[0] + shift_x, end[1] + shift_y)

                color = random.choice(colors)
                ax.arrow(start_shifted[0], start_shifted[1],
                         end_shifted[0] - start_shifted[0],
                         end_shifted[1] - start_shifted[1],
                         length_includes_head=True,
                         head_width=0.25 + 0.05 * count,
                         head_length=0.4 + 0.1 * count,
                         fc=color, ec=color, alpha=1,
                         linewidth=0.8 + 0.2 * count)


# =====================================================
# 4. Core logic
# =====================================================

def generate_spaghetti(map_path, bundle_path, output_dir):
    print(f"Loading map: {map_path}")
    print(f"Loading bundle: {bundle_path}")

    map_data = load_json(map_path)
    bundle_data = load_json(bundle_path)

    rooms = map_data["rooms"]
    room_order = [r["id"] for r in rooms]

    # auto-build matrix
    traffic_matrix = build_transition_matrix_from_bundle(bundle_data, room_order)

    fig, ax = plt.subplots(figsize=(8, 6))
    ax.set_title("Spaghetti Diagram — Room-to-Room Flow", fontsize=12, weight="bold")
    ax.set_aspect("equal", adjustable="datalim")
    ax.axis("off")

    centroids = draw_rooms(ax, rooms)
    draw_arrows(ax, centroids, traffic_matrix, room_order)

    os.makedirs(output_dir, exist_ok=True)
    bundle_name = os.path.splitext(os.path.basename(bundle_path))[0]
    output_file = os.path.join(output_dir, f"{bundle_name}_spaghetti.png")

    plt.savefig(output_file, dpi=300, bbox_inches="tight")
    plt.close(fig)
    print(f"Saved spaghetti diagram to: {output_file}")
    print("All spaghetti map generation completed successfully.")


# =====================================================
# 5. Main entry
# =====================================================

def main():
    if len(sys.argv) != 4:
        print("Usage: python traffic_spaghetti.py <map_json> <bundle_json> <output_dir>")
        sys.exit(1)

    map_path, bundle_path, output_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    generate_spaghetti(map_path, bundle_path, output_dir)


if __name__ == "__main__":
    main()
