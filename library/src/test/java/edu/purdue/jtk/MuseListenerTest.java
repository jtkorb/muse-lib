package edu.purdue.jtk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MuseListenerTest {
    MuseListener ml;

    @BeforeEach
    void setUp() {
        ml = new MuseListener(0,null, null);
    }

    @AfterEach
    void tearDown() {
        ml = null;
    }

    @Test
    void extractAddress() {
        assertEquals("alpha_session_score", ml.extractAddress("/muse/elements/alpha_session_score"));
        assertEquals("alpha_session_score", ml.extractAddress("//elements/alpha_session_score"));
        assertEquals("alpha_session_score", ml.extractAddress("muse/elements/alpha_session_score"));
    }
}
