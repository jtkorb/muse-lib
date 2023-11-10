package edu.purdue.jtk;

/**
 * The Model class contains all the data that is shared between the Muse headband (including data and file
 * generators) and the views that display the data.
 *
 * @author Tim Korb
 * @since 1.0.0
 */
class Model {
    /**
     * BEGIN MODEL
     *
     * These variables are shared among the views and listeners, running on different threads.
     */
    private MuseStatistics ms;

    private final int WAVE_LENGTH = Wave.values().length;
    private final int SENSOR_LENGTH = Sensor.values().length;

    private final float[][] grid = new float[WAVE_LENGTH][SENSOR_LENGTH];
    private final long[][] time = new long[WAVE_LENGTH][SENSOR_LENGTH];
    private final float[][] gridDrawn = new float[WAVE_LENGTH][SENSOR_LENGTH];
    private final long[][] timeDrawn = new long[WAVE_LENGTH][SENSOR_LENGTH];
    private final WindowedScaler[][] wsGrid = new WindowedScaler[WAVE_LENGTH][SENSOR_LENGTH];

    private float[] horseshoe = new float[SENSOR_LENGTH]; // 1.0f == good, 2.0f == OK, 3.0f == bad
    private int[] isGood = new int[SENSOR_LENGTH]; // 1 == good, 0 == bad
    private boolean touchingForehead;

    private boolean doSmoothing = true;
    private boolean allowUpscaling = true;
    /*
     * END MODEL
     */

    /**
     * Creates a Muse model.
     *
     * @param ms    the MuseStatistics object used to record statistics about the data received.
     */
    Model(MuseStatistics ms) {
        this.ms = ms;
        reset();
    }

    /**
     * Resets the grid and other data structures that holds the shared data.
     */
    void reset() {
        long currentTime = System.currentTimeMillis();

        for (Wave wave : Wave.values())
            for (Sensor sensor : Sensor.values()) {
                grid[wave.value][sensor.value] = 0.0f;
                time[wave.value][sensor.value] = currentTime;
                gridDrawn[wave.value][sensor.value] = 0.0f;
                timeDrawn[wave.value][sensor.value] = currentTime;
                wsGrid[wave.value][sensor.value] = new WindowedScaler(600);  // 600 -> one minute scaling window
            }
    }

    /*
     * MODEL SETTERS
     */
    //@formatter:off
    void setGrid(int waveIndex, int sensorIndex, float value, long currentTime) {
        grid[waveIndex][sensorIndex] = wsGrid[waveIndex][sensorIndex].scale(value, allowUpscaling);  // last received data value
        time[waveIndex][sensorIndex] = currentTime;
    }

    void setTouchingForehead(boolean value) {
        touchingForehead = value;
    }

    void setHorseshoe(int sensorIndex, float value) {
        horseshoe[sensorIndex] = value;
    }

    void setIsGood(int sensorIndex, int value) {
        isGood[sensorIndex] = value;
    }

    void setDoSmoothing(boolean doSmoothing) {
        this.doSmoothing = doSmoothing;
    }

    void setUpscaling(boolean allowUpscaling) { this.allowUpscaling = allowUpscaling; }
    //@formatter:on

    /*
     * MODEL GETTERS
     */

    /**
     * Returns the power value of a given wave at the given sensor.
     *
     * @param wave      Wave value specifying the wave
     * @param sensor    Sensor location to use
     * @return          a float value, in the range [0..1]
     */
    float getGrid(Wave wave, Sensor sensor) {
        long currentTime = System.currentTimeMillis();
        float value, drawn, speed, draw;

        value = grid[wave.value][sensor.value];        // last received data value
        drawn = gridDrawn[wave.value][sensor.value];   // last data value drawn

        /*
         * The code below handles two cases...
         *   (1) No new events have arrived, but want to keep location moving at "current" speed (within limits)
         *   (2) Current event value is too far from last drawn, so need to limit distance moved
         *
         * There are three cases:
         *   (1) time == timeDrawn: have just drawn value at most recently reported location
         *   (2) time < timeDrawn: no new event has arrived, need to extrapolate value to draw
         *   (3) time > timeDrawn: new event arrived, need to interpolate value to draw
         *
         * Case (1): Set speed to zero (draw in same location)
         * Case (2) and (3): Use absolute value of difference to get "current speed"; use that to either
         * interpolate or extrapolate, as necessary (formula is the same).
         */
        long deltaTime = time[wave.value][sensor.value] - timeDrawn[wave.value][sensor.value];
        float deltaValue = value - drawn;

        // Calculate speed of last drawing movement...
        if (deltaTime == 0)
            speed = 0;
        else
            speed = deltaValue / Math.abs(deltaTime);

        ms.trackSpeed(speed);

        // Limit speed to a fraction of the screen per second...
        float ABS_MAX_SPEED = 0.1F;
        speed = Math.min(speed, ABS_MAX_SPEED / 1000);
        speed = Math.max(speed, -ABS_MAX_SPEED / 1000);

        // Calculate amount to move during this time step...
        float move = speed * (currentTime - timeDrawn[wave.value][sensor.value]);

        draw = drawn + move;

        draw = Math.min(draw, 1.0F);
        draw = Math.max(draw, 0.0F);

        timeDrawn[wave.value][sensor.value] = currentTime;

        if (doSmoothing) {
            gridDrawn[wave.value][sensor.value] = draw;
            return draw;
        } else {
            gridDrawn[wave.value][sensor.value] = value;
            return value;
        }
    }

    /**
     * Returns an array of sensor values for the given wave.
     *
     * @param wave      Wave value
     * @return          an array of floats with one value for each sensor
     */
    // float[] getWaveAtSensors(Wave wave) {
    //     float[] values = new float[Sensor.values().length];
    // 
    //     for (Sensor sensor : Sensor.values())
    //         values[sensor.value] = getGrid(wave, sensor);
    //
    //     return values;
    // }

    float getHorseshoe(Sensor sensor) {
        return horseshoe[sensor.value];
    }

    int isGood(Sensor sensor) {
        return isGood[sensor.value];
    }

    boolean getTouchingForehead() {
        return touchingForehead;
    }

    boolean getDoSmoothing() {
        return doSmoothing;
    }

    boolean getUpscaling() {
        return allowUpscaling;
    }
}
