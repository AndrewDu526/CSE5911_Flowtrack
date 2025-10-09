package com.example.prototypeapp.domain.rssiSmoother.rssiSmootherImpl;

import com.example.prototypeapp.domain.rssiSmoother.RssiSmoother;

/**
 * Exponential Moving Average (EMA) for RSSI smoothing.
 *
 * Formula:
 *      EMAₜ = α * xₜ + (1 - α) * EMAₜ₋₁
 *
 * Where:
 *   - xₜ:     RSSI value at current time step
 *   - EMAₜ₋₁: EMA from previous step
 *   - α:      smoothing factor, 0 < α < 1 (higher α gives more weight to recent values)
 *
 * Common α values range from 0.2 to 0.5. In this implementation, α = 0.4.
 *
 * Compared to sliding window methods, EMA requires less memory and reacts more quickly,
 * but may be more sensitive to noise.
 */
public class EmaSmoother implements RssiSmoother {

    private static final double ALPHA = 0.4;

    private double ema = 0;

    @Override
    public void addSingleRssi(int rssi) {ema = ALPHA * rssi + (1 - ALPHA) * ema;}


    @Override
    public double getSmoothedRssi() {return ema;}
}

