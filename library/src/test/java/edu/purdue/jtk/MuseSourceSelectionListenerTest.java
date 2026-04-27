package edu.purdue.jtk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MuseSourceSelectionListenerTest {
    private Muse muse;

    @BeforeEach
    void setUp() {
        muse = new Muse(true);
    }

    @Test
    void headbandOffHeadbandSequenceNotifiesExpectedStates() {
        List<Boolean> selections = new ArrayList<>();
        muse.setSourceSelectionListener(selections::add);

        // Mirrors UI sequence: click Headband, unclick Headband, click Headband again.
        muse.setBleSource();
        muse.clearSource();
        muse.setBleSource();

        assertEquals(List.of(true, false, true), selections);
    }

    @Test
    void nonHeadbandSourcesNotifyHeadbandDeselected() {
        List<Boolean> selections = new ArrayList<>();
        muse.setSourceSelectionListener(selections::add);

        muse.setGeneratorSource();
        muse.setFileSource();

        assertEquals(List.of(false, false), selections);
    }
}
