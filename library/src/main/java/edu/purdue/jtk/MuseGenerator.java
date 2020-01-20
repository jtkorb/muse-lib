package edu.purdue.jtk;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * MuseGenerator generates configurable pseudo-random events to send to a MuseListener.  It is used for debugging
 * and demonstration.
 */
public class MuseGenerator extends MuseSource implements Runnable {
    private Generate generate;
    private Queue<MuseMessage> queue;
    private boolean clearQueue = false;

    private Random random = new Random(1);
    private float[][] grid = new float[Wave.values().length][Sensor.values().length];

    private boolean shutdown = false;

    MuseGenerator(Generate generate, Model model, MuseStatistics ms) {
        super(model, ms);
        this.generate = generate;
        queue = new LinkedList<>();
        fillQueue(System.currentTimeMillis() / 1000);

        new Thread(this).start();
    }

    void setGenerate(Generate generate) {
        this.generate = generate;
        clearQueue = true;
    }

    private double limit(double value) {
        if (value < 0)
            return 0;
        else if (value > 1)
            return 1;
        else
            return value;
    }

    private void setZeroDesinations() {
        for (Wave wave : Wave.values())
            for (Sensor sensor : Sensor.values())
                grid[wave.value][sensor.value] = 0;
    }

    private void setUnfocusedDestinations() {
        double distance = 0.9;
        double variance = 0.3;
        int quadrant = random.nextInt(4);

        for (Sensor sensor : Sensor.values())
            for (Wave wave : Wave.values())
                if (sensor.value == quadrant)
                    grid[wave.value][sensor.value] = (float) limit(distance + variance * random.nextGaussian());
                else
                    grid[wave.value][sensor.value] = (float) limit( distance / 2 + variance / 2 * random.nextGaussian());
    }

    private void setFocusedDestinations() {
        for (Sensor sensor : Sensor.values())
            for (Wave wave : Wave.values())
//              grid[wave.value][sensor.value] = (float) limit(0.7);
                grid[wave.value][sensor.value] = (float) limit(0.7 + 0.02 * random.nextGaussian());
    }

    private void setCalmDestinations() {
        for (Sensor sensor : Sensor.values())
            for (Wave wave : Wave.values())
//              grid[wave.value][sensor.value] = (float) limit(0.1);
                grid[wave.value][sensor.value] = (float) limit(0.1 + 0.02 * random.nextGaussian());
    }

    private void setHalfDestinations(int q1, int q2) {
        double distance = 0.9;
        double variance = 0.3;
        for (Sensor sensor : Sensor.values())
            for (Wave wave : Wave.values())
                if (sensor.value == q1 || sensor.value == q2)
                    grid[wave.value][sensor.value] = (float) limit(distance + variance * random.nextGaussian());
                else
                    grid[wave.value][sensor.value] = (float) limit( distance / 10 + variance / 2 * random.nextGaussian());
    }

    private void setFrontBrainDestinations() {
        setHalfDestinations(Sensor.LEFT_FH.value, Sensor.RIGHT_FH.value);
    }

    private void setRearBrainDestinations() {
        setHalfDestinations(Sensor.LEFT_EAR.value, Sensor.RIGHT_EAR.value);
    }

    private void setLeftBrainDestinations() {
        setHalfDestinations(Sensor.LEFT_FH.value, Sensor.LEFT_EAR.value);
    }

    private void setRightBrainDestinations() {
        setHalfDestinations(Sensor.RIGHT_FH.value, Sensor.RIGHT_EAR.value);
    }

    private void setMaxRight() {
        for (Wave wave : Wave.values()) {
            for (Sensor sensor : Sensor.values()) {
                if (sensor.value == Sensor.RIGHT_FH.value || sensor.value == Sensor.RIGHT_EAR.value)
                    grid[wave.value][sensor.value] = 1;
                else
                    grid[wave.value][sensor.value] = 0;
            }
        }
    }

    private void setUpperRightDestinations(float value) {
        for (Wave wave : Wave.values()) {
            for (Sensor sensor : Sensor.values()) {
                if (sensor.value == Sensor.RIGHT_FH.value)
                    grid[wave.value][sensor.value] = value;
                else
                    grid[wave.value][sensor.value] = 0;
            }
        }
    }

    private void generateHorshoe(double currentTime) {
        MuseMessage mm;

        Integer[] integers = new Integer[Sensor.values().length];
        for (int i = 0; i < integers.length; i++)
            integers[i] = 1;

        mm = new MuseMessage(currentTime, "touching_forehead", integers);
        queue.add(mm);

        Float[] floats = new Float[Sensor.values().length];
        for (int i = 0; i < floats.length; i++)
            floats[i] = 1.0F;

        mm = new MuseMessage(currentTime, "horseshoe", floats);
        queue.add(mm);
    }

    private void generateEvents(double currentTime, int millis, int gap) {
        generateHorshoe(currentTime);

        for (int t = 0; t < millis; t += gap) {
            for (Wave wave : Wave.values()) {
                Float[] arguments = new Float[Sensor.values().length];
                for (Sensor sensor : Sensor.values())
                    arguments[sensor.value] = grid[wave.value][sensor.value];
                MuseMessage mm = new MuseMessage(currentTime + t / 1000.0, wave.getAddress(), arguments);
                queue.add(mm);
            }
        }
    }

    private void fillQueue(double currentTime) {
        int MILLIS = 1000;
        int GAP = 50;

        if (generate == Generate.Winner)
            generateWinners(currentTime);
        else if (generate == Generate.Loser)
            generateLosers(currentTime);
        else {
            for (int i = 0; i < 10; i++) {
                switch (generate) {
                    case Unfocused: setUnfocusedDestinations(); break;
                    case Focused: setFocusedDestinations(); break;
                    case Calm: setCalmDestinations(); break;
                    case FrontBrain: setFrontBrainDestinations(); break;
                    case RearBrain: setRearBrainDestinations(); break;
                    case LeftBrain: setLeftBrainDestinations(); break;
                    case RightBrain: setRightBrainDestinations(); break;
                    case MaxRight: setMaxRight(); break;
                    case Zero: setZeroDesinations(); break;
                }
                generateEvents(currentTime + i * MILLIS / 1000, MILLIS, GAP);
            }
        }
}

    private void generateLosers(double currentTime) {
        int CALIBRATION = 10000;
        int FOCUS = 60000;
        int START = FOCUS / 6;
        float initialValue = 0.2F;
        float finalValue = 0.8F;

        generateValues(currentTime, initialValue, initialValue, CALIBRATION);
        generateValues(currentTime + CALIBRATION / 1000, finalValue, initialValue, START);
        generateValues(currentTime + (CALIBRATION + START) / 1000, initialValue, finalValue, FOCUS - START);
    }

    private void generateWinners(double currentTime) {
        int CALIBRATION = 10000;
        int FOCUS = 60000;
        float initialValue = 0.8F;
        float finalValue = 0.2F;
        generateValues(currentTime, 0.5F, 0.5F, CALIBRATION);
        generateValues(currentTime + CALIBRATION / 1000, initialValue, finalValue, FOCUS);
    }

    private void generateValues(double currentTime, float initialValue, float finalValue, int millis) {
        int GAP = 25;
        float value = initialValue;

        for (int t = 0; t < millis; t += GAP) {
            setUpperRightDestinations(value);
            for (Wave wave : Wave.values()) {
                Float[] arguments = new Float[Sensor.values().length];
                for (Sensor sensor : Sensor.values())
                    arguments[sensor.value] = grid[wave.value][sensor.value];
                MuseMessage mm = new MuseMessage(currentTime + t / 1000.0, wave.getAddress(), arguments);
                queue.add(mm);
            }
            value -= (initialValue - finalValue) / (millis / GAP);
        }
    }

    @Override
    public void run() {
        while (!shutdown) {
            double currentTime = System.currentTimeMillis() / 1000D;
            if (clearQueue) {
                queue.clear();
                clearQueue = false;
            }
            if (queue.isEmpty())
                fillQueue(currentTime);
            MuseMessage mm = queue.remove();

            double timestamp = mm.getTimestamp();
            if (timestamp > currentTime) {
                long sleeptime = (long) ((timestamp - currentTime) * 1000);
                try { Thread.sleep(sleeptime); } catch (InterruptedException e) { e.printStackTrace(); }
            }

            dispatchMessage(mm.addrPattern(), mm);
        }

        model.reset();  // NB: This ia a bit of a hack--the resetGrid() should be handled elsewhere, after the Generator is fully shutdown.
        System.out.println("Generator shutting down.");
    }

    @Override
    public void shutdown() {
        super.shutdown();
        shutdown = true;
    }
}
