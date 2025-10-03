#ifndef TRAFFIC_MATRIX_H
#define TRAFFIC_MATRIX_H

#include "./../projutils.h"

struct InputData() {
    std::chrono::nanoseconds    time;
    Data                        point;
    std::string                 room_tag;
    std::vector<Polygon>        room_bounds;
};

class TrafficMatrix {
    private:
        std::vector<std::vector<int>>   __matrix__;
        int                             _num_rooms;
        std::chrono::nanoseconds        tNs_start;
        std::chrono::nanoseconds        tNs_end;
        
    public:
        TrafficMatrix() = default;
        TrafficMatrix(int num_rooms,
                      std::chrono::nanoseconds start, 
                      std::chrono::nanoseconds end); 

        void insert(int row, int column);
        //TODO - std::chrono::time_point buildTimePoint(std::chrono::nanoseconds ns);
};

struct InputDataHash {
    std::size_t operator()(InputData& in_data) {
        //TODO 
    }
};

#endif



