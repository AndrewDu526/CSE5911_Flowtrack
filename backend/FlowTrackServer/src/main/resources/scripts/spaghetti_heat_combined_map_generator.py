#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
combined_generator.py
Command-line tool for generating the combined FlowTrack visualization:
room heatmap (dwell time) + spaghetti arrows (travel flow).

Usage:
    python combined_generator.py <map_json> <bundle_json> <output_dir>

Example:
    python combined_generator.py test_map_001.json test_output_bundles.json ./generated

The output is a single PNG visualization combining room dwell-time heat and
room-to-room flow arrows, saved in <output_dir>.
"""

import os
import sys
import json
import math
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
from collections import OrderedDict
from matplotlib.patches import FancyArrowPatch
from matplotlib.path import Path
from shapely.geometry import Polygon
from shapely.ops import unary_union
from datetime import datetime


# =====================================================
# 1. Utility
# =====================================================

def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def ensure_dir(p):
    os.makedirs(p, exist_ok=True)

def load_map(path):
    data = load_json(path)
    rooms = OrderedDict()
    for r in data["rooms"]:
        poly = Polygon([(v["x"], v["y"]) for v in r["vertices"]])
        rooms[r["id"]] = {"poly": poly, "name": r.get("name", r["id"])}
    return rooms


# =====================================================
# 2. Heat Aggregation
# =====================================================

def aggregate_heat(segs, rooms):
    heat = {rid: 0.0 for rid in rooms.keys()}
    for s in segs:
        rid = s["room_id"]
        if rid in heat:
            heat[rid] += s.get("duration_ms", 0.0)
    return heat

def normalize_and_colorize(heat, alpha=0.95):
    vals = np.array(list(heat.values()))
    if len(vals) == 0:
        return {}, None, None
    vmin, vmax = np.percentile(vals, 5), np.percentile(vals, 95)
    norm = mcolors.Normalize(vmin=vmin, vmax=vmax)
    cmap = mcolors.LinearSegmentedColormap.from_list("strongReds", [
        (0, (0.9, 0.75, 0.75)),
        (0.5, (0.85, 0.3, 0.3)),
        (1, (0.4, 0.05, 0.05))
    ])
    rgba = {rid: tuple(list(cmap(norm(v)))[:3] + [alpha]) for rid, v in heat.items()}
    return rgba, norm, cmap


# =====================================================
# 3. Transition Matrix
# =====================================================

def build_transition_matrix(bundle, room_order):
    n = len(room_order)
    idx = {r: i for i, r in enumerate(room_order)}
    M = [[0.0 for _ in range(n)] for _ in range(n)]
    for t in bundle.get("rs_aggregated", []):
        frm, to = t["from"], t["to"]
        if frm in idx and to in idx:
            M[idx[frm]][idx[to]] += float(t.get("total_travel_s", 0.0))
    return M


# =====================================================
# 4. Visualization
# =====================================================

def curved_arrow(ax, start, end, curvature=0.25, color="#2B5DA5",
                 lw=1.0, alpha=0.9, avoid_radius=0, start_radius=0):
    sx, sy = start
    ex, ey = end
    dx, dy = ex - sx, ey - sy
    dist = math.hypot(dx, dy)
    if dist < 1e-6:
        return
    ux, uy = dx / dist, dy / dist
    sx_adj = sx + ux * start_radius * 1.2
    sy_adj = sy + uy * start_radius * 1.2
    ex_adj = ex - ux * avoid_radius * 1.2
    ey_adj = ey - uy * avoid_radius * 1.2
    mx, my = (sx_adj + ex_adj) / 2, (sy_adj + ey_adj) / 2
    ctrl = (mx + curvature * (ey_adj - sy_adj),
            my - curvature * (ex_adj - sx_adj))
    path = Path([(sx_adj, sy_adj), ctrl, (ex_adj, ey_adj)],
                [Path.MOVETO, Path.CURVE3, Path.CURVE3])
    patch = FancyArrowPatch(path=path, arrowstyle='-|>', mutation_scale=10 + 3 * lw,
                            lw=lw, color=color, alpha=alpha, zorder=120)
    ax.add_patch(patch)

def draw_heat(ax, rooms, heat, norm, cmap):
    radii = {}
    all_bounds = [r["poly"].bounds for r in rooms.values()]
    max_dim = max(max(b[2]-b[0], b[3]-b[1]) for b in all_bounds)
    base_radius = 0.05 * max_dim

    for rid, r in rooms.items():
        val = heat.get(rid, 0)
        if val <= 0:
            continue
        minx, miny, maxx, maxy = r["poly"].bounds
        cx, cy = r["poly"].centroid.coords[0]
        radius = base_radius
        radii[rid] = radius
        circ = plt.Circle((cx, cy), radius, color=cmap(norm(val)), alpha=0.95, lw=0)
        ax.add_patch(circ)
        text_x = minx + 0.02 * (maxx - minx)
        text_y = maxy - 0.02 * (maxy - miny)
        ax.text(text_x, text_y, rid, fontsize=11, fontweight='bold',
                ha='left', va='top', color='black', zorder=300)

    return radii

def draw_spaghetti(ax, rooms, matrix, room_order, heat_radii):
    centroids = {r: rooms[r]["poly"].centroid.coords[0] for r in room_order}
    values = [matrix[i][j] for i in range(len(room_order))
              for j in range(len(room_order)) if matrix[i][j] > 0 and i != j]
    if not values:
        return
    fmin, fmax = min(values), max(values)

    for i, frm in enumerate(room_order):
        for j, to in enumerate(room_order):
            v = matrix[i][j]
            if v <= 0 or frm == to or v < 2.0:
                continue

            lw = 0.4 + 5 * (v - fmin) / (fmax - fmin)
            curved_arrow(ax, centroids[frm], centroids[to],
                        lw=lw, color="#2B5DA5",
                        avoid_radius=heat_radii.get(to, 0.0) + 0.2,
                        start_radius=heat_radii.get(frm, 0.0) + 0.2,
                        curvature=0.35, alpha=0.9)

def render(rooms, heat, norm, cmap, matrix, room_order, out_path):
    from matplotlib.colorbar import ColorbarBase

    union = unary_union([r["poly"] for r in rooms.values()])
    minx, miny, maxx, maxy = union.bounds
    dx, dy = maxx - minx, maxy - miny

    plt.rcParams.update({
        "font.family": "DejaVu Sans",
        "axes.titlesize": 17,
        "axes.titleweight": "bold"
    })

    # === 主图部分 ===
    fig, ax = plt.subplots(figsize=(12, 7))
    ax.set_aspect("equal")
    ax.axis("off")

    # 居中地图
    x_center = (minx + maxx) / 2
    y_center = (miny + maxy) / 2
    pad_x, pad_y = 0.08 * dx, 0.08 * dy
    ax.set_xlim(x_center - dx / 2 - pad_x, x_center + dx / 2 + pad_x)
    ax.set_ylim(y_center - dy / 2 - pad_y, y_center + dy / 2 + pad_y)

    # 绘制房间与路线
    for r in rooms.values():
        coords = np.array(r["poly"].exterior.coords)
        ax.add_patch(plt.Polygon(coords, facecolor=(0.96, 0.96, 0.96, 1),
                                 edgecolor="black", lw=0.8))
    heat_radii = draw_heat(ax, rooms, heat, norm, cmap)
    draw_spaghetti(ax, rooms, matrix, room_order, heat_radii)

    # 调整布局并保存主图
    plt.subplots_adjust(left=0.07, right=0.93, top=0.93, bottom=0.07)

    base, _ = os.path.splitext(out_path)
    map_path = base + "_map.png"
    fig.savefig(map_path, dpi=300)
    plt.close(fig)
    print(f"Saved main map: {map_path}")

    # === colorbar 单独输出 ===
    fig_bar, ax_bar = plt.subplots(figsize=(1.5, 5))
    fig_bar.subplots_adjust(left=0.4, right=0.7, top=0.9, bottom=0.1)

    sm = plt.cm.ScalarMappable(norm=norm, cmap=cmap)
    ColorbarBase(ax_bar, cmap=cmap, norm=norm, orientation='vertical')
    ax_bar.set_ylabel("Room dwell time (ms)", fontsize=11, fontweight='bold')
    ax_bar.yaxis.set_label_position("right")

    bar_path = base + "_bar.png"
    fig_bar.savefig(bar_path, dpi=300, bbox_inches="tight", transparent=True)
    plt.close(fig_bar)
    print(f"Saved colorbar: {bar_path}")



# =====================================================
# 5. Main Entry
# =====================================================

def main():
    if len(sys.argv) != 4:
        print("Usage: python combined_generator.py <map_json> <bundle_json> <output_dir>")
        sys.exit(1)

    map_path, bundle_path, output_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    ensure_dir(output_dir)

    print(f"Loading map: {map_path}")
    print(f"Loading bundle: {bundle_path}")

    rooms = load_map(map_path)
    bundle = load_json(bundle_path)

    segs = bundle.get("stay_segments", [])
    for s in segs:
        s["duration_ms"] = float(s.get("duration_s", 0.0)) * 1000.0

    heat = aggregate_heat(segs, rooms)
    rgba, norm, cmap = normalize_and_colorize(heat)

    room_order = list(rooms.keys())
    matrix = build_transition_matrix(bundle, room_order)

    bundle_name = os.path.splitext(os.path.basename(bundle_path))[0]
    out_path = os.path.join(output_dir, f"{bundle_name}_combined.png")
    render(rooms, heat, norm, cmap, matrix, room_order, out_path)

    print("All combined visualizations generated successfully.")


if __name__ == "__main__":
    main()
