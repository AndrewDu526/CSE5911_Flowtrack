#include "traffic_matrix.h"

//#define FLP_JSON_PATH ""
#define POLYGON_JSON_PATH "./..'/../scripts/MapGeneration/output.json"

int main() {
    std::unordered_multiset<InputData>                  in_data; // dont include data with low accuracy (<10-15 or smth)
    std::unordered_map<std::string, int>                rooms_pseudo_enum;  //Build from room tags 
    std::unordered_map<std::string, std::vector<Data>>  room_bounds;

    simdjson::dom::parser parser;
    simdjson::dom::element json_data; 
    try {
        json_data = parser.load(POLYGON_JSON_PATH);
    } catch (const simdjson::simdjson_error &e) {
        std::cerr << "Error parsing JSON: " << e.what() << std::endl;
        return 1;
    }

    // polygon json data grabbing and corresponding local data structure populating
    for (simdjson::dom::element map_data : json_data.get_object()) {
        if (map_data.key() == "rooms") {
            int room_enum_val = 0;
            for (simdjson::dom::element rooms : map_data.value().get_array()) {
                std::string room_tag = rooms["room_name"].get_c_str().value();
                int room_index = rooms["room_index"].get_int64().value(); // Get room_index
                std::vector<Data> coords;

                // Parse the polygon_coords array of [x, y] pairs
                for (simdjson::dom::element coordinates : rooms["polygon_coords"].get_array()) {
                    auto coord_array = coordinates.get_array();
                    if (coord_array.size() == 2) {
                        float x = static_cast<float>(coord_array.at(0).get_double().value());
                        float y = static_cast<float>(coord_array.at(1).get_double().value());
                        coords.emplace_back(x, y); // Use constructor to create Data
                    }
                }

                room_bounds.insert({room_tag, coords});
                room_indices.insert({room_tag, room_enum_val});
                room_enum_val++;
            }
        }
    }
   
    /*
    try {
        json_data = parser.load(FLP_JSON_PATH);
    } catch (const simdjson::simdjson_error &e) {
        std::cerr << "Error parsing JSON: " << e.what() << std::endl;
        return 1;
    }*/

    return 0;
}
