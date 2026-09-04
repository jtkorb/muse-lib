package edu.purdue.jtk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuseRelaxationTest {
    Muse muse;
    Model model;
    MuseControl mc;

    @BeforeEach
    void setUp() {
        muse = new Muse(true);
        model = muse.model;
        mc = muse.getMuseControl();
        model.setDoSmoothing(false);
        model.setUpscaling(false);
        setContactGood();
    }

    @AfterEach
    void tearDown() {
        muse.clearSource();
    }

    @Test
    void highAlphaIsRelaxing() {
        storeBands(0.10f, 0.10f, 0.60f, 0.10f, 0.10f);
        float relaxation = muse.computeRelaxation();
        assertTrue(muse.isRelaxationReliable());
        assertTrue(relaxation > 0.7f, "high alpha should be relaxing, was " + relaxation);
        assertTrue(muse.getRelativeAlpha() > muse.getRelativeBeta());
    }

    @Test
    void highBetaIsThinking() {
        storeBands(0.08f, 0.08f, 0.08f, 0.55f, 0.21f);
        float relaxation = muse.computeRelaxation();
        assertTrue(muse.isRelaxationReliable());
        assertTrue(relaxation < 0.3f, "high beta should be thinking, was " + relaxation);
        assertTrue(muse.getRelativeBeta() > muse.getRelativeAlpha());
    }

    @Test
    void logAbsolutePowersUseBase10() {
        // Muse Direct-style log10 powers: alpha >> beta/gamma.
        storeBands(-0.2f, -1.0f, -0.1f, -1.2f, -1.3f);
        float relaxation = muse.computeRelaxation();
        assertTrue(relaxation > 0.7f, "log10 high-alpha should be relaxing, was " + relaxation);
    }

    @Test
    void prefersEarsWhenContactIsGood() {
        storeBandsAt(Sensor.LEFT_EAR, 0.10f, 0.10f, 0.70f, 0.05f, 0.05f);
        storeBandsAt(Sensor.RIGHT_EAR, 0.10f, 0.10f, 0.70f, 0.05f, 0.05f);
        storeBandsAt(Sensor.LEFT_FH, 0.05f, 0.05f, 0.05f, 0.70f, 0.15f);
        storeBandsAt(Sensor.RIGHT_FH, 0.05f, 0.05f, 0.05f, 0.70f, 0.15f);

        float withEars = muse.computeRelaxation();
        assertTrue(withEars > 0.7f, "ears are high-alpha so index should relax, was " + withEars);

        mc.setShowSensor(Sensor.LEFT_EAR, false);
        mc.setShowSensor(Sensor.RIGHT_EAR, false);
        float foreheadOnly = muse.computeRelaxation();
        assertTrue(foreheadOnly < 0.3f, "forehead is high-beta so index should think, was " + foreheadOnly);
    }

    @Test
    void poorContactHoldsLastValue() {
        storeBands(0.10f, 0.10f, 0.70f, 0.05f, 0.05f);
        float relaxing = muse.computeRelaxation();
        assertTrue(muse.isRelaxationReliable());

        model.setTouchingForehead(false);
        storeBands(0.05f, 0.05f, 0.05f, 0.70f, 0.15f);
        float held = muse.computeRelaxation();
        assertFalse(muse.isRelaxationReliable());
        assertEquals(relaxing, held, 1e-6f);
    }

    @Test
    void storesUnscaledSnapshot() {
        storeBandsAt(Sensor.LEFT_EAR, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        assertEquals(0.3f, muse.getUnscaledGrid(Wave.ALPHA, Sensor.LEFT_EAR), 1e-6f);
        assertEquals(0.4f, muse.getUnscaledGrid(Wave.BETA, Sensor.LEFT_EAR), 1e-6f);
    }

    @Test
    void smoothingBlendsTowardNewValue() {
        model.setDoSmoothing(true);
        storeBands(0.08f, 0.08f, 0.08f, 0.55f, 0.21f);
        float thinking = muse.computeRelaxation();
        storeBands(0.10f, 0.10f, 0.70f, 0.05f, 0.05f);
        float blended = muse.computeRelaxation();
        float relaxingInstant = 0.70f / (0.70f + 0.05f + 0.05f);
        assertTrue(blended > thinking);
        assertTrue(blended < relaxingInstant - 0.05f, "EMA should not jump all the way to the new value");
    }

    private void setContactGood() {
        model.setTouchingForehead(true);
        for (Sensor sensor : Sensor.values()) {
            model.setHorseshoe(sensor.value, 1.0f);
            model.setIsGood(sensor.value, 1);
        }
    }

    private void storeBands(float delta, float theta, float alpha, float beta, float gamma) {
        for (Sensor sensor : Sensor.values()) {
            storeBandsAt(sensor, delta, theta, alpha, beta, gamma);
        }
    }

    private void storeBandsAt(Sensor sensor, float delta, float theta, float alpha, float beta, float gamma) {
        long now = 1L;
        model.setGrid(Wave.DELTA.value, sensor.value, delta, now);
        model.setGrid(Wave.THETA.value, sensor.value, theta, now);
        model.setGrid(Wave.ALPHA.value, sensor.value, alpha, now);
        model.setGrid(Wave.BETA.value, sensor.value, beta, now);
        model.setGrid(Wave.GAMMA.value, sensor.value, gamma, now);
    }
}
