package edu.purdue.jtk;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.PriorityQueue;

class MuseFileReaderTest {
    String fileName = "data/Muse-1258_2019-05-03--21-16-40_1556971094450.json";
    MuseFileReader museFileReader;

    @BeforeEach
    void setUp() {
        museFileReader = new MuseFileReader();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void handleFileProcessing() {
        String jsonString = museFileReader.readJsonFile(fileName);
        assertEquals(25779597, jsonString.length());

        JSONObject jsonObject = museFileReader.parseJsonString(jsonString);
        assertTrue(jsonObject.containsKey("timeseries"));
        assertTrue(jsonObject.containsKey("meta_data"));
        assertTrue(jsonObject.containsKey("annotations"));

        PriorityQueue<MuseMessage> queue = museFileReader.createEventQueue(jsonObject);
        assertEquals(282979, queue.size());
    }
}
