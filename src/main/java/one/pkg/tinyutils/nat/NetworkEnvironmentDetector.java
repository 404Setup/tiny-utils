package one.pkg.tinyutils.nat;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkEnvironmentDetector {

    public static IPVersionSupport detectIPVersionSupport() throws SocketException {
        IPVersionSupport support = new IPVersionSupport();

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();

            if (iface.isLoopback() || !iface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();

                if (addr instanceof Inet4Address) {
                    support.ipv4Supported = true;
                    support.ipv4Addresses.add(addr.getHostAddress());
                } else if (addr instanceof Inet6Address) {
                    support.ipv6Supported = true;
                    support.ipv6Addresses.add(addr.getHostAddress());
                }
            }
        }

        return support;
    }

    public static boolean testIPv4Connectivity() {
        try {
            InetAddress address = InetAddress.getByName("8.8.8.8");
            return address.isReachable(3000);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean testIPv6Connectivity() {
        try {
            InetAddress address = InetAddress.getByName("2001:4860:4860::8888");
            return address.isReachable(3000);
        } catch (Exception e) {
            return false;
        }
    }

    public static class IPVersionSupport {
        public boolean ipv4Supported = false;
        public boolean ipv6Supported = false;
        public List<String> ipv4Addresses = new ArrayList<>();
        public List<String> ipv6Addresses = new ArrayList<>();
    }
}