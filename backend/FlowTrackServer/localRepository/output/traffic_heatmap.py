"""
traffic_heatmap.py — unified version
Supports both raw batch and cleaned stay_segments JSON.
Generates a heatmap (no overlaps, top summary table).
"""

import json, os
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.colors as mcolors
from shapely.geometry import Polygon, Point
from shapely.ops import unary_union
from collections import defaultdict, OrderedDict

# ---------- 1) Load map ----------
def load_map(path: str):
    data = json.load(open(path, "r"))
    rooms = OrderedDict()
    for r in data["rooms"]:
        poly = Polygon([(v["x"], v["y"]) for v in r["vertices"]]).buffer(0)
        rooms[r["id"]] = {"poly": poly, "name": r.get("name", r["id"])}
    print(f"✅ Loaded {len(rooms)} rooms from map.")
    return rooms

# ---------- 1.1) Make rooms disjoint ----------
def make_disjoint(rooms: dict):
    order = sorted(rooms.keys(), key=lambda rid: rooms[rid]["poly"].area)
    occupied = None
    out = OrderedDict()
    for rid in order:
        poly = rooms[rid]["poly"].buffer(0)
        clean = poly if occupied is None else poly.difference(occupied).buffer(0)
        out[rid] = {"poly": clean, "name": rooms[rid]["name"]}
        occupied = clean if occupied is None else occupied.union(clean).buffer(0)
    print("🧩 Disjoint geometry built (no overlaps).")
    return out

# ---------- 2) Load raw points ----------
def load_points(path: str):
    d = json.load(open(path, "r"))
    pts = sorted([p for p in d["points"] if p.get("accepted", True)], key=lambda p: p["time"])
    print(f"✅ Loaded {len(pts)} accepted points.")
    return pts

# ---------- 3) Helper for point quality ----------
def q(p):
    a = p.get("anchorsUsed", 0)
    rms = p.get("rms", 5.0)
    return max(0.0, min(1.0, 0.5*(a/6.0) + 0.5*(1.0/(1.0+rms))))

# ---------- 4) Locate point in room ----------
def locate(p, rooms, last_room=None, eps=0.3):
    pt = Point(p["x"], p["y"])
    if last_room and rooms[last_room]["poly"].distance(pt) <= eps:
        return last_room
    for rid, r in rooms.items():
        poly = r["poly"]
        if poly.is_empty: 
            continue
        minx, miny, maxx, maxy = poly.bounds
        if not (minx <= pt.x <= maxx and miny <= pt.y <= maxy):
            continue
        if poly.covers(pt):
            return rid
    return None

# ---------- 5) Build stay segments ----------
def build_segments(points, rooms):
    segs = []
    if not points:
        return segs
    last_room = locate(points[0], rooms, None)
    t0, w_sum, n = points[0]["time"], 0.0, 0
    for i in range(len(points)-1):
        p, pn = points[i], points[i+1]
        r  = locate(p, rooms, last_room)
        rn = locate(pn, rooms, r)
        dt = pn["time"] - p["time"]
        if dt <= 0 or dt > 5000:
            continue
        if rn == r:
            w_sum += q(p); n += 1
        else:
            if r is not None:
                segs.append({
                    "room_id": r,
                    "t_start": t0,
                    "t_end": p["time"],
                    "duration_ms": p["time"] - t0,
                    "weight": (w_sum/max(1,n)) if n else 1.0
                })
            t0, w_sum, n = pn["time"], 0.0, 0
        last_room = rn
    if last_room and points[-1]["time"] > t0:
        segs.append({
            "room_id": last_room,
            "t_start": t0,
            "t_end": points[-1]["time"],
            "duration_ms": points[-1]["time"] - t0,
            "weight": (w_sum/max(1,n)) if n else 1.0
        })
    print(f"✅ Built {len(segs)} stay segments.")
    return segs

# ---------- 6) Load cleaned stay_segments ----------
def load_cleaned_segments(path: str):
    d = json.load(open(path, "r"))
    segs = []
    for s in d.get("stay_segments", []):
        segs.append({
            "room_id": s["room_id"],
            "t_start": s["start_t"],
            "t_end": s["end_t"],
            "duration_ms": int(round(float(s["duration_s"]) * 1000.0)),
            "weight": 1.0
        })
    print(f"✅ Loaded {len(segs)} stay segments from cleaned file.")
    return segs

# ---------- 7) Auto loader ----------
def load_input_auto(path: str, rooms):
    with open(path, "r") as f:
        d = json.load(f)
    if "stay_segments" in d:
        segs = load_cleaned_segments(path)
        return segs
    else:
        pts = sorted([p for p in d["points"] if p.get("accepted", True)], key=lambda p: p["time"])
        print(f"✅ Loaded {len(pts)} accepted points.")
        segs = build_segments(pts, rooms)
        return segs

# ---------- 8) Aggregate ----------
def aggregate_heat(segs, rooms):
    heat = {rid: 0.0 for rid in rooms.keys()}
    visits = {rid: 0 for rid in rooms.keys()}
    last = None
    for s in segs:
        if s["room_id"] not in heat:
            continue
        heat[s["room_id"]] += s["duration_ms"] * s["weight"]
        if last != s["room_id"]:
            visits[s["room_id"]] += 1
        last = s["room_id"]
    print(f"✅ Aggregated {len(heat)} rooms with dwell time.")
    return heat, visits

# ---------- 9) Normalize colors ----------
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

# ---------- 10) Render ----------
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

    sm = plt.cm.ScalarMappable(norm=norm, cmap=cmap)
    cbar = fig.colorbar(sm, ax=ax, fraction=0.035, pad=0.05)
    cbar.set_label("Dwell time (ms)", rotation=270, labelpad=15)

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
    print(f"✅ Heatmap saved to {out_path}")

# ---------- 11) Main ----------
if __name__ == "__main__":
    BASE = os.path.dirname(os.path.abspath(__file__))

    map_path   = "/Users/fanyiwen/CSE5911_Flowtrack/backend/FlowTrackServer/localRepository/input/maps/test_map_001.json"
    input_path = "/Users/fanyiwen/CSE5911_Flowtrack/backend/FlowTrackServer/localRepository/output/scriptsInputBundle/test_output_bundles.json"
    out_path   = os.path.join(BASE, "test_output_heatmap.png")

    print(f"📦 Using input file: {os.path.basename(input_path)}")

    rooms_raw = load_map(map_path)
    rooms     = make_disjoint(rooms_raw)
    segs      = load_input_auto(input_path, rooms)
    heat, _   = aggregate_heat(segs, rooms)
    rgba, norm, cmap = normalize_and_colorize(heat, alpha=0.8, cmap_name="Reds")
    render_heatmap(rooms, rgba, out_path, heat, norm, cmap)
    print("🎉 Done.")
