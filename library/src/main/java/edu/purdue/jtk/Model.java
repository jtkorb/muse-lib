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
    private final float[][] unscaledGrid = new float[WAVE_LENGTH][SENSOR_LENGTH];
    private final long[][] time = new long[WAVE_LENGTH][SENSOR_LENGTH];
    private final float[][] gridDrawn = new float[WAVE_LENGTH][SENSOR_LENGTH];
    private final long[][] timeDrawn = new long[WAVE_LENGTH][SENSOR_LENGTH];
    private final WindowedScaler[][] wsGrid = new WindowedScaler[WAVE_LENGTH][SENSOR_LENGTH];

    private float[] horseshoe = new float[SENSOR_LENGTH]; // 1.0f == good, 2.0f == OK, 3.0f == bad
    private int[] isGood = new int[SENSOR_LENGTH]; // 1 == good, 0 == bad
    private boolean touchingForehead;

    private boolean doSmoothing = true;
    private boolean allowUpscaling = false;
    private boolean useBleAbsolute = false;

    /** Horseshoe 1 = good, 2 = OK; values at or above this are too poor to use. */
    static final float HORSESHOE_UNUSABLE = 2.5f;
    static final float RELAXATION_EMA = 0.2f;

    private float smoothedRelaxation = 0.5f;
    private boolean hasRelaxationSample = false;
    private boolean relaxationReliable = false;
    private float relativeAlpha = 0f;
    private float relativeBeta = 0f;
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
                unscaledGrid[wave.value][sensor.value] = 0.0f;
                time[wave.value][sensor.value] = currentTime;
                gridDrawn[wave.value][sensor.value] = 0.0f;
                timeDrawn[wave.value][sensor.value] = currentTime;
                wsGrid[wave.value][sensor.value] = new WindowedScaler(600);  // 600 -> one minute scaling window
            }

        smoothedRelaxation = 0.5f;
        hasRelaxationSample = false;
        relaxationReliable = false;
        relativeAlpha = 0f;
        relativeBeta = 0f;
    }

    /*
     * MODEL SETTERS
     */
    //@formatter:off
    void setGrid(int waveIndex, int sensorIndex, float value, long currentTime) {
        unscaledGrid[waveIndex][sensorIndex] = value;
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

    void setUseBleAbsolute(boolean useBleAbsolute) { this.useBleAbsolute = useBleAbsolute; }
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

        // Substitute FH value if EAR is not good...
        if (getHorseshoe(sensor) >= 2f) {
            // System.err.format("horseshoe = %f; isGood = %d\n", getHorseshoe(sensor), isGood(sensor));
            if (sensor.value == Sensor.LEFT_EAR.value) {
                value = grid[wave.value][Sensor.LEFT_FH.value];
                // System.err.printf("LEFT_EAR missing; using LEFT_FH (%6.3f)\n", value);
            } else if (sensor.value == Sensor.RIGHT_EAR.value) {
                value = grid[wave.value][Sensor.RIGHT_FH.value];
                // System.err.printf("RIGHT_EAR missing; using RIGHT_FH (%6.3f)\n", value);
            }
        }

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

    float getDrawn(Wave wave, Sensor sensor) {
        return gridDrawn[wave.value][sensor.value];
    }

    float getUnscaledGrid(Wave wave, Sensor sensor) {
        return unscaledGrid[wave.value][sensor.value];
    }

    /**
     * Thinking-vs-relaxing index in [0, 1]: 1 is high alpha (relaxing), 0 is high beta/gamma (thinking).
     * Uses unscaled band powers so independently auto-scaled cells cannot distort the ratio.
     * When contact is poor, returns the last good smoothed value and {@link #isRelaxationReliable()} is false.
     */
    float computeRelaxation(boolean[] showSensor) {
        boolean[] usable = new boolean[SENSOR_LENGTH];
        boolean anyGoodEar = false;

        for (Sensor sensor : Sensor.values()) {
            if (showSensor != null && !showSensor[sensor.value]) {
                continue;
            }
            if (horseshoe[sensor.value] >= HORSESHOE_UNUSABLE) {
                continue;
            }
            usable[sensor.value] = true;
            if (sensor == Sensor.LEFT_EAR || sensor == Sensor.RIGHT_EAR) {
                anyGoodEar = true;
            }
        }

        if (anyGoodEar) {
            for (Sensor sensor : Sensor.values()) {
                if (sensor != Sensor.LEFT_EAR && sensor != Sensor.RIGHT_EAR) {
                    usable[sensor.value] = false;
                }
            }
        }

        int count = 0;
        double sumIndex = 0;
        double sumAlpha = 0;
        double sumBeta = 0;

        for (Sensor sensor : Sensor.values()) {
            if (!usable[sensor.value]) {
                continue;
            }
            double[] relative = relativePowers(sensor);
            if (relative == null) {
                continue;
            }
            double alpha = relative[Wave.ALPHA.value];
            double beta = relative[Wave.BETA.value];
            double gamma = relative[Wave.GAMMA.value];
            double denom = alpha + beta + gamma;
            if (denom <= 1e-12) {
                continue;
            }
            sumIndex += alpha / denom;
            sumAlpha += alpha;
            sumBeta += beta;
            count++;
        }

        boolean reliable = touchingForehead && count > 0;
        relaxationReliable = reliable;

        if (!reliable) {
            return hasRelaxationSample ? smoothedRelaxation : 0.5f;
        }

        float instant = (float) (sumIndex / count);
        relativeAlpha = (float) (sumAlpha / count);
        relativeBeta = (float) (sumBeta / count);

        if (doSmoothing && hasRelaxationSample) {
            smoothedRelaxation += RELAXATION_EMA * (instant - smoothedRelaxation);
        } else {
            smoothedRelaxation = instant;
        }
        hasRelaxationSample = true;
        return smoothedRelaxation;
    }

    boolean isRelaxationReliable() {
        return relaxationReliable;
    }

    float getRelativeAlpha() {
        return relativeAlpha;
    }

    float getRelativeBeta() {
        return relativeBeta;
    }

    /**
     * Converts the five unscaled bands at a sensor into relative powers that sum to 1.
     * Linear [0, 1] values (BLE relative, generator) are normalized across bands.
     * Log-scale absolute powers (OSC / BLE absolute, often negative) are converted with 10^v first.
     */
    private double[] relativePowers(Sensor sensor) {
        float[] raw = new float[WAVE_LENGTH];
        boolean any = false;
        boolean logScale = useBleAbsolute;

        for (Wave wave : Wave.values()) {
            raw[wave.value] = unscaledGrid[wave.value][sensor.value];
            if (raw[wave.value] != 0f) {
                any = true;
            }
            if (raw[wave.value] < 0f) {
                logScale = true;
            }
        }
        if (!any) {
            return null;
        }

        double[] power = new double[WAVE_LENGTH];
        double sum = 0;
        for (int i = 0; i < WAVE_LENGTH; i++) {
            double p = logScale ? Math.pow(10.0, raw[i]) : Math.max(raw[i], 0.0);
            power[i] = p;
            sum += p;
        }
        if (sum <= 0) {
            return null;
        }
        double inv = 1.0 / sum;
        for (int i = 0; i < WAVE_LENGTH; i++) {
            power[i] *= inv;
        }
        return power;
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

    boolean getUseBleAbsolute() {
        return useBleAbsolute;
    }
}
