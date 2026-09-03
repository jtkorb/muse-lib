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
    private static final int EEG_SAMPLES_PER_PACKET = 12;
    private static final long LOSS_LOG_INTERVAL_MS = 10_000L;

    private final Map<Sensor, MuseBandPowerEstimator> estimators = new EnumMap<>(Sensor.class);
    private final MuseElectrodeQualityEstimator electrodeQuality = new MuseElectrodeQualityEstimator();
    private final Map<Sensor, SequenceStats> sequenceStats = new EnumMap<>(Sensor.class);
    private long nextLossLogAtMs = System.currentTimeMillis() + LOSS_LOG_INTERVAL_MS;

    MuseBleSource(Model model, MuseStatistics ms) {
        super(model, ms);
        for (Sensor sensor : Sensor.values()) {
            estimators.put(sensor, new MuseBandPowerEstimator(SAMPLE_RATE_HZ, WINDOW_SIZE, HOP_SIZE));
            sequenceStats.put(sensor, new SequenceStats());
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
        sequenceStats.get(sensor).record(packet.sequence);
        maybeLogLossStats();

        processClassicElectrode(sensor, packet.samplesUv);
    }

    private void processClassicElectrode(Sensor sensor, double[] samplesUv) {
        ingestElectrodeOnly(sensor, samplesUv);
        electrodeQuality.applyToModel(model);
    }

    private void ingestElectrodeOnly(Sensor sensor, double[] samplesUv) {
        electrodeQuality.ingest(sensor.value, samplesUv);
        MuseBandPowerEstimator.BandPower bands = estimators.get(sensor).addSamples(samplesUv);
        applyBandGrid(sensor, bands);
    }

    private void applyBandGrid(Sensor sensor, MuseBandPowerEstimator.BandPower bands) {
        if (bands == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (model.getUseBleAbsolute()) {
            // Use log-transformed absolute band powers, matching the Internet/OSC path scale.
            model.setGrid(Wave.DELTA.value, sensor.value, (float) Math.log10(Math.max(bands.delta(), 1e-10)), now);
            model.setGrid(Wave.THETA.value, sensor.value, (float) Math.log10(Math.max(bands.theta(), 1e-10)), now);
            model.setGrid(Wave.ALPHA.value, sensor.value, (float) Math.log10(Math.max(bands.alpha(), 1e-10)), now);
            model.setGrid(Wave.BETA.value, sensor.value, (float) Math.log10(Math.max(bands.beta(), 1e-10)), now);
            model.setGrid(Wave.GAMMA.value, sensor.value, (float) Math.log10(Math.max(bands.gamma(), 1e-10)), now);
        } else {
            model.setGrid(Wave.DELTA.value, sensor.value, (float) bands.relDelta(), now);
            model.setGrid(Wave.THETA.value, sensor.value, (float) bands.relTheta(), now);
            model.setGrid(Wave.ALPHA.value, sensor.value, (float) bands.relAlpha(), now);
            model.setGrid(Wave.BETA.value, sensor.value, (float) bands.relBeta(), now);
            model.setGrid(Wave.GAMMA.value, sensor.value, (float) bands.relGamma(), now);
        }
    }

    private static Sensor sensorFromCharacteristic(String characteristicUuid) {
        String id = normalizeBleUuid(characteristicUuid);
        if (id.equals(normalizeBleUuid(MuseBleConstants.CHAR_EEG_TP9))) {
            return Sensor.LEFT_EAR;
        }
        if (id.equals(normalizeBleUuid(MuseBleConstants.CHAR_EEG_AF7))) {
            return Sensor.LEFT_FH;
        }
        if (id.equals(normalizeBleUuid(MuseBleConstants.CHAR_EEG_AF8))) {
            return Sensor.RIGHT_FH;
        }
        if (id.equals(normalizeBleUuid(MuseBleConstants.CHAR_EEG_TP10))) {
            return Sensor.RIGHT_EAR;
        }
        return null;
    }

    private static String normalizeBleUuid(String dashedUuid) {
        return dashedUuid.replace("-", "").toUpperCase(Locale.ROOT);
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

    private void maybeLogLossStats() {
        long now = System.currentTimeMillis();
        if (now < nextLossLogAtMs) {
            return;
        }
        nextLossLogAtMs = now + LOSS_LOG_INTERVAL_MS;

        StringBuilder sb = new StringBuilder("[BLE] EEG loss summary:");
        for (Sensor sensor : Sensor.values()) {
            SequenceStats stats = sequenceStats.get(sensor);
            sb.append(String.format(
                    Locale.ROOT,
                    " %s packets=%d missing=%d samples=%d loss=%.3f%%;",
                    sensor.getName(),
                    stats.totalPackets,
                    stats.missingPackets,
                    stats.totalSamples(),
                    stats.sampleLossPercent()
            ));
        }
        System.out.println(sb);
    }

    private static final class SequenceStats {
        private Integer lastSeq;
        private long totalPackets;
        private long missingPackets;

        void record(int sequence) {
            totalPackets++;
            if (lastSeq != null) {
                int expected = (lastSeq + 1) & 0xFFFF;
                int delta = (sequence - expected) & 0xFFFF;
                if (delta > 0) {
                    missingPackets += delta;
                }
            }
            lastSeq = sequence & 0xFFFF;
        }

        long totalSamples() {
            return totalPackets * EEG_SAMPLES_PER_PACKET;
        }

        double sampleLossPercent() {
            long delivered = totalSamples();
            long missing = missingPackets * EEG_SAMPLES_PER_PACKET;
            long expected = delivered + missing;
            if (expected == 0) {
                return 0.0;
            }
            return 100.0 * missing / (double) expected;
        }
    }
}
