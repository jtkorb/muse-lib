package edu.purdue.jtk;

import java.net .*;
import java.util .*;

/**
 * The NetworkInterfaces class looks for a local network interface with an IPv4, non-loopback address.
 *
 * Based on code from https://stackoverflow.com/a/7334091/288770.
 *
 * @author Tim Korb
 * @since 1.0.0
 */

public class NetworkInterfaces {
    public static String getNonLoopbackAddress() {
        Enumeration<NetworkInterface> n = null;
        try {
            n = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            System.out.println("COULD NOT GET NETWORK INTERFACES!  TELL A FRIEND.");
        }

        while (n.hasMoreElements()) {
            NetworkInterface e = n.nextElement();
            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements(); ) {
                InetAddress addr = a.nextElement();
                if (addr.getAddress().length == 4 && !addr.isLoopbackAddress())
                    return addr.getHostAddress();
            }
        }

        return "0.0.0.0"; // Error condition
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Host addr: " + InetAddress.getLocalHost().getHostAddress());  // often returns "127.0.0.1"
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        while (n.hasMoreElements()) {
            NetworkInterface e = n.nextElement();
            System.out.println("Interface: " + e.getName());
            Enumeration<InetAddress> a = e.getInetAddresses();
            while (a.hasMoreElements()) {
                InetAddress addr = a.nextElement();
                System.out.format("\t%s: any = %b, link = %b, loopback = %b, byte = %x, length = %d\n",
                        addr.getHostAddress(),
                        addr.isAnyLocalAddress(),
                        addr.isLinkLocalAddress(),
                        addr.isLoopbackAddress(),
                        addr.getAddress()[0],
                        addr.getAddress().length
                        );
                if (addr.getAddress().length == 4 && !addr.isLoopbackAddress())
                    System.out.println("WINNER!");
            }
        }
    }
}
