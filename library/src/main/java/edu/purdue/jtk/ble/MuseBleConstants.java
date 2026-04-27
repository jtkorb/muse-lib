package edu.purdue.jtk.ble;

/**
 * Muse BLE service/characteristic UUIDs and command bytes.
 */
public final class MuseBleConstants {

    public static final String SERVICE_UUID = "0000FE8D-0000-1000-8000-00805F9B34FB";

    public static final String CHAR_CONTROL = "273E0001-4C4D-454D-96BE-F03BAC821358";

    public static final String CHAR_EEG_TP9 = "273E0003-4C4D-454D-96BE-F03BAC821358";
    public static final String CHAR_EEG_AF7 = "273E0004-4C4D-454D-96BE-F03BAC821358";
    public static final String CHAR_EEG_AF8 = "273E0005-4C4D-454D-96BE-F03BAC821358";
    public static final String CHAR_EEG_TP10 = "273E0006-4C4D-454D-96BE-F03BAC821358";
    public static final String CHAR_EEG_AUX = "273E0007-4C4D-454D-96BE-F03BAC821358";

    public static final String CHAR_GYRO = "273E0009-4C4D-454D-96BE-F03BAC821358";
    public static final String CHAR_ACCEL = "273E000A-4C4D-454D-96BE-F03BAC821358";
    public static final String CHAR_BATTERY = "273E000B-4C4D-454D-96BE-F03BAC821358";

    public static final byte[] CMD_START = {0x02, 0x64, 0x0a};
    public static final byte[] CMD_STOP = {0x02, 0x68, 0x0a};
    public static final byte[] CMD_KEEP_ALIVE = {0x02, 0x6b, 0x0a};
    public static final byte[] CMD_VERSION = {0x03, 0x76, 0x31, 0x0a};
    public static final byte[] CMD_PRESET_P20 = {0x04, 0x70, 0x32, 0x30, 0x0a};
    public static final byte[] CMD_PRESET_P21 = {0x04, 0x70, 0x32, 0x31, 0x0a};

    public static final double EEG_SCALE_UV = 0.48828125;
    public static final double ACCEL_SCALE_G = 0.0000610352;
    public static final double GYRO_SCALE_DEG = 0.0074768;

    public static final long WRITE_WITH_RESPONSE = 0L;
    public static final long WRITE_WITHOUT_RESPONSE = 1L;

    private MuseBleConstants() {
    }
}
