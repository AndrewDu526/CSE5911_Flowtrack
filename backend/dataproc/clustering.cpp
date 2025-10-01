#include "clustering.h"

/*
int main() {
    std::mt19937 rng(static_cast<unsigned>(time(0))); // Mersenne Twister engine
    
    // Define distributions for three clusters
    std::uniform_real_distribution<float> dist1(0.0f, 30.0f);  // Cluster 1: x, y in [0, 30)
    std::uniform_real_distribution<float> dist2(30.0f, 60.0f); // Cluster 2: x, y in [20, 50)
    std::uniform_real_distribution<float> dist3(60.0f, 80.0f); // Cluster 3: x, y in [40, 70)
    std::uniform_real_distribution<float> dist4(80.0f, 110.0f); // Cluster 3: x, y in [40, 70)
    std::uniform_real_distribution<float> dist5(110.0f, 140.0f); // Cluster 3: x, y in [40, 70)
    
    std::vector<Data> v_data;
    for (int i = 0; i < 125; ++i) {
        float x, y;
        if (i < 25) {
            x = dist1(rng);
            y = dist1(rng);
        } else if (i < 50) {
            x = dist2(rng);
            y = dist2(rng);
        } else if (i<75){
            x = dist3(rng);
            y = dist3(rng);
        } else if (i < 100){
            x = dist4(rng);
            y = dist4(rng);
        } else {
            x = dist5(rng);
            y = dist5(rng);
        }

        Data data(x, y);
        v_data.push_back(data);
    }

    std::cout << "***Input Data: \n";
    for(std::size_t i = 0; i < v_data.size(); i++) {
        std::cout << "  Point(" << v_data[i].x << ", " << v_data[i].y << ")\n";
    }

    std::vector<std::vector<Data>> clusters = kMeans(5, v_data);

    for (size_t i = 0; i < clusters.size(); ++i) {
        std::cout << "Cluster " << i + 1 << ":\n";
        for (const auto& point : clusters[i]) {
            std::cout << "  Point(" << point.x << ", " << point.y << ")\n";
        }
    }

    return 0;
}
*/

std::vector<std::vector<Data>> kMeans(const int k, std::vector<Data> vec) {
    const int runs = 10;
    const int max_iter = 50;
    const float threshold = 0.01f;

    std::vector<std::vector<Data>> ret_clus_list;
    std::set<ClusterData> inert_clus; 
                              
    #pragma omp parallel for
    for(int r = 0; r < runs; r++) {
        std::vector<std::vector<Data>> clus_list;
        std::vector<Data> old_centroids;
        std::vector<Data> new_centroids;

          for(int l = 0; l < max_iter; l++) {
           
            // Why does this exist exaclty lol
            for(int i = 0; i < k; i++) {
                Data centroid = clus_list[i][0];  
                clus_list[i].clear();
                clus_list[i].push_back(centroid);
            }

            for(int i = 0; i < vec.size(); i++) {
                if(clustersContains(vec[i], clus_list)) // avoid centroids
                    continue;

                // TODO - O(n^2logk) time complexity, avoid clustercontains and using the set by using hash_set and hash_map
                Data curr_coords = vec[i];
                std::set<DistIndx> srt_distances; 
                //std::unordered_set<DistIndx, DistIndxHash> 
                
                /* 
                 * Closest centroid search
                 */
                for(int j = 0; j < clus_list.size(); j++) { 
                    Data curr_centroid = clus_list[j][0];
                    float dist_to_curr_centroid = eucDist(curr_coords, curr_centroid);
                    srt_distances.insert(DistIndx(dist_to_curr_centroid, j));
                }
           
                auto iterator = srt_distances.begin();
                DistIndx best_dist = *iterator;
                clus_list[best_dist._indx].push_back(curr_coords);
            }
       
            if(l > 0 && centroidsConverged(old_centroids, new_centroids, threshold))
                break;
            
            old_centroids = new_centroids;
            nextCentroids(clus_list, new_centroids);
        }
        
        float curr_inertia = inertia(k, clus_list);
        #pragma omp critical 
        {
            inert_clus.insert(ClusterData(curr_inertia, clus_list));
        }
    }
    
    auto iterator = inert_clus.begin();
    ret_clus_list = iterator->_clusters; 
    return ret_clus_list;
}

float eucDist(Data p1, Data p2) {
    return std::sqrt( (p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y) );
}

float inertia(int k, std::vector<std::vector<Data>> clusters) {
    float inertia = 0;
    for(int i = 0; i < k; i++) {
        Data centroid = clusters[i][0];
        for(int j = 1; j < clusters[i].size(); j++) {
            Data curr_pnt = clusters[i][j];
            inertia += eucDist(curr_pnt, centroid); 
        }
    }
    return inertia;
}

void nextCentroids(std::vector<std::vector<Data>> clusters, std::vector<Data>& centroids) {
    centroids.clear();
    for(int i = 0; i < clusters.size(); i++) {
        Data new_centroid = meanPoint(clusters[i]);
        centroids.push_back(new_centroid);
    }
}

Data meanPoint(std::vector<Data> cluster) {
    if(cluster.size() <= 1) {
        return cluster.empty() ? Data(0, 0) : cluster[0];
    }
    
    float xsum = 0, ysum = 0;
    for(int i = 1; i < cluster.size(); i++) {
        xsum += cluster[i].x;
        ysum += cluster[i].y;
    }
            
    int count = cluster.size() - 1;
    return Data(xsum/(float)count, ysum/(float)count);
}

bool clustersContains(Data val, std::vector<std::vector<Data>> clusters) {
    for(std::size_t i = 0; i < clusters.size(); i++) {
        for(std::size_t j = 0; j < clusters[i].size(); j++) {
            if(val == clusters[i][j])
                return true;
        }
    }
    return false;
}

bool centroidsConverged(const std::vector<Data>& old_centroids, 
                       const std::vector<Data>& new_centroids, 
                       float threshold) {
    if(old_centroids.size() != new_centroids.size()) return false;
    
    for(size_t i = 0; i < old_centroids.size(); i++) {
        if(eucDist(old_centroids[i], new_centroids[i]) > threshold) {
            return false;
        }
    }
    return true;
}
