package edu.purdue.jtk.ble;

/**
 * Parses raw Muse BLE notification payloads into EEG and IMU packets.
 */
public final class MuseBleDataParser {

    private MuseBleDataParser() {
    }

    public static EegPacket parseEeg(byte[] data) {
        if (data == null || data.length < 20) {
            return null;
        }
        int seq = uint16(data, 0);
        double[] samples = new double[12];
        for (int i = 0; i < 12; i++) {
            samples[i] = (unpack12bit(data, 2, i) - 0x800) * MuseBleConstants.EEG_SCALE_UV;
        }
        return new EegPacket(seq, samples);
    }

    public static ImuPacket parseImu(byte[] data, boolean isGyro) {
        if (data == null || data.length < 20) {
            return null;
        }
        int seq = uint16(data, 0);
        ImuSample[] samples = new ImuSample[3];
        double scale = isGyro ? MuseBleConstants.GYRO_SCALE_DEG : MuseBleConstants.ACCEL_SCALE_G;

        for (int i = 0; i < 3; i++) {
            int offset = 2 + i * 6;
            double x = int16(data, offset) * scale;
            double y = int16(data, offset + 2) * scale;
            double z = int16(data, offset + 4) * scale;
            samples[i] = new ImuSample(x, y, z);
        }
        return new ImuPacket(seq, samples, isGyro);
    }

    public static String hexDump(byte[] data) {
        if (data == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        return sb.append(']').toString();
    }

    private static int uint16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int int16(byte[] data, int offset) {
        int v = uint16(data, offset);
        return v >= 0x8000 ? v - 0x10000 : v;
    }

    private static int unpack12bit(byte[] data, int baseOffset, int index) {
        int byteIndex = baseOffset + (index * 3) / 2;
        if ((index & 1) == 0) {
            return ((data[byteIndex] & 0xFF) << 4) | ((data[byteIndex + 1] & 0xF0) >> 4);
        }
        return ((data[byteIndex] & 0x0F) << 8) | (data[byteIndex + 1] & 0xFF);
    }

    public static final class EegPacket {
        public final int sequence;
        public final double[] samplesUv;

        EegPacket(int sequence, double[] samplesUv) {
            this.sequence = sequence;
            this.samplesUv = samplesUv;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("seq=%5d  [", sequence));
            for (int i = 0; i < samplesUv.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(String.format("%8.2f", samplesUv[i]));
            }
            sb.append("] uV");
            return sb.toString();
        }
    }

    public static final class ImuSample {
        public final double x;
        public final double y;
        public final double z;

        ImuSample(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final class ImuPacket {
        public final int sequence;
        public final ImuSample[] samples;
        public final boolean isGyro;

        ImuPacket(int sequence, ImuSample[] samples, boolean isGyro) {
            this.sequence = sequence;
            this.samples = samples;
            this.isGyro = isGyro;
        }

        @Override
        public String toString() {
            String unit = isGyro ? "deg/s" : "g";
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("seq=%5d  [", sequence));
            for (int i = 0; i < samples.length; i++) {
                if (i > 0) {
                    sb.append(" | ");
                }
                sb.append(String.format("(%.4f, %.4f, %.4f)", samples[i].x, samples[i].y, samples[i].z));
            }
            sb.append("] ").append(unit);
            return sb.toString();
        }
    }
}
