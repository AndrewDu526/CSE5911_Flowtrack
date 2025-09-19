import json
from pathlib import Path
from typing import Union, List
import matplotlib.pyplot as plt
from geopy.distance import geodesic


def get_latest_json(dir_path: Union[str, Path]) -> dict:
    p = Path(dir_path)
    jsons = [f for f in p.iterdir() if f.is_file() and f.suffix.lower() == ".json"]
    if not jsons:
        raise FileNotFoundError(f"No .json file found in: {p}")
    latest = max(jsons, key=lambda f: f.stat().st_mtime)
    with latest.open("r", encoding="utf-8") as f:
        return json.load(f)


def extract_coordinates_from_json(data: dict) -> List[List[float]]:
    result = []
    for loc in data.get("locations", []):
        try:
            lon = float(loc["lon"])
            lat = float(loc["lat"])
            result.append([lon, lat])
        except (KeyError, ValueError, TypeError):
            continue
    return result


def plot_coordinates_latlon(coords, output_path="track_latlon.png", title="FLP Track (Lat/Lon)"):
    if len(coords) < 2:
        raise ValueError("needs at least 2 track points")

    xs = [pt[0] for pt in coords]  # lon
    ys = [pt[1] for pt in coords]  # lat

    plt.figure(figsize=(8, 6), dpi=150)
    plt.plot(xs, ys, marker='o', linestyle='-', linewidth=1.5, markersize=3)

    # start point and end point
    plt.scatter([xs[0]], [ys[0]], s=60, marker='s', label="Start")
    plt.scatter([xs[-1]], [ys[-1]], s=60, marker='*', label="End")
    plt.annotate("START", (xs[0], ys[0]), xytext=(5, 5), textcoords='offset points', fontsize=8)
    plt.annotate("END", (xs[-1], ys[-1]), xytext=(5, 5), textcoords='offset points', fontsize=8)

    plt.title(title)
    plt.xlabel("Longitude")
    plt.ylabel("Latitude")
    plt.grid(True, linestyle='--', alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(output_path)
    plt.close()
    print(f"Lat/Lon map saved：{output_path}")


def project_to_local_meters(coords):
    """
    transfer coords to relative meters coords
    """
    if len(coords) < 1:
        return []
    origin = coords[0]
    result = []
    for lon, lat in coords:
        dx = geodesic((origin[1], origin[0]), (origin[1], lon)).meters
        dy = geodesic((origin[1], origin[0]), (lat, origin[0])).meters
        if lon < origin[0]: dx = -dx
        if lat < origin[1]: dy = -dy
        result.append([dx, dy])
    return result


def plot_coordinates_meters(coords, output_path="track_meters.png", title="FLP Track (Meters)", grid_size=5):
    if len(coords) < 2:
        raise ValueError("needs at least 2 track points")

    xs = [pt[0] for pt in coords]
    ys = [pt[1] for pt in coords]

    plt.figure(figsize=(8, 6), dpi=150)
    plt.plot(xs, ys, marker='o', linestyle='-', linewidth=1.5, markersize=3)

    plt.scatter([xs[0]], [ys[0]], s=60, marker='s', label="Start")
    plt.scatter([xs[-1]], [ys[-1]], s=60, marker='*', label="End")
    plt.annotate("START", (xs[0], ys[0]), xytext=(5, 5), textcoords='offset points', fontsize=8)
    plt.annotate("END", (xs[-1], ys[-1]), xytext=(5, 5), textcoords='offset points', fontsize=8)

    plt.title(title)
    plt.xlabel("X (meters)")
    plt.ylabel("Y (meters)")
    plt.grid(True, linestyle='--', alpha=0.3)
    plt.gca().set_aspect('equal')
    # set 5*5 meters grid
    plt.xticks(
        ticks=range(int(min(xs)//grid_size)*grid_size, int(max(xs)//grid_size+2)*grid_size, grid_size)
    )
    plt.yticks(
        ticks=range(int(min(ys)//grid_size)*grid_size, int(max(ys)//grid_size+2)*grid_size, grid_size)
    )

    plt.legend()
    plt.tight_layout()
    plt.savefig(output_path)
    plt.close()
    print(f"meters map saved：{output_path}")


if __name__ == '__main__':
    data = get_latest_json(Path("D:/workspace/monorepo/FlowTrack/backend/testServer/output"))
    coords = extract_coordinates_from_json(data)

    plot_coordinates_latlon(coords, output_path="D:/workspace/monorepo/FlowTrack/scripts/FlpTrackMapGenerator/output/track_latlon.png")
    meter_coords = project_to_local_meters(coords)
    plot_coordinates_meters(meter_coords, output_path="D:/workspace/monorepo/FlowTrack/scripts/FlpTrackMapGenerator/output/track_meters.png")
