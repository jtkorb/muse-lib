package edu.purdue.jtk;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The MuseStatistics class is a utility for gathering information about the Muse data passing through the
 * system.  One of its main uses is for debugging--to make sure the data and processing seems "reasonable".
 */
class MuseStatistics {
    private ConcurrentHashMap<String, Integer> addresses = new ConcurrentHashMap<String, Integer>();
    private double timeCurrent = 0L;

    private Statistic focusMagnitude = new Statistic("Focus Mag");
    private Statistic focusX = new Statistic("Focus X");
    private Statistic focusY = new Statistic("Focus Y");
    private Statistic[][] waveRawValues = new Statistic[Wave.values().length][Sensor.values().length];
    private Statistic[][] waveScaledValues = new Statistic[Wave.values().length][Sensor.values().length];
    private Statistic waveRawAll = new Statistic("Raw Waves");
    private Statistic waveScaledAll = new Statistic("Scaled Waves");
    private Statistic wavesDrawn = new Statistic("Drawn Waves");
    private Statistic speeds = new Statistic("Speeds");

    MuseStatistics() {
        for (int waveIndex = 0; waveIndex < Wave.values().length; waveIndex++)
            for (int sensorIndex = 0; sensorIndex < Sensor.values().length; sensorIndex++) {
                waveRawValues[waveIndex][sensorIndex] = new Statistic(String.format("Raw %s @ %s",
                        Wave.values()[waveIndex], Sensor.values()[sensorIndex]));
                waveScaledValues[waveIndex][sensorIndex] = new Statistic(String.format("Scaled %s @ %s",
                        Wave.values()[waveIndex], Sensor.values()[sensorIndex]));
            }
    }

    void countAddress(String address, double timestamp) {
        addresses.put(address, addresses.getOrDefault(address, 0) + 1);
        timeCurrent = timestamp;
    }

    void printStats() {
        Runtime rt = Runtime.getRuntime();

        System.out.format("\nSTATISTICS at %f\n", timeCurrent);

        System.out.println(focusX);
        System.out.println(focusY);
        System.out.println(focusMagnitude);
        System.out.println(speeds);
        System.out.println(waveRawAll);
        System.out.println(waveScaledAll);
        System.out.println(wavesDrawn);

        System.out.format("processors: %d; memory: %3.2f MB free, %3.2fMB total, %3.2fMB max\n",
                rt.availableProcessors(), rt.freeMemory() / 1000000d, rt.totalMemory() / 1000000d, rt.maxMemory() / 1000000d);
    }

    void printMessageSummary() {
        System.out.format("Message Summary\n");

        String[] keys = addresses.keySet().toArray(new String[0]);
        int size = keys.length;

        for (int i = 0; i < size / 2; i++) {
            System.out.format("%5d %-30s", addresses.get(keys[i]), keys[i]);
            int iSecond = i + size/2;
            if (iSecond < size)
                System.out.format("%5d %s", addresses.get(keys[iSecond]), keys[iSecond]);
            System.out.format("\n");
        }
    }

    void trackFocus(PointVector pvFocus) {
        focusX.accumulate(pvFocus.x);
        focusY.accumulate(pvFocus.y);
        focusMagnitude.accumulate(pvFocus.magnitude());
    }

    void trackWave(int waveIndex, int sensorIndex, float value, float scaled) {
        waveRawAll.accumulate(value);
        waveRawValues[waveIndex][sensorIndex].accumulate(value);

        waveScaledAll.accumulate(scaled);
        waveScaledValues[waveIndex][sensorIndex].accumulate(scaled);
    }

    public void trackSpeed(float speed) {
        speeds.accumulate(speed);
    }
}
