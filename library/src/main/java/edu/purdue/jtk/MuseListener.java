package edu.purdue.jtk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import oscP5.OscEventListener;
import oscP5.OscP5;
import oscP5.OscStatus;

/**
 * The MuseListener class is a MuseSource that reads data from a Muse headband,
 * sent by the Muse Direct or compatible app (i.e., using the Open Sound Connect
 * protocol with pre-defined event names).
 */
class MuseListener extends MuseSource implements OscEventListener {
    private OscP5 oscP5;
    private String ipAddress;

    private final int SENSOR_LENGTH = Sensor.values().length;
    private final long startTime = System.currentTimeMillis();

    public boolean tracing = false;

    MuseListener(int port, Model model, MuseStatistics ms) {
        super(model, ms);
        oscP5 = new OscP5(this, port); // Calls oscEvent as Muse events arrive
        ipAddress = NetworkInterfaces.getNonLoopbackAddress();
    }

    String getIPAddress() {
        return ipAddress;
    }

    String extractAddress(String addrPattern) {
        int i = addrPattern.lastIndexOf('/');
        return addrPattern.substring(i + 1);
    }

    @Override
    public void oscEvent(oscP5.OscMessage oscMessage) {
        try {
            MuseMessage mm = new MuseMessage(oscMessage);
            String address = extractAddress(mm.addrPattern());

            // Record/dump events if tracing...
            if (tracing)
                recordEvent(System.currentTimeMillis() - startTime, address, mm);

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

    /*
     * Code to record events here to end of file...
     */
    boolean waitingSummary = true;

    private void recordEvent(Long time, String address, MuseMessage mm) {
        if (address.equals("eeg") || address.equals("gyro") || address.equals("acc"))
            return;
        else {
            // Approximately every 10 seconds, dump a summary...
            if (time % 10000 < 100) {
                if (waitingSummary) {
                    this.ms.printStats(time);
                    this.ms.printMessageSummary();
                    waitingSummary = false;
                }
            } else
                waitingSummary = true;
            if (eb == null) {
                eb = new EventBuffer(time);
            } else if (time - eb.time > 10) {
                // If 10ms time has passed, print and clear the buffer...
                eb.print(time);
                eb = new EventBuffer(time);
            }
            eb.add(address, mm.typetag(), mm.arguments());
        }
    }

    private class EventBuffer {
        long time;
        Map<String, String> mapHeadings = null;

        EventBuffer(long time) {
            this.time = time;
            if (mapHeadings == null) {
                mapHeadings = new HashMap<String, String>();
                mapHeadings.put("touching_forehead", "contact");
                mapHeadings.put("blink", "blink");
                mapHeadings.put("alpha_absolute", "alpha");
                mapHeadings.put("beta_absolute", "beta");
                mapHeadings.put("delta_absolute", "delta");
                mapHeadings.put("theta_absolute", "theta");
                mapHeadings.put("gamma_absolute", "gamma");
                mapHeadings.put("horseshoe", "horseshoe");
            }
        }

        private String mapHeading(String address) {
            String heading = mapHeadings.get(address);
            if (heading == null)
                heading = address;
            return heading;
        }

        private class Event {
            String address;
            String heading;
            String typetag;
            Object[] arguments;

            Event(String address, String typetag, Object[] arguments) {
                this.address = address;
                this.heading = mapHeading(address);
                this.typetag = typetag;
                this.arguments = arguments;
            }

            public String toString() {
                String result = String.format("%-10s", heading);
                for (Object argument : arguments) {
                    String value;
                    switch (argument.getClass().getName()) {
                        case "java.lang.Float":
                            value = String.format("%10.3f", argument);
                            break;
                        case "java.lang.Integer":
                            value = String.format("%10d", argument);
                            break;
                        default:
                            value = argument.toString();
                            break;
                    }
                    result += String.format(" %10s", value);
                }
                return result;
            }
        }

        ArrayList<Event> events = new ArrayList<>();

        private void add(String address, String typetag, Object[] arguments) {
            Event event = new Event(address, typetag, arguments);
            events.add(event);
        }

        void print(long timeEnd) {
            System.out.printf("===== Events during period %.3f to %.3f =====\n",
                    time / 1000d, timeEnd / 1000d);
            for (Event event : events) {
                System.out.printf("\t%s\n", event.toString());
            }
        }
    }

    private static EventBuffer eb = null;

    private void dumpAbsoluteEvent(String address, MuseMessage mm) {
        long time = System.currentTimeMillis();
        System.out.format("BEGIN WAVE: %s -> %s (length %d) at %f\n",
                address, mm.toString(), mm.arguments().length, time / 1000f);
        for (int i = 0; i < SENSOR_LENGTH; i++) {
            float value;
            boolean average;
            if (mm.arguments().length == 1) { // If length is 1, then Mind Monitor is sending averages
                value = mm.get(0).floatValue();
                average = true;
            } else {
                value = mm.get(i).floatValue();
                average = false;
            }
            System.out.printf("\t%d = %f (average = %b)\n", i, value, average);
        }
        System.out.format("END WAVE\n");
    }
}