package edu.purdue.jtk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MuseControlTest {
    Muse muse;
    MuseControl mc;

    @BeforeEach
    void setUp() {
        muse = new Muse(Generate.Unfocused);
        mc = muse.getMuseControl();
        System.out.format("MuseControl says it is ready\n");
    }

    @Test
    void getScaleValue() {
        assertEquals(100, mc.getScaleValue());
        assertEquals(true, mc.getShowActivity());
        assertEquals(true, mc.getShowFocus());
    }
}
