#include "traffic_matrix.h"

// main will be elsewhere
// main will need to parse json into a structure we can use and build the traffic traffic_matrix
// will need to call point in polygon on the data and put data into the matrix 

TrafficMatrix::TrafficMatrix(int num_rooms,
                             std::chrono::nanoseconds start, 
                             std::chrono::nanoseconds end)
    : this->tNs_start(start), this->tNs_end(end) {
    for(int i = 0; i < num_rooms; i++) {
        std::vector<int> row;
        for(int j = 0; j < num_rooms; j++) {
            row.push_back(0);
        }
        __matrix__.push_back(row);
    }
}

TrafficMatrix::insert(int row, int column) { __matrix__[i][j]++ }








