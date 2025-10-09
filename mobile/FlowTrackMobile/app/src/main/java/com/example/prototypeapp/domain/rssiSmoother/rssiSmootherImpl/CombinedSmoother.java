package com.example.prototypeapp.domain.rssiSmoother.rssiSmootherImpl;

import com.example.prototypeapp.domain.rssiSmoother.RssiSmoother;

import java.util.LinkedList;

/**
 * Combined RSSI Smoother: Applies Median filter first, then Exponential Moving Average (EMA).
 *
 * This combination improves stability by filtering out outliers (Median),
 * while preserving trend responsiveness (EMA).
 */
public class CombinedSmoother implements RssiSmoother {

    private final MedianSmoother medianSmoother = new MedianSmoother();
    private final EmaSmoother emaSmoother = new EmaSmoother();

    @Override
    public void addSingleRssi(int rssi) {
        medianSmoother.addSingleRssi(rssi);
        double median = medianSmoother.getSmoothedRssi();
        emaSmoother.addSingleRssi((int) median);
    }

    @Override
    public double getSmoothedRssi() {
        return emaSmoother.getSmoothedRssi();
    }
}
