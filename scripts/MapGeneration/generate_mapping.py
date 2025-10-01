import pandas as pd
import json
import math
from pathlib import Path 

# hard-coded input data (handled by the customer)
# TODO - integrate into app later to take file as input thru drop-box in app
csv_file = 'basic_room.csv'
building_name = Path(csv_file).stem;

# assumes first column is for room name, rest coords
# explain in user manual
df = pd.read_csv(csv_file);

columns = df.columns.tolist();
id_col = columns[0];
name_col = columns[1];
poly_coords = columns[2:];

data = {
    'building_name': building_name,
    'rooms': []   
}

for _, row in df.iterrows():
    coords = row[poly_coords].to_numpy();
    room = {
        'room_index': row[id_col],
        'room_name': row[name_col],
        'polygon_coords': []
    }
    for j in range(0, len(poly_coords), 2): # Must have even number of poly_coords, otherwise something went wrong
        x = coords[j];
        y = coords[j+1];
        if math.isnan(x) and math.isnan(y): # If empty. For ex, max_vertex_polygon_room_count_in_building = 10, and we have rooms  
            break;                           # with only 5 rooms, the other 5 are NaN, so we skip (csv is left-justified)
        room['polygon_coords'].append((x, y));
    
    data['rooms'].append(room)

#TODO - define reference point?

with open('output.json', 'w') as f:
    json.dump(data, f, indent=4)

print("Mapping complete. Check output.json.")
