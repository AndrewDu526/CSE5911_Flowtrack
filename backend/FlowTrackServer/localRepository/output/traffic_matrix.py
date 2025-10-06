import json
import os
import numpy as np
import matplotlib.pyplot as plt
from datetime import datetime, timedelta
from typing import Dict

def create_transition_traffic_matrix(data: dict) -> dict:
    """
    Creates a traffic matrix from room transition data.
    
    Args:
        data: JSON data containing meta information and aggregated room transitions
        
    Returns:
        Dictionary containing the traffic matrix and metadata
    """
    
    meta = data.get('meta', {})
    stay_segments = data.get('stay_segments', [])
    rs_aggregated = data.get('rs_aggregated', [])
    
    if stay_segments:
        start_time = min(seg['start_t'] for seg in stay_segments)
        end_time = max(seg['end_t'] for seg in stay_segments)
        start_dt = datetime.fromtimestamp(start_time / 1000)
        end_dt = datetime.fromtimestamp(end_time / 1000)
        
        # Round to hour window
        window_start = start_dt.replace(minute=0, second=0, microsecond=0)
        window_end = window_start + timedelta(hours=1)
    else:
        window_start = None
        window_end = None
    
    rooms = set()
    for transition in rs_aggregated:
        rooms.add(transition['from'])
        rooms.add(transition['to'])
    room_list = sorted(list(rooms))
    n_rooms = len(room_list)
    
    # mapping room to indx
    room_to_idx = {room: idx for idx, room in enumerate(room_list)}
   
    traffic_matrix = np.zeros((n_rooms, n_rooms), dtype=int)
    for transition in rs_aggregated:
        from_idx = room_to_idx[transition['from']]
        to_idx = room_to_idx[transition['to']]
        traffic_matrix[from_idx, to_idx] = transition['count_trips']
    
    result = {
        'metadata': {
            'map_id': meta.get('map_id'),
            'building_id': meta.get('building_id'),
            'floor_id': meta.get('floor_id'),
            'time_window': {
                'start': window_start.isoformat() if window_start else None,
                'end': window_end.isoformat() if window_end else None,
                'start_timestamp_ms': int(window_start.timestamp() * 1000) if window_start else None,
                'end_timestamp_ms': int(window_end.timestamp() * 1000) if window_end else None,
                'duration_hours': 1
            },
            'units': meta.get('units', {}),
            'rooms': room_list
        },
        'traffic_matrix': traffic_matrix.tolist(),
        'room_order': room_list
    }
    
    return result

def create_travel_time_traffic_matrix(data: dict) -> dict:
    """
    Creates a traffic matrix from room transition data.
    
    Args:
        data: JSON data containing meta information and aggregated room transitions
        
    Returns:
        Dictionary containing the traffic matrix and metadata
    """
    
    meta = data.get('meta', {})
    stay_segments = data.get('stay_segments', [])
    rs_aggregated = data.get('rs_aggregated', [])
    
    if stay_segments:
        start_time = min(seg['start_t'] for seg in stay_segments)
        end_time = max(seg['end_t'] for seg in stay_segments)
        start_dt = datetime.fromtimestamp(start_time / 1000)
        end_dt = datetime.fromtimestamp(end_time / 1000)
        
        # Round to hour window
        window_start = start_dt.replace(minute=0, second=0, microsecond=0)
        window_end = window_start + timedelta(hours=1)
    else:
        window_start = None
        window_end = None
    
    rooms = set()
    for transition in rs_aggregated:
        rooms.add(transition['from'])
        rooms.add(transition['to'])
    room_list = sorted(list(rooms))
    n_rooms = len(room_list)
    
    # mapping room to indx
    room_to_idx = {room: idx for idx, room in enumerate(room_list)}
   
    traffic_matrix = np.zeros((n_rooms, n_rooms), dtype=float)
    for transition in rs_aggregated:
        from_idx = room_to_idx[transition['from']]
        to_idx = room_to_idx[transition['to']]
        traffic_matrix[from_idx, to_idx] = transition['avg_travel_s']
   
    # edit this to also contain average transition time
    result = {
        'metadata': {
            'map_id': meta.get('map_id'),
            'building_id': meta.get('building_id'),
            'floor_id': meta.get('floor_id'),
            'time_window': {
                'start': window_start.isoformat() if window_start else None,
                'end': window_end.isoformat() if window_end else None,
                'start_timestamp_ms': int(window_start.timestamp() * 1000) if window_start else None,
                'end_timestamp_ms': int(window_end.timestamp() * 1000) if window_end else None,
                'duration_hours': 1
            },
            'units': meta.get('units', {}),
            'rooms': room_list
        },
        'traffic_matrix': traffic_matrix.tolist(),
        'room_order': room_list
    }
    
    return result

def visualize_travel_time_traffic_matrix(result: dict, out_file_name: str):
    """
    Create a table visualization of the traffic matrix.
    
    Args:
        result: Result dictionary from create_traffic_matrix
        save_path: Path to save the visualization
    """

    rooms = result['room_order']
    matrix = np.array(result['traffic_matrix'])
    metadata = result['metadata']
    
    # figure dimensions
    n_rooms = len(rooms)
    fig_width = max(8, n_rooms * 1.5)
    fig_height = max(6, n_rooms * 1.2)
    
    fig, ax = plt.subplots(figsize=(fig_width, fig_height))
    ax.axis('tight')
    ax.axis('off')
    
    # Headers
    table_data = []
    header_row = ['FROM \\ TO'] + rooms
    table_data.append(header_row)
    for i, from_room in enumerate(rooms):
        row = [from_room] + [str(matrix[i, j]) for j in range(n_rooms)]
        table_data.append(row)
    
    table = ax.table(cellText=table_data, 
                     cellLoc='center',
                     loc='center',
                     bbox=[0, 0, 1, 0.85])
    
    # table styling
    table.auto_set_font_size(False)
    table.set_fontsize(10)
    table.scale(1, 2)
    
    # Style header row and column
    for i in range(len(header_row)):
        cell = table[(0, i)]
        cell.set_facecolor('#4472C4')
        cell.set_text_props(weight='bold', color='white')
    
    for i in range(1, len(table_data)):
        cell = table[(i, 0)]
        cell.set_facecolor('#4472C4')
        cell.set_text_props(weight='bold', color='white')
    
    for i in range(1, len(table_data)):
        for j in range(1, len(header_row)):
            cell = table[(i, j)]
            if i % 2 == 0:
                cell.set_facecolor('#E7E6E6')
            else:
                cell.set_facecolor('#FFFFFF')
    
    time_window = metadata['time_window']
    if time_window['start']:
        time_str = f"{time_window['start']} to {time_window['end']}"
    else:
        time_str = "Time window not specified"
    
    title = f"Room Traffic Matrix\n{metadata['building_id']} - {metadata['floor_id']}\n{time_str}\n\n"f"Average Travelling Time (Seconds)"
    plt.suptitle(title, fontsize=14, fontweight='bold', y=0.98)
     
    save_path = out_file_name + '.png'
    print("save_path: " + save_path)
    plt.savefig(save_path, dpi=300, bbox_inches='tight')
    print(f"✓ Visualization saved to '{save_path}'")
    plt.close()


def visualize_transition_traffic_matrix(result: dict, out_file_name: str):
    """
    Create a table visualization of the traffic matrix.
    
    Args:
        result: Result dictionary from create_traffic_matrix
        save_path: Path to save the visualization
    """

    rooms = result['room_order']
    matrix = np.array(result['traffic_matrix'])
    metadata = result['metadata']
    
    # figure dimensions
    n_rooms = len(rooms)
    fig_width = max(8, n_rooms * 1.5)
    fig_height = max(6, n_rooms * 1.2)
    
    fig, ax = plt.subplots(figsize=(fig_width, fig_height))
    ax.axis('tight')
    ax.axis('off')
    
    # Headers
    table_data = []
    header_row = ['FROM \\ TO'] + rooms
    table_data.append(header_row)
    for i, from_room in enumerate(rooms):
        row = [from_room] + [str(matrix[i, j]) for j in range(n_rooms)]
        table_data.append(row)
    
    table = ax.table(cellText=table_data, 
                     cellLoc='center',
                     loc='center',
                     bbox=[0, 0, 1, 0.85])
    
    # table styling
    table.auto_set_font_size(False)
    table.set_fontsize(10)
    table.scale(1, 2)
    
    # Style header row and column
    for i in range(len(header_row)):
        cell = table[(0, i)]
        cell.set_facecolor('#4472C4')
        cell.set_text_props(weight='bold', color='white')
    
    for i in range(1, len(table_data)):
        cell = table[(i, 0)]
        cell.set_facecolor('#4472C4')
        cell.set_text_props(weight='bold', color='white')
    
    for i in range(1, len(table_data)):
        for j in range(1, len(header_row)):
            cell = table[(i, j)]
            if i % 2 == 0:
                cell.set_facecolor('#E7E6E6')
            else:
                cell.set_facecolor('#FFFFFF')
    
    time_window = metadata['time_window']
    if time_window['start']:
        time_str = f"{time_window['start']} to {time_window['end']}"
    else:
        time_str = "Time window not specified"
    
    title = f"Room Traffic Matrix\n{metadata['building_id']} - {metadata['floor_id']}\n{time_str}\n\n"f"Total transfer counts"
    plt.suptitle(title, fontsize=14, fontweight='bold', y=0.98)
     
    save_path = out_file_name + '.png'
    print("save_path: " + save_path)
    plt.savefig(save_path, dpi=300, bbox_inches='tight')
    print(f"✓ Visualization saved to '{save_path}'")
    plt.close()


def print_traffic_matrix(result: dict):
    """Pretty print the traffic matrix with labels"""
    
    rooms = result['room_order']
    matrix = np.array(result['traffic_matrix'])
    
    print("\n=== Traffic Matrix ===")
    print(f"Building: {result['metadata']['building_id']}")
    print(f"Floor: {result['metadata']['floor_id']}")
    print(f"Map ID: {result['metadata']['map_id']}")
    
    time_window = result['metadata']['time_window']
    if time_window['start']:
        print(f"Time Window: {time_window['start']} to {time_window['end']}")
    else:
        print("Time Window: Not specified")
    
    print(f"\nRooms: {', '.join(rooms)}")
    print("\nTrips FROM (rows) TO (columns):")
    print(f"{'':>10}", end="")
    for room in rooms:
        print(f"{room:>10}", end="")
    print()
    
    for i, from_room in enumerate(rooms):
        print(f"{from_room:>10}", end="")
        for j in range(len(rooms)):
            print(f"{matrix[i, j]:>10}", end="")
        print()


if __name__ == "__main__":
   
    filedir = './scriptsInputBundle/'
    i = 0
    for file in os.listdir(filedir):
        i = i + 1
        with open(filedir + file, 'r') as f:
            data = json.load(f)

        filetitle = file[:file.find(".")]
    
        transition_cnt_matrix = create_transition_traffic_matrix(data)
        travel_time_matrix = create_travel_time_traffic_matrix(data)
        visualize_transition_traffic_matrix(transition_cnt_matrix, filetitle + "_transitions_" + str(i))
        visualize_travel_time_traffic_matrix(travel_time_matrix, filetitle + "_travel_time" + str(i))
    
        # Save to JSON file
        with open(filetitle + "_transitions" + '_out' + str(i) + '.json', 'w') as f:
            json.dump(transition_cnt_matrix, f, indent=2)
        
        with open(filetitle + "_travel_time" + '_out' + str(i) + '.json', 'w') as f:
            json.dump(travel_time_matrix, f, indent=2)
