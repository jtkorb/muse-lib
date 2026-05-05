package edu.purdue.jtk;

import java.util.Arrays;

/**
 * Classic BLE EEG: derives per-electrode {@link Model} horseshoe ({@code 1f}/{@code 2f}/{@code 3f}),
 * {@code isGood}, and forehead contact from short-term RMS noise in microvolts (256-sample window).
 */
final class MuseElectrodeQualityEstimator {

    private static final int WINDOW = 256;
    /** Below this RMS (µV) with enough samples is treated as flatlined / poor contact. */
    private static final double MIN_ACTIVE_RMS_UV = 8.0;
    private static final double GOOD_RMS_UV = 90.0;
    private static final double OK_RMS_UV = 240.0;

    private final double[][] ring = new double[4][WINDOW];
    private final int[] writePos = new int[4];
    private final int[] filled = new int[4];

    void reset() {
        Arrays.fill(writePos, 0);
        Arrays.fill(filled, 0);
        for (double[] row : ring) {
            Arrays.fill(row, 0.0);
        }
    }

    void ingest(int sensorIndex0To3, double[] samplesUv) {
        if (samplesUv == null) {
            return;
        }
        if (sensorIndex0To3 < 0 || sensorIndex0To3 > 3) {
            return;
        }
        for (double v : samplesUv) {
            push(sensorIndex0To3, v);
        }
    }

    private void push(int si, double uv) {
        int p = writePos[si];
        ring[si][p] = uv;
        writePos[si] = (p + 1) % WINDOW;
        if (filled[si] < WINDOW) {
            filled[si]++;
        }
    }

    void applyToModel(Model model) {
        boolean streaming = false;
        for (int i = 0; i < 4; i++) {
            if (filled[i] >= 32) {
                streaming = true;
            }
        }

        boolean anyGoodFit = false;
        for (int i = 0; i < 4; i++) {
            double sigma = stdDev(i);
            float shoe = horseshoeFromNoiseRms(sigma, filled[i]);
            model.setHorseshoe(i, shoe);
            model.setIsGood(i, shoe <= 2f ? 1 : 0);
            if (shoe < 3f && filled[i] >= 16) {
                anyGoodFit = true;
            }
        }

        model.setTouchingForehead(streaming && anyGoodFit);
    }

    private static float horseshoeFromNoiseRms(double sigmaUv, int nFilled) {
        if (nFilled < 16) {
            return 2f;
        }
        if (sigmaUv < MIN_ACTIVE_RMS_UV) {
            return 3f;
        }
        if (sigmaUv < GOOD_RMS_UV) {
            return 1f;
        }
        if (sigmaUv < OK_RMS_UV) {
            return 2f;
        }
        return 3f;
    }

    private double stdDev(int si) {
        int n = filled[si];
        if (n < 8) {
            return 0.0;
        }
        double mean = mean(si, n);
        double acc = 0.0;
        int wp = writePos[si];
        for (int k = 0; k < n; k++) {
            int idx = (wp - n + k + WINDOW) % WINDOW;
            double d = ring[si][idx] - mean;
            acc += d * d;
        }
        return Math.sqrt(acc / n);
    }

    private double mean(int si, int n) {
        int wp = writePos[si];
        double sum = 0.0;
        for (int k = 0; k < n; k++) {
            int idx = (wp - n + k + WINDOW) % WINDOW;
            sum += ring[si][idx];
        }
        return sum / n;
    }
}
