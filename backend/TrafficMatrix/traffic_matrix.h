#ifndef TRAFFIC_MATRIX_H
#define TRAFFIC_MATRIX_H

#include "projutils.h"

struct CellData {
    
};

class TrafficMatrix {
    private:
        std::vector<std::vector<CellData>> __matrix__;
        std::chrono::nanoseconds tNs_start;
        std::chrono::nanoseconds tNs_end;
        
    public:
        TrafficMatrix() = default;
        TrafficMatrix(std::chrono::nanoseconds start, std::chrono::nanoseconds end) 
            : this->tNs_start(start), this->tNs_end(end) 

        void insert(int row, int column);
        //TODO - buildTimePoint()
};


#endif



