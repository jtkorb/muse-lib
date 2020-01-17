package edu.purdue.jtk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowedScalerTest {
    WindowedScaler ws;

    @BeforeEach
    void setUp() {
        ws = new WindowedScaler(60);
    }

    @Test
    void scale() {
        for (int i = 0; i < 100; i++)
            assertEquals(0.5F, ws.scale(0.5F, true));

        ws.resetScale();
        assertEquals(0.5F, ws.scale(1.5F, true));
        for (int i = 0; i < 59; i++)
            assertEquals(0.0F, ws.scale(-0.5F, true));
        assertEquals(0.5F, ws.scale(-0.5F, true));  // Pushes 1.5 out of the queue; -0.5 now scales to 0.5
        assertEquals(0.5F, ws.scale(-0.5F, true));

        ws.resetScale();
        assertEquals(0.5F, ws.scale(-0.5F, true));
        assertEquals(1.0F, ws.scale(1.5F, true));
        assertEquals(0.0F, ws.scale(-0.5F, true));
        assertEquals(0.25F, ws.scale(0.0F, true));

        ws = new WindowedScaler(10);
        Random random = new Random(1);
        for (int i = 0; i < 100; i++) {
            float value = (float) (0.9 + random.nextGaussian() / 10);
            value = Math.min(1.0F, value);
            System.out.format("%8.5f %8.5f\n", value, ws.scale(value, true));
        }
    }
}
