#ifndef CLUSTERING_H
#define CLUSTERING_H

#include "./../projutils.h"

struct DistIndx {
    float _dist;
    int _indx;
    
    DistIndx() = default;
    DistIndx(float d, int i) : _dist(d), _indx(i) {}
   
    DistIndx(const DistIndx&) = default;                // copy constructor
    DistIndx(DistIndx&&) noexcept = default;            // move constructor
    DistIndx& operator=(const DistIndx&) = default;     // Copy assignment 
    DistIndx& operator=(DistIndx&&) = default;          // move assignemnt
                                                        
    bool operator<(const DistIndx& other) const noexcept {
        return _dist == other._dist ? _indx < other._indx : _dist < other._dist;
    }

    bool operator==(const DistIndx& other) const noexcept { 
        return _dist == other._dist && _indx == other._indx;
    }
};

struct ClusterData {
    float _inertia;
    std::vector<std::vector<Data>> _clusters;

    ClusterData() = default;
    ClusterData(float inert, std::vector<std::vector<Data>> cls) : _inertia(inert), _clusters(std::move(cls)) {} 

    ClusterData(ClusterData&) noexcept = default;
    ClusterData(ClusterData&&) noexcept = default;
    ClusterData& operator=(const ClusterData&) = default;
    ClusterData& operator=(ClusterData&&) = default;

    bool operator<(const ClusterData& other) const noexcept { 
        return _inertia < other._inertia; 
    }

    bool operator==(const ClusterData& other) const noexcept { 
        if(_clusters.size() != other._clusters.size()) return false;

        bool inertia_cmpr = _inertia == other._inertia;
        bool _2D_vector_cmpr = true;

        for(std::size_t i = 0; i < other._clusters.size(); i++) {
            for(std::size_t j = 0; j < other._clusters[i].size(); j++) {
                _2D_vector_cmpr = _clusters[i][j] == other._clusters[i][j] ? true : false;
            }
        }

        return inertia_cmpr && _2D_vector_cmpr;
    }
};

bool clustersContains(Data val, std::vector<std::vector<Data>> vec);
std::vector<std::vector<Data>> kMeans(const int k, std::vector<Data> vec);
float eucDist(Data p1, Data p2);
float inertia(int k, std::vector<std::vector<Data>> cluster);
void nextCentroids(std::vector<std::vector<Data>> clusters, std::vector<Data>& new_centroids);
Data meanPoint(std::vector<Data> list);
bool centroidsConverged(const std::vector<Data>& old_centroids, 
                       const std::vector<Data>& new_centroids, 
                       float threshold = 0.01f);

#endif
