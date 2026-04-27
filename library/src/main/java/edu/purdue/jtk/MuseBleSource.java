package edu.purdue.jtk;

import edu.purdue.jtk.ble.MuseBandPowerEstimator;
import edu.purdue.jtk.ble.MuseBleConstants;
import edu.purdue.jtk.ble.MuseBleDataParser;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * BLE-backed source adapter that accepts raw Muse BLE notifications and updates
 * the library model using relative band powers.
 */
public class MuseBleSource extends MuseSource {

    private static final double SAMPLE_RATE_HZ = 256.0;
    private static final int WINDOW_SIZE = 512;
    private static final int HOP_SIZE = 96;

    private final Map<Sensor, MuseBandPowerEstimator> estimators = new EnumMap<>(Sensor.class);

    MuseBleSource(Model model, MuseStatistics ms) {
        super(model, ms);
        for (Sensor sensor : Sensor.values()) {
            estimators.put(sensor, new MuseBandPowerEstimator(SAMPLE_RATE_HZ, WINDOW_SIZE, HOP_SIZE));
        }
    }

    @Override
    void dispatchMessage(String address, MuseMessage mm) {
        if (address == null || mm == null) {
            return;
        }

        Object[] args = mm.arguments();

        // Direct BLE route: address is the EEG characteristic UUID.
        byte[] directPayload = extractPayload(args);
        if (sensorFromCharacteristic(address) != null && directPayload != null) {
            ms.countAddress(address, mm.getTimestamp());
            onNotification(address, directPayload);
            return;
        }

        // Wrapped BLE route: address is "eeg" and args are [characteristicUuid, payload].
        if ("eeg".equalsIgnoreCase(address) && args.length >= 2 && args[0] instanceof String) {
            String characteristicUuid = (String) args[0];
            byte[] wrappedPayload = extractPayload(new Object[] { args[1] });
            if (sensorFromCharacteristic(characteristicUuid) != null && wrappedPayload != null) {
                ms.countAddress(address, mm.getTimestamp());
                onNotification(characteristicUuid, wrappedPayload);
                return;
            }
        }

        super.dispatchMessage(address, mm);
    }

    /**
     * Handle one raw BLE notification by characteristic UUID and value bytes.
     */
    public void onNotification(String characteristicUuid, byte[] data) {
        if (characteristicUuid == null || data == null || data.length == 0) {
            return;
        }
        Sensor sensor = sensorFromCharacteristic(characteristicUuid);
        if (sensor == null) {
            return;
        }

        MuseBleDataParser.EegPacket packet = MuseBleDataParser.parseEeg(data);
        if (packet == null) {
            return;
        }

        MuseBandPowerEstimator.BandPower bands = estimators.get(sensor).addSamples(packet.samplesUv);
        if (bands == null) {
            return;
        }

        long now = System.currentTimeMillis();
        model.setGrid(Wave.DELTA.value, sensor.value, (float) bands.relDelta(), now);
        model.setGrid(Wave.THETA.value, sensor.value, (float) bands.relTheta(), now);
        model.setGrid(Wave.ALPHA.value, sensor.value, (float) bands.relAlpha(), now);
        model.setGrid(Wave.BETA.value, sensor.value, (float) bands.relBeta(), now);
        model.setGrid(Wave.GAMMA.value, sensor.value, (float) bands.relGamma(), now);
        model.setTouchingForehead(true);
    }

    private static Sensor sensorFromCharacteristic(String characteristicUuid) {
        String id = characteristicUuid.toUpperCase(Locale.ROOT);
        return switch (id) {
            case MuseBleConstants.CHAR_EEG_TP9 -> Sensor.LEFT_EAR;
            case MuseBleConstants.CHAR_EEG_AF7 -> Sensor.LEFT_FH;
            case MuseBleConstants.CHAR_EEG_AF8 -> Sensor.RIGHT_FH;
            case MuseBleConstants.CHAR_EEG_TP10 -> Sensor.RIGHT_EAR;
            default -> null;
        };
    }

    private static byte[] extractPayload(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        if (args.length == 1) {
            Object value = args[0];
            if (value instanceof byte[]) {
                return (byte[]) value;
            }
            if (value instanceof Byte[]) {
                Byte[] boxed = (Byte[]) value;
                byte[] bytes = new byte[boxed.length];
                for (int i = 0; i < boxed.length; i++) {
                    bytes[i] = boxed[i] == null ? 0 : boxed[i];
                }
                return bytes;
            }
            if (value instanceof Number[]) {
                Number[] numbers = (Number[]) value;
                byte[] bytes = new byte[numbers.length];
                for (int i = 0; i < numbers.length; i++) {
                    bytes[i] = numbers[i].byteValue();
                }
                return bytes;
            }
        }

        boolean allNumbers = true;
        for (Object arg : args) {
            if (!(arg instanceof Number)) {
                allNumbers = false;
                break;
            }
        }
        if (!allNumbers) {
            return null;
        }

        byte[] bytes = new byte[args.length];
        for (int i = 0; i < args.length; i++) {
            bytes[i] = ((Number) args[i]).byteValue();
        }
        return bytes;
    }
}
