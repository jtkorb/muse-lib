package edu.purdue.jtk.ble;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuseBandPowerEstimatorTest {
    private static final double SAMPLE_RATE_HZ = 256.0;
    private static final int WINDOW = 512;
    private static final int HOP = 96;

    @Test
    void tenHertzSineDominatesAlpha() {
        MuseBandPowerEstimator.BandPower bands = estimateSine(10.0);
        assertNotNull(bands);
        assertTrue(bands.relAlpha() > 0.4, "10 Hz should land in alpha, relAlpha=" + bands.relAlpha());
        assertTrue(bands.relAlpha() > bands.relDelta());
        assertTrue(bands.relAlpha() > bands.relTheta());
        assertTrue(bands.relAlpha() > bands.relBeta());
        assertTrue(bands.relAlpha() > bands.relGamma());
    }

    @Test
    void twentyHertzSineDominatesBeta() {
        MuseBandPowerEstimator.BandPower bands = estimateSine(20.0);
        assertNotNull(bands);
        assertTrue(bands.relBeta() > 0.4, "20 Hz should land in beta, relBeta=" + bands.relBeta());
        assertTrue(bands.relBeta() > bands.relDelta());
        assertTrue(bands.relBeta() > bands.relTheta());
        assertTrue(bands.relBeta() > bands.relAlpha());
        assertTrue(bands.relBeta() > bands.relGamma());
    }

    private static MuseBandPowerEstimator.BandPower estimateSine(double frequencyHz) {
        MuseBandPowerEstimator estimator = new MuseBandPowerEstimator(SAMPLE_RATE_HZ, WINDOW, HOP);
        double[] samples = new double[WINDOW];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = 50.0 * Math.sin(2.0 * Math.PI * frequencyHz * i / SAMPLE_RATE_HZ);
        }
        return estimator.addSamples(samples);
    }
}
