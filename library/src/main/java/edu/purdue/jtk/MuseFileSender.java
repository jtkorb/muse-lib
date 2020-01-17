package edu.purdue.jtk;

/*
 * Beginning of a class to send packets to a waiting Muse Art listener.
 */

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import oscP5.OscProperties;

import java.io.IOException;
import java.net.*;

public class MuseFileSender {
    private OscP5 oscp5;
    private NetAddress destination;
    private DatagramSocket socket;
    private InetAddress address;

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
