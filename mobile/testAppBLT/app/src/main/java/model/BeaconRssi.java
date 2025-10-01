package model;

import java.util.ArrayDeque;
import java.util.Deque;

public class BeaconRssi {
    private final Deque<Integer> window = new ArrayDeque<>();
    private static final int WINDOW_SIZE = 5;

    private double ema = 0;
    private static final double ALPHA = 0.4;

    public void feed(int rssi) {
        if (window.size() >= WINDOW_SIZE) window.pollFirst();
        window.offerLast(rssi);

        // EMA
        if (ema == 0) ema = rssi;
        else ema = ALPHA * rssi + (1 - ALPHA) * ema;
    }

    public double getAvgRssi() {
        if (window.isEmpty()) return 0;
        double sum = 0;
        for (int r : window) sum += r;
        return sum / window.size();
    }

    public double getEmaRssi() {
        return ema;
    }

    public int getLatestRssi() {
        return window.isEmpty() ? 0 : window.getLast();
    }

    public int getSampleCount() {
        return window.size();
    }

    public double getDistanceFromRssi(double txPower, double n) {
        return Math.pow(10.0, (txPower - this.getEmaRssi()) / (10.0 * n));
    }
}
