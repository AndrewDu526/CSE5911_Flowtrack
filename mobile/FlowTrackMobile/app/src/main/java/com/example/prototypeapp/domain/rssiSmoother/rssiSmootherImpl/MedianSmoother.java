package com.example.prototypeapp.domain.rssiSmoother.rssiSmootherImpl;

import com.example.prototypeapp.domain.rssiSmoother.RssiSmoother;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Median Filter (Sliding Window) for RSSI smoothing.
 *
 * Description:
 *   Maintains a fixed-size window of the most recent RSSI values.
 *   Each time a new RSSI is added, the oldest is removed if the window is full.
 *   The smoothed RSSI is computed as the median of the values in the window.
 *
 * Compared to Exponential Moving Average (EMA):
 *   - More robust to outliers and sudden spikes
 *   - Better for noisy environments
 *   - Slower to respond to gradual trends
 *
 * Window size should be odd (e.g., 3, 5, 7) for stable median calculation.
 * Larger window = smoother but less responsive.
 */
public class MedianSmoother implements RssiSmoother {
    private final LinkedList<Integer> window = new LinkedList<>();
    private final int windowSize = 5;

    @Override
    public void addSingleRssi(int rssi) {
        // sliding window
        if (window.size() >= windowSize) {window.pollFirst();}
        window.offerLast(rssi);
    }


    @Override
    public double getSmoothedRssi() {
        if (window.isEmpty()) {return 0;}

        List<Integer> sorted = new ArrayList<>(window);
        Collections.sort(sorted);
        int mid = sorted.size() / 2;

        if (sorted.size() % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        } else {
            return sorted.get(mid);
        }
    }
}
