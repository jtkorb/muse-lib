package edu.purdue.jtk.ble;

/**
 * Receives raw BLE notifications from a device-specific BLE transport.
 */
@FunctionalInterface
public interface MuseNotificationSink {
    void onNotification(String characteristicUuid, byte[] data);
}
