package com.example.prototypeapp.domain.rssiSmoother;

public interface RssiSmoother {
    void addSingleRssi(int rssi);
    double getSmoothedRssi();
}
