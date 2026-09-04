package edu.purdue.jtk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MuseGeneratorTest {
    Muse muse;

    @BeforeEach
    void setUp() {
        muse = new Muse(true);
        muse.model.setDoSmoothing(false);
        muse.setGeneratorSource();
    }

    @AfterEach
    void tearDown() {
        muse.clearSource();
    }

    @Test
    void thinkingIsTowardZero() throws InterruptedException {
        float relaxation = sampleAfter(Generate.Thinking);
        assertTrue(relaxation < 0.45f, "Thinking generator should be toward thinking, was " + relaxation);
    }

    @Test
    void relaxingIsTowardOne() throws InterruptedException {
        float relaxation = sampleAfter(Generate.Relaxing);
        assertTrue(relaxation > 0.55f, "Relaxing generator should be toward relaxing, was " + relaxation);
    }

    @Test
    void thinkingAndRelaxingMoveOppositeDirections() throws InterruptedException {
        float thinking = sampleAfter(Generate.Thinking);
        float relaxing = sampleAfter(Generate.Relaxing);
        assertTrue(relaxing > thinking + 0.2f,
                "Relaxing (" + relaxing + ") should be well above Thinking (" + thinking + ")");
    }

    @Test
    void keysSelectThinkingAndRelaxing() throws InterruptedException {
        assertTrue(muse.getMuseControl().handleKeyPressed('t', 0));
        float thinking = sampleAfter(null);
        assertTrue(muse.getMuseControl().handleKeyPressed('r', 0));
        float relaxing = sampleAfter(null);
        assertTrue(relaxing > thinking + 0.2f,
                "key R (" + relaxing + ") should be well above key T (" + thinking + ")");
    }

    private float sampleAfter(Generate generate) throws InterruptedException {
        if (generate != null) {
            muse.setGenerate(generate);
        }
        Thread.sleep(250);
        waitUntilReliable();
        return muse.computeRelaxation();
    }

    private void waitUntilReliable() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            muse.computeRelaxation();
            if (muse.isRelaxationReliable()) {
                return;
            }
            Thread.sleep(40);
        }
        throw new AssertionError("generator never produced a reliable relaxation sample");
    }
}
