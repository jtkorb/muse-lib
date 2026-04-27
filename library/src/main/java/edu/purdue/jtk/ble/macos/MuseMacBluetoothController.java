package edu.purdue.jtk.ble.macos;

import edu.purdue.jtk.Muse;
import edu.purdue.jtk.ble.MuseNotificationSink;

/**
 * High-level bridge that wires the library's Muse BLE source to the macOS
 * CoreBluetooth transport.
 */
public final class MuseMacBluetoothController {

    private final Muse muse;
    private final CoreBluetoothManager manager;
    private volatile boolean headbandPressed;

    public MuseMacBluetoothController() {
        this(new Muse());
    }

    public MuseMacBluetoothController(Muse muse) {
        if (muse == null) {
            throw new IllegalArgumentException("muse cannot be null");
        }
        this.muse = muse;
        this.manager = new CoreBluetoothManager(createMuseNotificationSink(muse));
    }

    public Muse getMuse() {
        return muse;
    }

    public void start() {
        ensureMacOs();
        manager.start();
        manager.setConnectionEnabled(headbandPressed);
    }

    public void setHeadbandPressed(boolean pressed) {
        headbandPressed = pressed;
        manager.setConnectionEnabled(pressed);
    }

    public boolean isHeadbandPressed() {
        return headbandPressed;
    }

    public void runUntilStopped() {
        ObjC.cfRunLoopRunUntilStopped();
    }

    public void stop() {
        ObjC.cfRunLoopStop();
    }

    public void startAndRunUntilStopped() {
        start();
        runUntilStopped();
    }

    private static MuseNotificationSink createMuseNotificationSink(Muse muse) {
        return muse::onBleNotification;
    }

    private static void ensureMacOs() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            throw new IllegalStateException("This Bluetooth transport requires macOS (CoreBluetooth).");
        }
    }
}
