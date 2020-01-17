package edu.purdue.jtk;

import oscP5.OscEventListener;
import oscP5.OscP5;
import oscP5.OscStatus;

class MuseListener extends MuseSource implements OscEventListener {
    private OscP5 oscP5;
    private String ipAddress;

    private final int SENSOR_LENGTH = Sensor.values().length;

    MuseListener(int port, Model model, MuseStatistics ms) {
        super(model, ms);
        oscP5 = new OscP5(this, port); // calls oscEvent as Muse events arrive
        ipAddress = oscP5.ip();
    }

    String getIPAddress() { return ipAddress; }

    String extractAddress(String addrPattern) {
        int i = addrPattern.lastIndexOf('/');
        return addrPattern.substring(i+1);
    }

    @Override
    public void oscEvent(oscP5.OscMessage oscMessage) {
        try {
            MuseMessage mm = new MuseMessage(oscMessage);
            String address = extractAddress(mm.addrPattern());
            dispatchMessage(address, mm);
        } catch (Exception e) {
            System.out.format("Exception in oscEvent handling: %s\n", e);
        }
    }

    @Override
    public void oscStatus(OscStatus oscStatus) {
        System.out.format("OscStatus: %s\n", oscStatus);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        oscP5.stop();
    }
}
