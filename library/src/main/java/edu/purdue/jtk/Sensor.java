package edu.purdue.jtk;

/**
 * The Sensor class enumerates locations used by the Muse headband (FH = forehead).
 */
public enum Sensor {
    LEFT_EAR(0, "Left Ear"),
    LEFT_FH(1, "Left FH"),
    RIGHT_FH(2, "Right FH"),
    RIGHT_EAR(3, "Right Ear");

    final int value;
    final String name;

    Sensor(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
