package edu.purdue.jtk.ble;

/**
 * Estimates EEG band powers from a rolling sample stream.
 */
public final class MuseBandPowerEstimator {

    private static final double MIN_FREQ_HZ = 0.5;
    private static final double MAX_FREQ_HZ = 45.0;

    private final double sampleRateHz;
    private final int windowSize;
    private final int hopSize;

    private final double[] ring;
    private int writeIndex = 0;
    private int sampleCount = 0;
    private int samplesSinceLastEstimate = 0;

    public MuseBandPowerEstimator(double sampleRateHz, int windowSize, int hopSize) {
        if (sampleRateHz <= 0.0) {
            throw new IllegalArgumentException("sampleRateHz must be > 0");
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be > 0");
        }
        if (hopSize <= 0) {
            throw new IllegalArgumentException("hopSize must be > 0");
        }
        if (hopSize > windowSize) {
            throw new IllegalArgumentException("hopSize must be <= windowSize");
        }
        this.sampleRateHz = sampleRateHz;
        this.windowSize = windowSize;
        this.hopSize = hopSize;
        this.ring = new double[windowSize];
    }

    public BandPower addSamples(double[] samplesUv) {
        if (samplesUv == null || samplesUv.length == 0) {
            return null;
        }
        for (double sample : samplesUv) {
            ring[writeIndex] = sample;
            writeIndex = (writeIndex + 1) % windowSize;
            if (sampleCount < windowSize) {
                sampleCount++;
            }
            samplesSinceLastEstimate++;
        }

        if (sampleCount < windowSize || samplesSinceLastEstimate < hopSize) {
            return null;
        }

        samplesSinceLastEstimate = 0;
        return estimate(snapshotWindow());
    }

    private double[] snapshotWindow() {
        double[] out = new double[windowSize];
        int head = writeIndex;
        for (int i = 0; i < windowSize; i++) {
            out[i] = ring[(head + i) % windowSize];
        }
        return out;
    }

    private BandPower estimate(double[] window) {
        int n = window.length;

        double mean = 0.0;
        for (double v : window) {
            mean += v;
        }
        mean /= n;

        double[] xw = new double[n];
        double windowPower = 0.0;
        for (int i = 0; i < n; i++) {
            double w = 0.5 - 0.5 * Math.cos((2.0 * Math.PI * i) / (n - 1));
            xw[i] = (window[i] - mean) * w;
            windowPower += w * w;
        }

        double binWidthHz = sampleRateHz / n;
        int maxBin = n / 2;

        double delta = 0.0;
        double theta = 0.0;
        double alpha = 0.0;
        double beta = 0.0;
        double gamma = 0.0;
        double total = 0.0;

        for (int k = 1; k <= maxBin; k++) {
            double freq = k * binWidthHz;
            if (freq > MAX_FREQ_HZ) {
                break;
            }
            if (freq < MIN_FREQ_HZ) {
                continue;
            }

            double re = 0.0;
            double im = 0.0;
            for (int t = 0; t < n; t++) {
                double angle = (2.0 * Math.PI * k * t) / n;
                re += xw[t] * Math.cos(angle);
                im -= xw[t] * Math.sin(angle);
            }

            double psd = (re * re + im * im) / (sampleRateHz * windowPower);
            double bandPower = psd * binWidthHz;

            total += bandPower;
            if (freq < 4.0) {
                delta += bandPower;
            } else if (freq < 8.0) {
                theta += bandPower;
            } else if (freq < 12.0) {
                alpha += bandPower;
            } else if (freq < 30.0) {
                beta += bandPower;
            } else {
                gamma += bandPower;
            }
        }

        double invTotal = total > 0.0 ? (1.0 / total) : 0.0;
        return new BandPower(
                delta, theta, alpha, beta, gamma,
                delta * invTotal,
                theta * invTotal,
                alpha * invTotal,
                beta * invTotal,
                gamma * invTotal
        );
    }

    public record BandPower(
            double delta,
            double theta,
            double alpha,
            double beta,
            double gamma,
            double relDelta,
            double relTheta,
            double relAlpha,
            double relBeta,
            double relGamma
    ) {
    }
}
