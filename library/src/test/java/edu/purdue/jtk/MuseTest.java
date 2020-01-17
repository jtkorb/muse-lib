package edu.purdue.jtk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MuseTest {
    Muse muse;
    Model model;
    MuseControl mc;
    Random random = new Random(42);  // Seed value needed for consistent results

    interface SensorChooser {
        boolean choose(Sensor s);
    }

    void storeValues(float value, long currentTime, SensorChooser sc) {
        for (Wave wave : Wave.values())
            for (Sensor sensor : Sensor.values())
                if (sc.choose(sensor))
                    model.setGrid(wave.getValue(), sensor.getValue(), value, currentTime);
                else
                    model.setGrid(wave.getValue(), sensor.getValue(), 0F, currentTime);
    }

    void storeRandomValues(SensorChooser sc) {
        for (Wave wave : Wave.values())
            for (Sensor sensor : Sensor.values())
                if (sc.choose(sensor))
                    model.setGrid(wave.getValue(), sensor.getValue(), random.nextFloat(), 0);
                else
                    model.setGrid(wave.getValue(), sensor.getValue(), 0F, 0);
    }

    String sensorString(Sensor sensor) {
        String s = "";
        for (Wave wave : Wave.values())
            s += String.format("%5.3f ", muse.getGrid(wave, sensor));
        return s;
    }

    void printGrid() {
        System.out.println(sensorString(Sensor.LEFT_FH) + " " + sensorString(Sensor.RIGHT_FH));
        System.out.println(sensorString(Sensor.LEFT_EAR) + " " + sensorString(Sensor.RIGHT_EAR));
    }

    @BeforeEach
    void setUp() {
        muse = new Muse(true);
        model = muse.model;
        mc = muse.getMuseControl();

        model.setUpscaling(false);
        model.setDoSmoothing(false);
    }

    @Test
    void computeWaveVector() {
        PointVector pv;
        float magnitude, activity;

        storeValues(0.5F, 0, s -> (s == Sensor.LEFT_FH || s == Sensor.RIGHT_FH));
        for (Wave wave : Wave.values()) {
            pv = muse.computeWaveVector(wave);
            assertEquals(0.0F, pv.x);
            assertEquals(0.5F, pv.y);
        }

        storeRandomValues(s -> (s == Sensor.LEFT_FH || s == Sensor.RIGHT_FH));
        PointVector[] pvs1 = {
                new PointVector(-0.336F, 0.391F),
                new PointVector(-0.318F, 0.366F),
                new PointVector(0.317F, 0.625F),
                new PointVector(0.215F, 0.492F),
                new PointVector(-0.287F, 0.378F)};

//        System.out.println("Random values in front brain...");
//        printGrid();

        for (Wave wave : Wave.values()) {
            pv = muse.computeWaveVector(wave);
//            System.out.format("%-5s %s\n", String.format("%s:", wave.getName()), pv);
            assertEquals(pvs1[wave.value].x, pv.x, 0.001);
            assertEquals(pvs1[wave.value].y, pv.y, 0.001);
        }

        pv = muse.computeFocus();
        magnitude = pv.magnitude();
        activity = muse.computeActivity();
//        System.out.format("magnitude = %5.3f, activity = %5.3f\n", magnitude, activity);

        assertEquals(0.458, magnitude, 0.001);
        assertEquals(0.225, activity, 0.001);

        mc.setShowSensor(Sensor.RIGHT_FH, false);
        mc.setShowWave(Wave.ALPHA, false);

        PointVector[] pvs2 = {
            new PointVector(-0.364F,0.364F),
            new PointVector(-0.342F,0.342F),
            new PointVector(-0.154F,0.154F),
            new PointVector(-0.139F,0.139F),
            new PointVector(-0.333F,0.333F)
        };

        for (Wave wave : Wave.values()) {
            pv = muse.computeWaveVector(wave);
//            System.out.format("%-5s %s\n", String.format("%s:", wave.getName()), pv);
            assertEquals(pvs2[wave.value].x, pv.x, 0.001);
            assertEquals(pvs2[wave.value].y, pv.y, 0.001);
        }

        pv = muse.computeFocus();
        magnitude = pv.magnitude();
        activity = muse.computeActivity();
//        System.out.format("magnitude = %5.3f, activity = %5.3f\n", magnitude, activity);

        assertEquals(0.416, magnitude, 0.001);
        assertEquals(0.196, activity, 0.001);
    }

    @AfterEach
    void tearDown() {
    }
}
