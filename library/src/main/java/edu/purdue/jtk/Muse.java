package edu.purdue.jtk;

import edu.purdue.jtk.ble.macos.MuseMacBluetoothController;

import java.io.File;

/**
 * Muse is the primary class for receiving data from the Muse headband.
 *
 * @author Tim Korb
 * @since 1.0.0
 */
public class Muse {
    @FunctionalInterface
    public interface SourceSelectionListener {
        void onHeadbandSelected(boolean selected);
    }

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
    private SourceSelectionListener sourceSelectionListener;
    private final boolean testingMode;

    private final Object bleLifecycleLock = new Object();
    private MuseMacBluetoothController bleController;
    private Thread bleRunLoopThread;

    public Muse(Generate generate) {
        // NB: The generate argument is ignored and the mc radio button is not being set.  Deprecate this method?
        source = new MuseGenerator(generate, model, ms);
        mc = new MuseControl(this);
        testingMode = false;
    }

    /**
     * Class constructor, waits for source from control panel.
     */
    public Muse() {
        source = null;
        mc = new MuseControl(this, false);
        testingMode = false;
    }

    /**
     * Class constructor, used for testing.
     */
    public Muse(boolean testing) {
        source = null;
        mc = new MuseControl(this, testing);
        testingMode = testing;
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
        testingMode = false;
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
        testingMode = false;
    }

    /**
     * Class constructor, reads a saved Muse session from the given JSON File object.
     *
     * @param file the File object identifying the JSON file
     */
    public Muse(File file) {
        source = new MuseFileReader(file.getName(), model, ms);
        mc = new MuseControl(this);
        testingMode = false;
    }

    public MuseControl getMuseControl() {
        mc.waitForStartup();
        return mc;
    }

    @SuppressWarnings("unused")
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

        // Ensure division by multiplier in loop below is correct.  
        // TODO: Fix with symbolic expression.
        assert Wave.values().length == 5;

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

        boolean showLeftFH = mc.getShowSensor(Sensor.LEFT_FH);
        boolean showLeftEar = mc.getShowSensor(Sensor.LEFT_EAR);
        boolean showRightFH = mc.getShowSensor(Sensor.RIGHT_FH);
        boolean showRightEar = mc.getShowSensor(Sensor.RIGHT_EAR);

        // float leftFH = mc.getShowSensor(Sensor.LEFT_FH) ? model.getGrid(wave, Sensor.LEFT_FH) : 0F;
        // float leftEar = mc.getShowSensor(Sensor.LEFT_EAR) ? model.getGrid(wave, Sensor.LEFT_EAR) : 0F;
        // float rightFH = mc.getShowSensor(Sensor.RIGHT_FH) ? model.getGrid(wave, Sensor.RIGHT_FH) : 0F;
        // float rightEar = mc.getShowSensor(Sensor.RIGHT_EAR) ? model.getGrid(wave, Sensor.RIGHT_EAR) : 0F;
        
        float leftFH, leftEar, rightFH, rightEar;
        float leftFHDrawn, leftEarDrawn, rightFHDrawn, rightEarDrawn;

        boolean NEW_WAY = false;

        // Compute "drawn" first to avoid recalculation when calling "grid".
        leftFHDrawn = model.getDrawn(wave, Sensor.LEFT_FH);
        leftEarDrawn = model.getDrawn(wave, Sensor.LEFT_EAR);
        rightFHDrawn = model.getDrawn(wave, Sensor.RIGHT_FH);
        rightEarDrawn = model.getDrawn(wave, Sensor.RIGHT_EAR);

        leftFH = showLeftFH ? model.getGrid(wave, Sensor.LEFT_FH) : 0;
        leftEar = showLeftEar ? model.getGrid(wave, Sensor.LEFT_EAR) : 0;
        rightFH = showRightFH ? model.getGrid(wave, Sensor.RIGHT_FH) : 0;
        rightEar = showRightEar ? model.getGrid(wave, Sensor.RIGHT_EAR) : 0;

        if (NEW_WAY) {
            float multiplier = 100;
            leftFH = multiplier * Math.abs(leftFH - leftFHDrawn);
            leftEar = multiplier * Math.abs(leftEar - leftEarDrawn);
            rightFH = multiplier * Math.abs(rightFH - rightFHDrawn);
            rightEar = multiplier * Math.abs(rightEar - rightEarDrawn);
        }

        pv.add(-leftEar, -leftEar);
        pv.add(-leftFH, leftFH);
        pv.add(rightFH, rightFH);
        pv.add(rightEar, -rightEar);

        // Scale to ensure value remains in range [0..1]...
        pv.x /= 2;
        pv.y /= 2;

        return pv;
    }

    /**
     * Returns a measure of the overall brain activity by adding the values of all shown waves and sensors.
     * 
     * @return  average value of all shown sensors
     */
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

    // public float[] getWaveAtSensors(Wave wave) {
    //     return model.getWaveAtSensors(wave);
    // }

    public void setGenerate(Generate generate) {
        if (source instanceof MuseGenerator)
            ((MuseGenerator) source).setGenerate(generate);
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean noSource() {
        return source == null;
    }

    public void clearSource() {
        stopBleControllerIfRunning();
        if (source != null) {
            source.shutdown();
            model.reset();
            source = null;
        }
        notifyHeadbandSelected(false);
    }

    /**
     * Sets a BLE-backed source that receives raw notification bytes and updates
     * the model directly. This is intended for direct Bluetooth integrations
     * where OSC is not used.
     *
     * @return the created BLE source for wiring notification callbacks
     */
    public MuseBleSource setBleSource() {
        assert model != null;
        assert ms != null;

        if (source != null) {
            source.shutdown();
            model.reset();
        }

        model.reset();
        MuseBleSource bleSource = new MuseBleSource(model, ms);
        source = bleSource;
        startBleControllerIfNeeded();
        notifyHeadbandSelected(true);
        return bleSource;
    }

    public void setHeadbandSource() {
        assert model != null;
        assert ms != null;

        stopBleControllerIfRunning();
        if (source != null) {
            source.shutdown();
            model.reset();
        }
        // NB: Reset model and ms?
        model.reset();  // NB: Partial fix for the race condition below.
        source = new MuseListener(8000, model, ms);
        notifyHeadbandSelected(true);
    }

    public void setGeneratorSource() {
        assert model != null;
        assert ms != null;

        stopBleControllerIfRunning();
        if (source != null) {
            source.shutdown();
            model.reset();  // NB: Race condition--the generator drops a few into the grid after it is reset.
        }

        // NB: Reset model and ms?
        source = new MuseGenerator(Generate.Unfocused, model, ms);
        notifyHeadbandSelected(false);
    }

    public void setFileSource() {
        assert model != null;
        assert ms != null;

        stopBleControllerIfRunning();
        if (source != null) {
            source.shutdown();
            model.reset();
        }

        // NB: Reset model and ms?
        System.out.println("Setting file source not implemented yet.");
        notifyHeadbandSelected(false);
    }

    public void setSourceSelectionListener(SourceSelectionListener sourceSelectionListener) {
        this.sourceSelectionListener = sourceSelectionListener;
    }

    private void notifyHeadbandSelected(boolean selected) {
        if (sourceSelectionListener != null) {
            sourceSelectionListener.onHeadbandSelected(selected);
        }
    }

    public void onBleNotification(String characteristicUuid, byte[] data) {
        if (source instanceof MuseBleSource) {
            ((MuseBleSource) source).onNotification(characteristicUuid, data);
        }
    }

    private void startBleControllerIfNeeded() {
        if (testingMode) {
            return;
        }

        synchronized (bleLifecycleLock) {
            if (bleRunLoopThread != null && bleRunLoopThread.isAlive()) {
                if (bleController != null) {
                    bleController.setHeadbandPressed(true);
                }
                return;
            }

            bleController = new MuseMacBluetoothController(this);
            MuseMacBluetoothController controller = bleController;
            controller.setHeadbandPressed(true);
            bleRunLoopThread = new Thread(() -> {
                try {
                    controller.start();
                    controller.runUntilStopped();
                } catch (RuntimeException runtimeException) {
                    System.err.println("[Muse] BLE controller stopped with error: " + runtimeException.getMessage());
                }
            }, "muse-ble-runloop");
            bleRunLoopThread.setDaemon(true);
            bleRunLoopThread.start();
        }
    }

    private void stopBleControllerIfRunning() {
        if (testingMode) {
            return;
        }

        synchronized (bleLifecycleLock) {
            Thread runLoopThread = bleRunLoopThread;
            if (bleController != null) {
                bleController.setHeadbandPressed(false);
                bleController.stop();
            }
            if (runLoopThread != null && runLoopThread.isAlive()) {
                try {
                    runLoopThread.join(300L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            bleController = null;
            bleRunLoopThread = null;
        }
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
