#ifndef PROJUTILS_H
#define PROJUTILS_H

#include <iostream>
#include <cstdlib>
#include <ctime>
#include <vector>
#include <chrono>
#include <set>
#include <algorithm>
#include <cmath>
#include <random>

constexpr float MAX_FLOAT = std::numeric_limits<float>::max();

struct Data {
    float x;
    float y;
    
    Data() = default;
    Data(float xx, float yy) : x(xx), y(yy) {}

    Data(const Data&) = default;                // copy constructor
    Data(Data&&) noexcept = default;            // move constructor
    Data& operator=(const Data&) = default;     // Copy assignment -> const as we do not modify the original
    Data& operator=(Data&&) noexcept = default; // move assignemnt -> not const since we empty the original

    bool operator==(const Data& other) const noexcept { 
        return x == other.x && y == other.y;
    }
};

struct Polygon {
    std::string         room_tag;
    std::vector<Data>   coords;

    Polygon& operator=(const Polygon&) = default;
    Polygon& operator=(Polygon&&) noexcept = default;
}


#endif
