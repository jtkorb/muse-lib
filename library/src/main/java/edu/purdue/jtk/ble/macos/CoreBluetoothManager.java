package edu.purdue.jtk.ble.macos;

import edu.purdue.jtk.ble.MuseBandPowerEstimator;
import edu.purdue.jtk.ble.MuseBleConstants;
import edu.purdue.jtk.ble.MuseBleDataParser;
import edu.purdue.jtk.ble.MuseNotificationSink;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages Bluetooth scanning, connection, and data streaming for a Muse headband
 * using macOS CoreBluetooth via the Objective-C runtime and JNA.
 */
public class CoreBluetoothManager {

    private static CoreBluetoothManager INSTANCE;

    private final AtomicLong centralManager = new AtomicLong(0);
    private final AtomicLong targetPeripheral = new AtomicLong(0);
    private final AtomicLong controlChar = new AtomicLong(0);
    private final AtomicLong centralDelegateClass = new AtomicLong(0);
    private final AtomicLong peripheralDelegateClass = new AtomicLong(0);
    private final AtomicBoolean startSent = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connectionEnabled = new AtomicBoolean(false);

    private long serviceUuidArray = 0;

    private final List<Callback> liveCallbacks = new ArrayList<>();
    private final Map<String, MuseBandPowerEstimator> eegBandEstimators = new HashMap<>();
    private final MuseNotificationSink notificationSink;

    public CoreBluetoothManager() {
        this(null);
    }

    public CoreBluetoothManager(MuseNotificationSink notificationSink) {
        this.notificationSink = notificationSink;
        INSTANCE = this;
    }

    /**
     * Set up the central manager and begin scanning.
     * Must be called on the Java main thread before the run loop starts.
     */
    public void start() {
        long nsObjectClass = ObjC.getClass("NSObject");

        long centralDelegateClass = buildCentralDelegateClass(nsObjectClass);
        long centralDelegate = ObjC.allocInit(centralDelegateClass);

        long cbCmClass = ObjC.getClass("CBCentralManager");
        long alloc = ObjC.msgSend(cbCmClass, ObjC.sel("alloc"));
        long cm = ObjC.msgSend(alloc, ObjC.sel("initWithDelegate:queue:"), centralDelegate, 0L);
        centralManager.set(cm);

        serviceUuidArray = buildMuseServiceUuidArray();

        System.out.println("[BLE] CBCentralManager created — waiting for Bluetooth to power on…");
    }

    public void setConnectionEnabled(boolean enabled) {
        connectionEnabled.set(enabled);

        long cm = centralManager.get();
        if (cm == 0L) {
            return;
        }

        if (enabled) {
            if (isBluetoothPoweredOn(cm) && targetPeripheral.get() == 0L) {
                ObjC.msgSend(cm, ObjC.sel("scanForPeripheralsWithServices:options:"),
                        serviceUuidArray, 0L);
            }
            return;
        }

        ObjC.msgSend(cm, ObjC.sel("stopScan"));

        long peripheral = targetPeripheral.get();
        if (peripheral != 0L) {
            ObjC.msgSend(cm, ObjC.sel("cancelPeripheralConnection:"), peripheral);
        }

        startSent.set(false);
        controlChar.set(0L);
        eegBandEstimators.clear();
    }

    private long buildCentralDelegateClass(long nsObjectClass) {
        long existing = centralDelegateClass.get();
        if (existing != 0L) {
            return existing;
        }

        String className = "JavaCBCentralDelegate_" + System.identityHashCode(this);
        long cls = ObjC.allocateClassPair(nsObjectClass, className);
        if (cls == 0L) {
            cls = ObjC.getClass(className);
            if (cls != 0L) {
                centralDelegateClass.compareAndSet(0L, cls);
                return cls;
            }
            throw new IllegalStateException("Unable to allocate central delegate class: " + className);
        }

        Callback stateUpdated = (OneArgCallback) (self, cmd, cm) -> {
            long state = ObjC.msgSend(cm, ObjC.sel("state"));
            onCentralStateUpdated(cm, state);
        };
        liveCallbacks.add(stateUpdated);
        ObjC.classAddMethod(cls, ObjC.sel("centralManagerDidUpdateState:"), stateUpdated, "v@:@");

        Callback discovered = (FourArgCallback) (self, cmd, cm, peripheral, advData, rssi) ->
                onDiscoveredPeripheral(peripheral, rssi);
        liveCallbacks.add(discovered);
        ObjC.classAddMethod(cls,
                ObjC.sel("centralManager:didDiscoverPeripheral:advertisementData:RSSI:"),
                discovered, "v@:@@@@");

        Callback connectedCallback = (TwoArgCallback) (self, cmd, cm, peripheral) ->
                onConnected(cm, peripheral);
        liveCallbacks.add(connectedCallback);
        ObjC.classAddMethod(cls,
                ObjC.sel("centralManager:didConnectPeripheral:"),
                connectedCallback, "v@:@@");

        Callback failedToConnect = (ThreeArgCallback) (self, cmd, cm, peripheral, error) -> {
            String desc = errorDescription(error);
            System.err.println("[BLE] Failed to connect: " + desc);
        };
        liveCallbacks.add(failedToConnect);
        ObjC.classAddMethod(cls,
                ObjC.sel("centralManager:didFailToConnectPeripheral:error:"),
                failedToConnect, "v@:@@@");

        Callback disconnectedCallback = (ThreeArgCallback) (self, cmd, cm, peripheral, error) -> {
            System.out.println("[BLE] Disconnected. Restarting scan…");
            INSTANCE.connected.set(false);
            INSTANCE.startSent.set(false);
            INSTANCE.targetPeripheral.set(0);
            INSTANCE.controlChar.set(0);
            INSTANCE.eegBandEstimators.clear();
            if (INSTANCE.connectionEnabled.get()) {
                ObjC.msgSend(cm, ObjC.sel("scanForPeripheralsWithServices:options:"),
                        INSTANCE.serviceUuidArray, 0L);
            }
        };
        liveCallbacks.add(disconnectedCallback);
        ObjC.classAddMethod(cls,
                ObjC.sel("centralManager:didDisconnectPeripheral:error:"),
                disconnectedCallback, "v@:@@@");

        ObjC.registerClassPair(cls);
        centralDelegateClass.compareAndSet(0L, cls);
        return cls;
    }

    private long buildPeripheralDelegateClass(long nsObjectClass) {
        long existing = peripheralDelegateClass.get();
        if (existing != 0L) {
            return existing;
        }

        String className = "JavaCBPeripheralDelegate_" + System.identityHashCode(this);
        long cls = ObjC.allocateClassPair(nsObjectClass, className);
        if (cls == 0L) {
            cls = ObjC.getClass(className);
            if (cls != 0L) {
                peripheralDelegateClass.compareAndSet(0L, cls);
                return cls;
            }
            throw new IllegalStateException("Unable to allocate peripheral delegate class: " + className);
        }

        Callback didDiscoverServices = (TwoArgCallback) (self, cmd, peripheral, error) -> {
            if (error != 0L) {
                System.err.println("[BLE] Service discovery error: " + errorDescription(error));
                return;
            }
            onDiscoveredServices(peripheral);
        };
        liveCallbacks.add(didDiscoverServices);
        ObjC.classAddMethod(cls,
                ObjC.sel("peripheral:didDiscoverServices:"),
                didDiscoverServices, "v@:@@");

        Callback didDiscoverChars = (ThreeArgCallback) (self, cmd, peripheral, service, error) -> {
            if (error != 0L) {
                System.err.println("[BLE] Characteristic discovery error: " + errorDescription(error));
                return;
            }
            onDiscoveredCharacteristics(peripheral, service);
        };
        liveCallbacks.add(didDiscoverChars);
        ObjC.classAddMethod(cls,
                ObjC.sel("peripheral:didDiscoverCharacteristicsForService:error:"),
                didDiscoverChars, "v@:@@@");

        Callback didUpdateValue = (ThreeArgCallback) (self, cmd, peripheral, characteristic, error) -> {
            if (error != 0L) {
                return;
            }
            onCharacteristicValueUpdated(characteristic);
        };
        liveCallbacks.add(didUpdateValue);
        ObjC.classAddMethod(cls,
                ObjC.sel("peripheral:didUpdateValueForCharacteristic:error:"),
                didUpdateValue, "v@:@@@");

        Callback didWrite = (ThreeArgCallback) (self, cmd, peripheral, characteristic, error) -> {
        };
        liveCallbacks.add(didWrite);
        ObjC.classAddMethod(cls,
                ObjC.sel("peripheral:didWriteValueForCharacteristic:error:"),
                didWrite, "v@:@@@");

        ObjC.registerClassPair(cls);
        peripheralDelegateClass.compareAndSet(0L, cls);
        return cls;
    }

    private void onCentralStateUpdated(long cm, long state) {
        if (state == 5) {
            if (connectionEnabled.get()) {
                System.out.println("[BLE] Bluetooth powered on — scanning for Muse devices…");
                ObjC.msgSend(cm, ObjC.sel("scanForPeripheralsWithServices:options:"),
                        serviceUuidArray, 0L);
            } else {
                System.out.println("[BLE] Bluetooth powered on — waiting for Headband button press…");
            }
        } else {
            String desc;
            switch ((int) state) {
                case 0 -> desc = "Unknown";
                case 1 -> desc = "Resetting";
                case 2 -> desc = "Unsupported";
                case 3 -> desc = "Unauthorized — grant Bluetooth permission in System Settings";
                case 4 -> desc = "Powered Off — please enable Bluetooth";
                default -> desc = "State " + state;
            }
            System.out.println("[BLE] Central manager state: " + desc);
        }
    }

    private void onDiscoveredPeripheral(long peripheral, long rssiNSNumber) {
        if (!connectionEnabled.get()) {
            return;
        }

        if (targetPeripheral.get() != 0L) {
            return;
        }

        String name = peripheralName(peripheral);
        String rssiStr = rssiNSNumber != 0L
                ? ObjC.fromNSString(ObjC.msgSend(rssiNSNumber, ObjC.sel("description")))
                : "?";

        System.out.printf("[BLE] Discovered: %-30s  RSSI: %s dBm%n",
                name != null ? name : "(unknown)", rssiStr);

        boolean isMuse = name != null && name.toLowerCase(Locale.ROOT).startsWith("muse");
        if (!isMuse) {
            return;
        }

        System.out.println("[BLE] Found Muse device \"" + name + "\" — connecting…");
        targetPeripheral.set(peripheral);

        ObjC.msgSend(peripheral, ObjC.sel("retain"));

        long cm = centralManager.get();
        ObjC.msgSend(cm, ObjC.sel("stopScan"));
        ObjC.msgSend(cm, ObjC.sel("connectPeripheral:options:"), peripheral, 0L);
    }

    private void onConnected(long cm, long peripheral) {
        connected.set(true);
        eegBandEstimators.clear();
        System.out.println("[BLE] Connected to Muse — discovering services…");

        long nsObjectClass = ObjC.getClass("NSObject");
        long periDelegateClass = buildPeripheralDelegateClass(nsObjectClass);
        long periDelegate = ObjC.allocInit(periDelegateClass);
        ObjC.msgSend(peripheral, ObjC.sel("setDelegate:"), periDelegate);

        long serviceUuid = cbuuid(MuseBleConstants.SERVICE_UUID);
        long serviceArray = ObjC.nsArrayOf(serviceUuid);
        ObjC.msgSend(peripheral, ObjC.sel("discoverServices:"), serviceArray);
    }

    private void onDiscoveredServices(long peripheral) {
        long services = ObjC.msgSend(peripheral, ObjC.sel("services"));
        long count = ObjC.msgSend(services, ObjC.sel("count"));
        System.out.println("[BLE] Services discovered: " + count);

        for (long i = 0; i < count; i++) {
            long service = ObjC.msgSend(services, ObjC.sel("objectAtIndex:"), i);
            String uuid = cbuuidString(ObjC.msgSend(service, ObjC.sel("UUID")));
            System.out.println("[BLE]   Service: " + uuid);
            ObjC.msgSend(peripheral, ObjC.sel("discoverCharacteristics:forService:"), 0L, service);
        }
    }

    private void onDiscoveredCharacteristics(long peripheral, long service) {
        long chars = ObjC.msgSend(service, ObjC.sel("characteristics"));
        long count = ObjC.msgSend(chars, ObjC.sel("count"));
        System.out.println("[BLE] Characteristics in service: " + count);

        for (long i = 0; i < count; i++) {
            long ch = ObjC.msgSend(chars, ObjC.sel("objectAtIndex:"), i);
            String id = charUuidString(ch);
            System.out.println("[BLE]   Characteristic: " + id);

            switch (id.toUpperCase(Locale.ROOT)) {
                case MuseBleConstants.CHAR_CONTROL -> {
                    controlChar.set(ch);
                    System.out.println("[BLE]     → control channel found");
                }
                case MuseBleConstants.CHAR_EEG_TP9,
                     MuseBleConstants.CHAR_EEG_AF7,
                     MuseBleConstants.CHAR_EEG_AF8,
                     MuseBleConstants.CHAR_EEG_TP10,
                     MuseBleConstants.CHAR_EEG_AUX,
                     MuseBleConstants.CHAR_ACCEL,
                     MuseBleConstants.CHAR_GYRO,
                     MuseBleConstants.CHAR_BATTERY -> {
                    ObjC.msgSend(peripheral,
                            ObjC.sel("setNotifyValue:forCharacteristic:"), 1L, ch);
                    System.out.println("[BLE]     → subscribed to notifications");
                }
                default -> {
                }
            }
        }

        maybeSendStart(peripheral);
    }

    private void maybeSendStart(long peripheral) {
        if (startSent.get()) {
            return;
        }
        long ctrl = controlChar.get();
        if (ctrl == 0L) {
            return;
        }
        if (!startSent.compareAndSet(false, true)) {
            return;
        }

        System.out.println("[BLE] Sending START command to Muse…");
        writeCommand(peripheral, ctrl, MuseBleConstants.CMD_PRESET_P20);
        writeCommand(peripheral, ctrl, MuseBleConstants.CMD_START);
        System.out.println("[BLE] Streaming started. Waiting for data…");
    }

    private void onCharacteristicValueUpdated(long characteristic) {
        String id = charUuidString(characteristic);
        byte[] data = readNsData(ObjC.msgSend(characteristic, ObjC.sel("value")));
        if (data == null || data.length == 0) {
            return;
        }

        if (notificationSink != null) {
            notificationSink.onNotification(id, data);
        }

        switch (id.toUpperCase(Locale.ROOT)) {
            case MuseBleConstants.CHAR_EEG_TP9 -> logEeg("TP9 ", data);
            case MuseBleConstants.CHAR_EEG_AF7 -> logEeg("AF7 ", data);
            case MuseBleConstants.CHAR_EEG_AF8 -> logEeg("AF8 ", data);
            case MuseBleConstants.CHAR_EEG_TP10 -> logEeg("TP10", data);
            case MuseBleConstants.CHAR_EEG_AUX -> logEeg("AUX ", data);
            case MuseBleConstants.CHAR_ACCEL -> printImu("ACCEL", data, false);
            case MuseBleConstants.CHAR_GYRO -> printImu("GYRO ", data, true);
            case MuseBleConstants.CHAR_BATTERY ->
                    System.out.printf("[DATA] BATT  raw: %s%n", MuseBleDataParser.hexDump(data));
            default ->
                    System.out.printf("[DATA] %s  raw: %s%n", id, MuseBleDataParser.hexDump(data));
        }
    }

    private void logEeg(String channel, byte[] data) {
        MuseBleDataParser.EegPacket pkt = MuseBleDataParser.parseEeg(data);
        if (pkt == null) {
            return;
        }

        System.out.printf("[DATA] EEG  %s  %s%n", channel, pkt);

        String channelId = channel.trim();
        if (!isPrimaryEegChannel(channelId)) {
            return;
        }

        MuseBandPowerEstimator estimator = eegBandEstimators.computeIfAbsent(
                channelId,
                ignored -> new MuseBandPowerEstimator(256.0, 512, 96)
        );

        MuseBandPowerEstimator.BandPower bands = estimator.addSamples(pkt.samplesUv);
        if (bands == null) {
            return;
        }

        System.out.printf(
                Locale.ROOT,
                "[BAND] EEG  %-4s  d %.1f%%  t %.1f%%  a %.1f%%  b %.1f%%  g %.1f%%%n",
                channelId,
                bands.relDelta() * 100.0,
                bands.relTheta() * 100.0,
                bands.relAlpha() * 100.0,
                bands.relBeta() * 100.0,
                bands.relGamma() * 100.0
        );
    }

    private static boolean isPrimaryEegChannel(String channelId) {
        return "TP9".equals(channelId)
                || "AF7".equals(channelId)
                || "AF8".equals(channelId)
                || "TP10".equals(channelId);
    }

    private static void printImu(String label, byte[] data, boolean isGyro) {
        MuseBleDataParser.ImuPacket pkt = MuseBleDataParser.parseImu(data, isGyro);
        if (pkt != null) {
            System.out.printf("[DATA] %-6s  %s%n", label, pkt);
        }
    }

    private static void writeCommand(long peripheral, long characteristic, byte[] cmd) {
        long nsData = ObjC.nsData(cmd);
        ObjC.msgSend(peripheral,
                ObjC.sel("writeValue:forCharacteristic:type:"),
                nsData, characteristic,
                MuseBleConstants.WRITE_WITHOUT_RESPONSE);
    }

    private static long cbuuid(String uuidStr) {
        long cls = ObjC.getClass("CBUUID");
        long nsStr = ObjC.nsString(uuidStr);
        return ObjC.msgSend(cls, ObjC.sel("UUIDWithString:"), nsStr);
    }

    private static String cbuuidString(long cbuuid) {
        if (cbuuid == 0L) {
            return "";
        }
        long nsStr = ObjC.msgSend(cbuuid, ObjC.sel("UUIDString"));
        String value = ObjC.fromNSString(nsStr);
        return value != null ? value.toUpperCase(Locale.ROOT) : "";
    }

    private static String charUuidString(long characteristic) {
        return cbuuidString(ObjC.msgSend(characteristic, ObjC.sel("UUID")));
    }

    private static String peripheralName(long peripheral) {
        long nsName = ObjC.msgSend(peripheral, ObjC.sel("name"));
        return ObjC.fromNSString(nsName);
    }

    private static byte[] readNsData(long nsData) {
        if (nsData == 0L) {
            return null;
        }
        long length = ObjC.msgSend(nsData, ObjC.sel("length"));
        if (length <= 0) {
            return new byte[0];
        }
        long bytesPtr = ObjC.msgSend(nsData, ObjC.sel("bytes"));
        if (bytesPtr == 0L) {
            return null;
        }
        return new Pointer(bytesPtr).getByteArray(0, (int) length);
    }

    private static String errorDescription(long error) {
        if (error == 0L) {
            return "(null)";
        }
        long desc = ObjC.msgSend(error, ObjC.sel("localizedDescription"));
        String value = ObjC.fromNSString(desc);
        return value != null ? value : "(unknown)";
    }

    private static long buildMuseServiceUuidArray() {
        return ObjC.nsArrayOf(cbuuid(MuseBleConstants.SERVICE_UUID));
    }

    private static boolean isBluetoothPoweredOn(long cm) {
        return ObjC.msgSend(cm, ObjC.sel("state")) == 5L;
    }

    @FunctionalInterface
    interface OneArgCallback extends Callback {
        void invoke(long self, long cmd, long a);
    }

    @FunctionalInterface
    interface TwoArgCallback extends Callback {
        void invoke(long self, long cmd, long a, long b);
    }

    @FunctionalInterface
    interface ThreeArgCallback extends Callback {
        void invoke(long self, long cmd, long a, long b, long c);
    }

    @FunctionalInterface
    interface FourArgCallback extends Callback {
        void invoke(long self, long cmd, long a, long b, long c, long d);
    }
}
