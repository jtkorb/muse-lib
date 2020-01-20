package edu.purdue.jtk;

/**
 * The Wave class defines and encapsulates the waves sensed by the Muse headband.
 */
public enum Wave {
    DELTA(0, "Delta", "delta_absolute"),
    THETA(1, "Theta", "theta_absolute"),
    ALPHA(2, "Alpha", "alpha_absolute"),
    BETA(3, "Beta", "beta_absolute"),
    GAMMA(4, "Gamma", "gamma_absolute");

    final int value;
    final String name;
    final String address;

    /**
     * Creates a Wave enumerated object.
     *
     * @param value     a value use to index this wave item
     * @param name      the name of this wave
     * @param address   the OSC address used by Muse to identify this wave
     */
    Wave(int value, String name, String address) {
        this.value = value;
        this.name = name;
        this.address = address;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getAddress() { return address; }
}
