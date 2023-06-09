package edu.purdue.jtk;

/**
 * The abstract MuseSource class is the interface between the Muse data and the Model.
 */
abstract class MuseSource {
    protected final Model model;
    private final MuseStatistics ms;

    private final int SENSOR_LENGTH = Sensor.values().length;

    MuseSource(Model model, MuseStatistics ms) {
        this.model = model;
        this.ms = ms;
    }

    void dispatchMessage(String address, MuseMessage mm) {
        ms.countAddress(address, mm.getTimestamp());

        switch (address) {
            case "alpha_absolute":
                storeWaves(Wave.ALPHA, mm);
                break;
            case "beta_absolute":
                storeWaves(Wave.BETA, mm);
                break;
            case "gamma_absolute":
                storeWaves(Wave.GAMMA, mm);
                break;
            case "delta_absolute":
                storeWaves(Wave.DELTA, mm);
                break;
            case "theta_absolute":
                storeWaves(Wave.THETA, mm);
                break;
            case "is_good":
                storeIsGood(mm);
                break;
            case "touching_forehead":
                model.setTouchingForehead(mm.get(0).intValue() == 1);
                break;
            case "horseshoe":
                storeHorseshoe(mm);
                break;
            case "blink", "gyro", "acc", "eeg":
                break;
            default:
                System.out.printf("Unknown address: %s\n", address);
        }
    }

    private void storeHorseshoe(MuseMessage mm) {
        for (int i = 0; i < SENSOR_LENGTH; i++)
            model.setHorseshoe(i, mm.get(i).floatValue());
    }

    private void storeIsGood(MuseMessage mm) {
        for (int i = 0; i < SENSOR_LENGTH; i++)
            model.setIsGood(i, (int) mm.get(i).floatValue());
    }

    /**
     * Stores the four sensor values arriving for wave.
     *
     * @param wave  One of the five brain waves (Alpha, Beta, Gamma, Delta, Theta)
     * @param mm    A Muse message containing the four sensor values (left ear, left fh, right fh, right ear)
     */
    private void storeWaves(Wave wave, MuseMessage mm) {
        long time = System.currentTimeMillis();

        for (int i = 0; i < SENSOR_LENGTH; i++) {
            float value;
            if (mm.arguments().length == 1) // If length is 1, then Muse Monitor is sending averages
                value = mm.get(0).floatValue();
            else
                value = mm.get(i).floatValue();

            model.setGrid(wave.value, i, value, time);
        }
    }

    public void shutdown() {
        model.setTouchingForehead(false);  // Turn off control panel wave display
    };
}
