package edu.purdue.jtk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.PriorityQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * The MuseFileReader class provides a Muse source that pulls data from a file instead of the Muse headband.
 */
class MuseFileReader extends MuseSource implements Runnable {
    private PriorityQueue<MuseMessage> queue;
    private boolean shutdown = false;

    /**
     * Construct a queue of MuseMessages from an input file and deliver to MuseListener.
     *
     * @param fileName
     */
    MuseFileReader(String fileName, Model model, MuseStatistics ms) {
        super(model, ms);
        String json = readJsonFile(fileName);
        JSONObject jsonObject = parseJsonString(json);
        queue = createEventQueue(jsonObject);
        new Thread(this).start();
    }

    MuseFileReader() {
        super(null, null);
    }

    public void shutdown() {
        super.shutdown();
        shutdown = true;
    }

    String readJsonFile(String fileName) {
        String json;

        try {
            Path path = Paths.get(fileName);
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String data = String.join("\n", lines);
            json = data.replaceAll("\\bNaN\\b", "\"null\"");
        } catch (IOException e) {
            json = "";
            e.printStackTrace();
        }
        return json;
    }

    JSONObject parseJsonString(String json) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;

        try {
            jsonObject = (JSONObject) parser.parse(json);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    PriorityQueue<MuseMessage> createEventQueue(JSONObject jsonObject){
        queue = new PriorityQueue<>();

        System.out.print("Building priority queue...");

        JSONObject timeseries = (JSONObject) jsonObject.get("timeseries");
        for (Object key : timeseries.keySet()) {
            JSONObject value = (JSONObject) timeseries.get(key);
            JSONArray timestamps = (JSONArray) value.get("timestamps");
            JSONArray samples = (JSONArray) value.get("samples");

            assert timestamps.size() == samples.size();
            for (int i = 0; i < timestamps.size(); i++) {
                Object[] arguments = ((JSONArray) samples.get(i)).toArray();
                for (int j = 0; j < arguments.length; j++) {
                    if (arguments[j] instanceof Double)
                        arguments[j] = (float) ((Double) arguments[j]).doubleValue();
                }
                MuseMessage mm = new MuseMessage((double) timestamps.get(i), (String) key, arguments);
                queue.add(mm);
            }
        }
        System.out.println(" done.");
        return queue;
    }

    @Override
    public void run() {
        double currentTime = Double.MAX_VALUE;

        while (!queue.isEmpty() && !shutdown) {
            MuseMessage mm = queue.remove();
            double timestamp = mm.getTimestamp();
            if (timestamp > currentTime) {
                long sleeptime = (long) ((timestamp - currentTime) * 1000);
                try { Thread.sleep(sleeptime); } catch (InterruptedException e) { e.printStackTrace(); }
            }
            currentTime = timestamp;
            dispatchMessage(mm.addrPattern(), mm);
        }
        if (shutdown)
            System.out.println("MuseFileReader shutting down by request.");
        else
            System.out.println("MuseFileReader has run out of data.");
    }
}
