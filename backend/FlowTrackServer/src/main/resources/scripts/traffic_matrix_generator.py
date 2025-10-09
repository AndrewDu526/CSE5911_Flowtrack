#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
traffic_matrix.py
Command-line tool for generating room transition and travel-time matrices (table visualization only).

Usage:
    python traffic_matrix.py <map_json> <bundle_json> <output_dir>
Example:
    python traffic_matrix.py test_map_001.json test_output_bundles.json ./generated
"""

import json
import os
import sys
import numpy as np
import matplotlib.pyplot as plt
from datetime import datetime, timedelta


# =====================================================
# 1. Utility
# =====================================================

def load_json(path: str):
    """Load a JSON file."""
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


# =====================================================
# 2. Matrix Builders
# =====================================================

def create_transition_traffic_matrix(bundle: dict, map_data: dict) -> dict:
    """Build traffic count matrix from room transitions."""
    meta = bundle.get('meta', {})
    stay_segments = bundle.get('stay_segments', [])
    rs_aggregated = bundle.get('rs_aggregated', [])

    # Time window
    if stay_segments:
        start_time = min(seg['start_t'] for seg in stay_segments)
        end_time = max(seg['end_t'] for seg in stay_segments)
        start_dt = datetime.fromtimestamp(start_time / 1000)
        window_start = start_dt.replace(minute=0, second=0, microsecond=0)
        window_end = window_start + timedelta(hours=1)
    else:
        window_start = window_end = None

    # Room ID from map JSON
    room_list = [r['id'] for r in map_data.get('rooms', [])]
    # Include rooms appearing in bundle transitions
    for t in rs_aggregated:
        if t['from'] not in room_list:
            room_list.append(t['from'])
        if t['to'] not in room_list:
            room_list.append(t['to'])
    n = len(room_list)
    room_to_idx = {r: i for i, r in enumerate(room_list)}

    matrix = np.zeros((n, n), dtype=int)
    for t in rs_aggregated:
        i, j = room_to_idx[t['from']], room_to_idx[t['to']]
        matrix[i, j] = t['count_trips']

    return {
        'metadata': {
            'map_id': meta.get('map_id'),
            'building_id': meta.get('building_id'),
            'floor_id': meta.get('floor_id'),
            'time_window': {
                'start': window_start.isoformat() if window_start else None,
                'end': window_end.isoformat() if window_end else None
            },
            'units': meta.get('units', {}),
            'rooms': room_list
        },
        'traffic_matrix': matrix.tolist(),
        'room_order': room_list
    }


def create_travel_time_traffic_matrix(bundle: dict, map_data: dict) -> dict:
    """Build average travel time matrix from room transitions."""
    meta = bundle.get('meta', {})
    rs_aggregated = bundle.get('rs_aggregated', [])

    room_list = [r['id'] for r in map_data.get('rooms', [])]
    for t in rs_aggregated:
        if t['from'] not in room_list:
            room_list.append(t['from'])
        if t['to'] not in room_list:
            room_list.append(t['to'])
    n = len(room_list)
    room_to_idx = {r: i for i, r in enumerate(room_list)}

    matrix = np.zeros((n, n), dtype=float)
    for t in rs_aggregated:
        i, j = room_to_idx[t['from']], room_to_idx[t['to']]
        matrix[i, j] = t['avg_travel_s']

    return {
        'metadata': meta,
        'traffic_matrix': matrix.tolist(),
        'room_order': room_list
    }


# =====================================================
# 3. Visualization
# =====================================================

def visualize_matrix(result: dict, out_path: str, title: str, subtitle: str):
    """Visualize the given traffic matrix as a table."""
    rooms = result['room_order']
    matrix = np.array(result['traffic_matrix'])
    n = len(rooms)

    fig, ax = plt.subplots(figsize=(max(8, n * 1.5), max(6, n * 1.2)))
    ax.axis('tight')
    ax.axis('off')

    # table content
    header = ['FROM \\ TO'] + rooms
    table_data = [header]
    for i, frm in enumerate(rooms):
        row = [frm] + [str(matrix[i, j]) for j in range(n)]
        table_data.append(row)

    table = ax.table(cellText=table_data, cellLoc='center', loc='center', bbox=[0, 0, 1, 0.85])
    table.auto_set_font_size(False)
    table.set_fontsize(10)
    table.scale(1, 2)

    # style
    for i in range(len(header)):
        cell = table[(0, i)]
        cell.set_facecolor('#4472C4')
        cell.set_text_props(weight='bold', color='white')
    for i in range(1, len(table_data)):
        cell = table[(i, 0)]
        cell.set_facecolor('#4472C4')
        cell.set_text_props(weight='bold', color='white')
    for i in range(1, len(table_data)):
        for j in range(1, len(header)):
            table[(i, j)].set_facecolor('#E7E6E6' if i % 2 == 0 else '#FFFFFF')

    plt.suptitle(f"{title}\n{subtitle}", fontsize=14, fontweight='bold', y=0.98)
    plt.savefig(out_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Saved: {out_path}")


# =====================================================
# 4. Main Entry
# =====================================================

def main():
    if len(sys.argv) != 4:
        print("Usage: python traffic_matrix.py <map_json> <bundle_json> <output_dir>")
        sys.exit(1)

    map_path, bundle_path, output_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(output_dir, exist_ok=True)

    print(f"Loading map: {map_path}")
    print(f"Loading bundle: {bundle_path}")

    map_data = load_json(map_path)
    bundle = load_json(bundle_path)

    bundle_name = os.path.splitext(os.path.basename(bundle_path))[0]

    # generate matrices
    transition_m = create_transition_traffic_matrix(bundle, map_data)
    travel_time_m = create_travel_time_traffic_matrix(bundle, map_data)

    # only visualize, no JSON export
    visualize_matrix(transition_m,
                     os.path.join(output_dir, f"{bundle_name}_transitions.png"),
                     "Room Traffic Matrix", "Total Transfer Counts")

    visualize_matrix(travel_time_m,
                     os.path.join(output_dir, f"{bundle_name}_travel_time.png"),
                     "Room Traffic Matrix", "Average Travel Time (Seconds)")

    print("All traffic matrix tables generated successfully.")


if __name__ == "__main__":
    main()
