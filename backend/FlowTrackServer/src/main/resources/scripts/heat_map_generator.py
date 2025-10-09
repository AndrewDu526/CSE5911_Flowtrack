#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
traffic_heatmap.py
Command-line tool to generate room dwell-time heatmap from bundle data.

Usage:
    python traffic_heatmap.py <map_json> <bundle_json> <output_dir>
Example:
    python traffic_heatmap.py test_map_001.json test_output_bundles.json ./generated
"""

import json
import os
import sys
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
from shapely.geometry import Polygon, Point
from shapely.ops import unary_union
from collections import OrderedDict


# =====================================================
# 1. Utility
# =====================================================

def load_json(path: str):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


# =====================================================
# 2. Core Logic
# =====================================================

def load_map(path: str):
    data = load_json(path)
    rooms = OrderedDict()
    for r in data["rooms"]:
        poly = Polygon([(v["x"], v["y"]) for v in r["vertices"]]).buffer(0)
        rooms[r["id"]] = {"poly": poly, "name": r.get("name", r["id"])}
    print(f"Loaded {len(rooms)} rooms from map.")
    return rooms


def make_disjoint(rooms: dict):
    order = sorted(rooms.keys(), key=lambda rid: rooms[rid]["poly"].area)
    occupied = None
    out = OrderedDict()
    for rid in order:
        poly = rooms[rid]["poly"].buffer(0)
        clean = poly if occupied is None else poly.difference(occupied).buffer(0)
        out[rid] = {"poly": clean, "name": rooms[rid]["name"]}
        occupied = clean if occupied is None else occupied.union(clean).buffer(0)
    print("Disjoint geometry built (no overlaps).")
    return out


def load_cleaned_segments(bundle: dict):
    segs = []
    for s in bundle.get("stay_segments", []):
        segs.append({
            "room_id": s["room_id"],
            "t_start": s["start_t"],
            "t_end": s["end_t"],
            "duration_ms": int(round(float(s["duration_s"]) * 1000.0)),
            "weight": 1.0
        })
    print(f"Loaded {len(segs)} stay segments from bundle.")
    return segs


def aggregate_heat(segs, rooms):
    heat = {rid: 0.0 for rid in rooms.keys()}
    visits = {rid: 0 for rid in rooms.keys()}
    last = None
    for s in segs:
        rid = s["room_id"]
        if rid not in heat:
            continue
        heat[rid] += s["duration_ms"] * s["weight"]
        if last != rid:
            visits[rid] += 1
        last = rid
    print(f"Aggregated dwell time for {len(heat)} rooms.")
    return heat, visits


def normalize_and_colorize(heat, alpha=0.8, cmap_name="Reds"):
    vals = np.array(list(heat.values()))
    if vals.size == 0:
        return {}, None, None
    vmin, vmax = np.percentile(vals, 5), np.percentile(vals, 95)
    if vmin == vmax:
        vmin, vmax = float(vals.min()), float(vals.max() + 1.0)
    norm = mcolors.Normalize(vmin=vmin, vmax=vmax)
    cmap = plt.colormaps.get(cmap_name)
    rgba = {}
    for rid, v in heat.items():
        c = list(cmap(norm(v)))
        c[3] = alpha
        rgba[rid] = tuple(c)
    return rgba, norm, cmap


def render_heatmap(rooms, rgba_colors, out_path, heat, norm, cmap):
    union = unary_union([r["poly"] for r in rooms.values() if not r["poly"].is_empty])
    minx, miny, maxx, maxy = union.bounds
    dx, dy = maxx - minx, maxy - miny
    pad = 0.08

    fig, ax = plt.subplots(figsize=(12, 8))
    plt.subplots_adjust(top=0.82, right=0.85)
    ax.set_xlim(minx - pad * dx, maxx + pad * dx)
    ax.set_ylim(miny - pad * dy, maxy + pad * dy)
    ax.set_aspect("equal")
    ax.axis("off")

    # draw polygon
    for i, (rid, r) in enumerate(rooms.items()):
        poly = r["poly"]
        if poly.is_empty:
            continue
        coords = np.asarray(poly.exterior.coords)
        ax.add_patch(plt.Polygon(coords,
                                 facecolor=rgba_colors.get(rid, (0.9, 0.9, 0.9, 0.4)),
                                 edgecolor="black",
                                 linewidth=1.2,
                                 zorder=1+i))
        cpt = poly.representative_point()
        ax.text(cpt.x, cpt.y, rid, ha="center", va="center",
                fontsize=10, color="black")

    # color annotation 
    sm = plt.cm.ScalarMappable(norm=norm, cmap=cmap)
    cbar = fig.colorbar(sm, ax=ax, fraction=0.035, pad=0.05)
    cbar.set_label("Dwell time (ms)", rotation=270, labelpad=15)

    # title and abstract
    fig.text(0.5, 0.97, "Room Dwell Summary", ha="center", va="top",
             fontsize=14, fontweight="bold", color="black")

    x_text = 0.86
    y_text = 0.94
    for rid in rooms.keys():
        v_ms = heat.get(rid, 0.0)
        txt = f"{rid}: {v_ms/1000.0:.1f}s"
        fig.text(x_text, y_text, txt, ha="left", va="top",
                 fontsize=12, color="black")
        y_text -= 0.035

    fig.savefig(out_path, dpi=200, transparent=True,
                bbox_inches="tight", pad_inches=0.2)
    plt.close(fig)
    print(f"Saved heatmap to {out_path}")


# =====================================================
# 3. Main Entry
# =====================================================

def main():
    if len(sys.argv) != 4:
        print("Usage: python traffic_heatmap.py <map_json> <bundle_json> <output_dir>")
        sys.exit(1)

    map_path, bundle_path, output_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(output_dir, exist_ok=True)

    print(f"Loading map: {map_path}")
    print(f"Loading bundle: {bundle_path}")

    map_data = load_json(map_path)
    bundle = load_json(bundle_path)

    rooms_raw = load_map(map_path)
    rooms = make_disjoint(rooms_raw)
    segs = load_cleaned_segments(bundle)
    heat, _ = aggregate_heat(segs, rooms)
    rgba, norm, cmap = normalize_and_colorize(heat, alpha=0.8, cmap_name="Reds")

    bundle_name = os.path.splitext(os.path.basename(bundle_path))[0]
    out_file = os.path.join(output_dir, f"{bundle_name}_heatmap.png")

    render_heatmap(rooms, rgba, out_file, heat, norm, cmap)
    print("All heatmap generation completed successfully.")


if __name__ == "__main__":
    main()
