package edu.purdue.jtk;

import java.io.File;

/**
 * Muse is the primary class for receiving data from the Muse headband.
 *
 * @author Tim Korb
 * @since 2.0.0
 */
public class Muse {
    /**
     * Wave processing done by the Muse Direct app.
     * <ul>
     * <li>SESSION_SCORE: Scaled ([0..1]) value of band power based on recent history.</li>
     * <li>ABSOLUTE: Absolute band power; values are negative and greater than one, but auto-scaled to [0..1].</li>
     * <li>RELATIVE: Band power relative to other bands.  Range is (0..1); all bands vector sum to 0.</li>
     * </ul>
     */

    MuseStatistics ms = new MuseStatistics();
    Model model = new Model(ms);
    MuseSource source;
    MuseControl mc;
    private boolean isPaused = false;

    public Muse(Generate generate) {
        // NB: The generate argument is ignored and the mc radio button is not being set.  Deprecate this method?
        source = new MuseGenerator(generate, model, ms);
        mc = new MuseControl(this);
    }

    /**
     * Class constructor, waits for source from control panel.
     */
    public Muse() {
        source = null;
        mc = new MuseControl(this, false);
    }

    /**
     * Class constructor, used for testing.
     */
    public Muse(boolean testing) {
        source = null;
        mc = new MuseControl(this, testing);
    }

    /**
     * Class constructor, listens on specified port.
     *
     * @param port an int giving the UDP port number to listen for connection from the Muse Direct app
     */
    public Muse(int port) {
        // NB: Deprecate this method or figure out how to initialize a source on startup.
        // source = new MuseListener(port, model, ms);
        source = null;
        mc = new MuseControl(this);
        mc.waitForStartup();
    }

    /**
     * Class constructor, reads a saved Muse session from the named JSON file.
     *
     * @param fileName the String name of the file
     */
    public Muse(String fileName) {
        source = new MuseFileReader(fileName, model, ms);
        mc = new MuseControl(this);
    }

    /**
     * Class constructor, reads a saved Muse session from the given JSON File object.
     *
     * @param file the File object identifying the JSON file
     */
    public Muse(File file) {
        source = new MuseFileReader(file.getName(), model, ms);
        mc = new MuseControl(this);
    }

    public MuseControl getMuseControl() {
        mc.waitForStartup();
        return mc;
    }

    private PointVector[] computeSensorVector() {
        PointVector[] pvSensors = new PointVector[Sensor.values().length];
        pvSensors[Sensor.LEFT_EAR.value] = new PointVector(0, 0);
        pvSensors[Sensor.LEFT_FH.value] = new PointVector(0, 0);
        pvSensors[Sensor.RIGHT_FH.value] = new PointVector(0, 0);
        pvSensors[Sensor.RIGHT_EAR.value] = new PointVector(0, 0);

        for (Wave wave : Wave.values()) {
            float leftFH = model.getGrid(wave, Sensor.LEFT_FH);
            float leftEar = model.getGrid(wave, Sensor.LEFT_EAR);
            float rightFH = model.getGrid(wave, Sensor.RIGHT_FH);
            float rightEar = model.getGrid(wave, Sensor.RIGHT_EAR);

            pvSensors[Sensor.LEFT_EAR.value].add(-leftEar, -leftEar);
            pvSensors[Sensor.LEFT_FH.value].add(-leftFH, leftFH);
            pvSensors[Sensor.RIGHT_FH.value].add(rightFH, rightFH);
            pvSensors[Sensor.RIGHT_EAR.value].add(rightEar, -rightEar);
        }

        for (PointVector pv : pvSensors) {
            pv.x /= 5;
            pv.y /= 5;
        }
        return pvSensors;
    }

    /**
     * Returns the "focus" of the waves currently being sent by the Muse headband, and selected by the user.
     *
     * @return a PointVector with magnitude [0..sqrt(2)] indicating strength and direction toward highest activity brain area
     */
    public PointVector computeFocus() {
        PointVector pvFocus = new PointVector(0, 0);

        int c = 0;
        for (Wave wave : Wave.values())
            if (mc.getShowWave(wave)) {
                PointVector pvWave = computeWaveVector(wave);
                pvFocus.add(pvWave.x, pvWave.y);
                c++;
            }

        if (c > 0) {
            pvFocus.x /= c;
            pvFocus.y /= c;
        }

        ms.trackFocus(pvFocus);

        return pvFocus;
    }

    public PointVector computeWaveVector(Wave wave) {
        PointVector pv = new PointVector(0, 0);

        float leftFH = mc.getShowSensor(Sensor.LEFT_FH) ? model.getGrid(wave, Sensor.LEFT_FH) : 0F;
        float leftEar = mc.getShowSensor(Sensor.LEFT_EAR) ? model.getGrid(wave, Sensor.LEFT_EAR) : 0F;
        float rightFH = mc.getShowSensor(Sensor.RIGHT_FH) ? model.getGrid(wave, Sensor.RIGHT_FH) : 0F;
        float rightEar = mc.getShowSensor(Sensor.RIGHT_EAR) ? model.getGrid(wave, Sensor.RIGHT_EAR) : 0F;

        pv.add(-leftEar, -leftEar);
        pv.add(-leftFH, leftFH);
        pv.add(rightFH, rightFH);
        pv.add(rightEar, -rightEar);

        // Scale to ensure value remains in range [0..1]...
        pv.x /= 2;
        pv.y /= 2;

        return pv;
    }

    public float computeActivity() {
        int c = 0;
        float value = 0;

        for (Wave wave : Wave.values()) {
            if (mc.getShowWave(wave))
                for (Sensor sensor : Sensor.values())
                    if (mc.getShowSensor(sensor)) {
                        value += model.getGrid(wave, sensor);
                        c++;
                    }
        }

        return (c == 0) ? 0 : value / c;
    }

    public float getGrid(Wave wave, Sensor sensor) {
        return model.getGrid(wave, sensor);
    }

    public float[] getWaveAtSensors(Wave wave) {
        return model.getWaveAtSensors(wave);
    }

    public void setGenerate(Generate generate) {
        if (source instanceof MuseGenerator)
            ((MuseGenerator) source).setGenerate(generate);
    }

//    public void resetScaling() {
//        model.resetScaling();
//    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean noSource() {
        return source == null;
    }

    public void clearSource() {
        if (source != null) {
            source.shutdown();
            model.resetGrid();
            source = null;
        }
    }

    public void setHeadbandSource() {
        assert model != null;
        assert ms != null;

        if (source != null) {
            source.shutdown();
            model.resetGrid();
        }
        // NB: Reset model and ms?
        model.resetGrid();  // NB: Partial fix for the race condition below.
        source = new MuseListener(8000, model, ms);
    }

    public void setGeneratorSource() {
        assert model != null;
        assert ms != null;

        if (source != null) {
            source.shutdown();
            model.resetGrid();  // NB: Race condition--the generator drops a few into the grid after it is reset.
        }

        // NB: Reset model and ms?
        source = new MuseGenerator(Generate.Unfocused, model, ms);
    }

    public void setFileSource() {
        assert model != null;
        assert ms != null;

        if (source != null) {
            source.shutdown();
            model.resetGrid();
        }

        // NB: Reset model and ms?
        System.out.println("Setting file source not implemented yet.");
    }

    public void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    public float getHorseshoe(Sensor sensor) {
        return model.getHorseshoe(sensor);
    }

    public int isGood(Sensor sensor) {
        return model.isGood(sensor);
    }
}
