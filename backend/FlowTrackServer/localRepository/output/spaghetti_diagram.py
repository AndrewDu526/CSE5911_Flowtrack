import json
import matplotlib.pyplot as plt
from matplotlib.patches import Polygon
from matplotlib.collections import PatchCollection
import numpy as np
import random
import matplotlib.colors as mcolors
import math

"""Load and return a JSON file as a Python object."""
def load_json(file_path: str):
    with open(file_path, "r") as f:
        return json.load(f)

"""Compute center of polygon (average of vertex coordinates)."""
def get_room_centroid(vertices):
    xs = [v["x"] for v in vertices]
    ys = [v["y"] for v in vertices]
    return np.mean(xs), np.mean(ys)

"""
Draw rooms as polygons on the provided matplotlib axis.
Returns a dict mapping room_id -> (centroid_x, centroid_y)
    
"""
def draw_rooms(ax, rooms):
    patches = []
    centroids = {}

    for room in rooms:
        verts = [(v["x"], v["y"]) for v in room["vertices"]]
        polygon = Polygon(verts, closed=True)
        patches.append(polygon)

        # Compute center of the polygons (room shapes)
        cx, cy = get_room_centroid(room["vertices"])
        centroids[room["id"]] = (cx, cy)

        # Add room label can change where the room label will be written
        ax.text(cx, cy + 1, room["id"], ha="center", va="center",
                fontsize=9, color="black", weight="bold")

    # Draw all polygons together and style of how rooms are drawn
    p = PatchCollection(patches, alpha=0.5, facecolor="skyblue",
                        edgecolor="black", linewidth=1.2)
    ax.add_collection(p)
    return centroids

"""
Draw arrows between rooms based on traffic_matrix counts.
Arrows are given a random color every time.
Repeated arrows between the same pair are slightly shifted so they don't overlap.
    
"""
def draw_arrows(ax, centroids, traffic_matrix, room_order):
    n = len(room_order)
    colors = list(mcolors.TABLEAU_COLORS.values())
    # Track how many arrows have been drawn between each room pair (both directions)
    arrow_counts = {}

    for i in range(n):
        for j in range(n):
            count = traffic_matrix[i][j]
            if count > 0:
                start = centroids[room_order[i]]
                end = centroids[room_order[j]]
                if start == end:
                    continue

                # Create a key for the pair (unordered) to track shifts
                pair_key = tuple(sorted([room_order[i], room_order[j]]))
                k = arrow_counts.get(pair_key, 0)  # how many arrows already drawn
                arrow_counts[pair_key] = k + 1

                # Compute arrow direction
                dx, dy = end[0] - start[0], end[1] - start[1]

                # Compute perpendicular shift
                dist = math.sqrt(dx**2 + dy**2)
                if dist != 0:
                    shift_magnitude = 0.2 * k  # shift distance grows with number of arrows
                    shift_x = -dy / dist * shift_magnitude
                    shift_y = dx / dist * shift_magnitude
                else:
                    shift_x = shift_y = 0

                # Apply shift to start and end positions
                start_shifted = (start[0] + shift_x, start[1] + shift_y)
                end_shifted   = (end[0] + shift_x, end[1] + shift_y)

                # Pick a random color for the arrow
                color = random.choice(colors)

                ax.arrow(start_shifted[0], start_shifted[1], 
                         end_shifted[0] - start_shifted[0], 
                         end_shifted[1] - start_shifted[1],
                         length_includes_head=True,
                         head_width=0.25 + 0.05 * count,
                         head_length=0.4 + 0.1 * count,
                         fc=color, ec=color, alpha=1,
                         linewidth=0.8 + 0.2 * count)

"""
Generate a spaghetti diagram from map and traffic data and save it as a PNG.

"""
def generate_spaghetti_diagram(map_file, traffic_file, output_file="spaghetti_diagram.png"):
    # Load data
    map_data = load_json(map_file)
    traffic_data = load_json(traffic_file)

    rooms = map_data["rooms"]
    traffic_matrix = traffic_data["traffic_matrix"]
    room_order = traffic_data["room_order"]

    # Setup figure
    fig, ax = plt.subplots(figsize=(8, 6))
    ax.set_title("Spaghetti Diagram — Floor Plan Traffic Flow", fontsize=12, weight="bold")
    ax.set_aspect("equal", adjustable="datalim")
    ax.axis('off')

    # Draw layout and arrows
    centroids = draw_rooms(ax, rooms)
    draw_arrows(ax, centroids, traffic_matrix, room_order)

    # Labels and grid (can add back if needed)
    """
    ax.set_xlabel("X (m)")
    ax.set_ylabel("Y (m)")
    ax.grid(True, linestyle="--", alpha=0.4)

    """

    # Save output
    plt.savefig(output_file, dpi=300, bbox_inches="tight")
    plt.close(fig)
    print(f"✅ Spaghetti diagram saved to: {output_file}")


if __name__ == "__main__":
    #change filenames as needed
    map_file = "/Users/dongj/CSE5911_Flowtrack/backend/FlowTrackServer/localRepository/input/maps/test_map_001.json"
    traffic_file = "/Users/dongj/CSE5911_Flowtrack/backend/FlowTrackServer/localRepository/output/test_output_bundles_transitions_out1.json"
    output_file = "spaghetti_diagram.png"

    generate_spaghetti_diagram(map_file, traffic_file, output_file)
