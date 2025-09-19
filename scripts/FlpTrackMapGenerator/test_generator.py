import json
from pathlib import Path
from typing import Union, List
import matplotlib.pyplot as plt


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


def plot_coordinates(coords, output_path, title="FLP Track Test"):
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
    print(f"map save to: {output_path}")


if __name__ == '__main__':
    data = get_latest_json(Path("D:/workspace/monorepo/FlowTrack/backend/testServer/output"))
    coords = extract_coordinates_from_json(data)
    plot_coordinates(coords, output_path="D:/workspace/monorepo/FlowTrack/scripts/FlpTrackMapGenerator/output"
                                         "/track_latest.png")
