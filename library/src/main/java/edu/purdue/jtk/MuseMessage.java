package edu.purdue.jtk;

import oscP5.OscMessage;

/**
 * The MuseMessage class encapsulates the data sent from the Muse headband (or proxy sources) to the Model.
 */
class MuseMessage extends OscMessage implements Comparable<MuseMessage> {
    private Double timestamp;

    MuseMessage(double timestamp, String address, Object[] arguments) {
        super(address, arguments);
        this.timestamp = timestamp;
        hostAddress = "127.0.0.1";
        port = 8000;
    }

    MuseMessage(OscMessage oscMessage) {
        super(oscMessage);
        timestamp = System.currentTimeMillis() / 1000.0;
    }

    double getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(MuseMessage o) {
        return timestamp.compareTo(o.timestamp);
    }
}
