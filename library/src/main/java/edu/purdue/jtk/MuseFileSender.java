package edu.purdue.jtk;

import java.io.IOException;
import java.net.DatagramSocket;

import oscP5.OscMessage;
import oscP5.OscP5;
import oscP5.OscProperties;

/**
 * The MuseFileSender class is an alternative to MuseFileReader for pulling Muse data from a file and sending it
 * to a waiting Muse Listener object.  This code is incomplete and is not yet integrated into the library.
 */
public class MuseFileSender {
    private OscP5 oscp5;
    private DatagramSocket socket;

    public MuseFileSender() {
        OscProperties properties = new OscProperties();
        properties.setRemoteAddress("127.0.0.1", 8080);
        properties.setNetworkProtocol(OscProperties.UDP);
        oscp5 = new OscP5(this, properties);
    }

    public void send(String message) {
        oscp5.send(new OscMessage(message));
    }

    public void send(OscMessage message) {
        oscp5.send(message);
    }

    public void close() {
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        MuseFileSender oscs = new MuseFileSender();
        oscs.send("hello");
        oscs.send("there");
    }
}
