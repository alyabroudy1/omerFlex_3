package com.omerflex.providers;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class SsdpDiscoverer {
    private static final String TAG = "SsdpDiscoverer";
    private static final String SSDP_HOST = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    private static final int TIMEOUT = 3000;

    public interface DiscoveryListener {
        void onDeviceFound(String deviceInfo, String ipAddress);
        void onDiscoveryComplete(List<String> devices);
        void onError(String error);
    }

    // Add this method to SsdpDiscoverer class
    public static class DlnaDevice {
        public String friendlyName;
        public String ipAddress;
        public String location;
        public String server;
        public String usn;

        public DlnaDevice(String friendlyName, String ipAddress, String location, String server, String usn) {
            this.friendlyName = friendlyName;
            this.ipAddress = ipAddress;
            this.location = location;
            this.server = server;
            this.usn = usn;
        }

        @Override
        public String toString() {
            return friendlyName + " (" + ipAddress + ")";
        }
    }

    // Update the discovery method to return DlnaDevice objects
    public static void discoverDevicesWithDetails(DiscoveryListener listener) {
        new Thread(() -> {
            try {
                List<DlnaDevice> devices = new ArrayList<>();

                String searchMessage =
                        "M-SEARCH * HTTP/1.1\r\n" +
                                "HOST: " + SSDP_HOST + ":" + SSDP_PORT + "\r\n" +
                                "MAN: \"ssdp:discover\"\r\n" +
                                "MX: 3\r\n" +
                                "ST: urn:schemas-upnp-org:service:AVTransport:1\r\n" + // Media renderers
                                "\r\n";

                byte[] sendData = searchMessage.getBytes();

                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(TIMEOUT);

                    InetAddress group = InetAddress.getByName(SSDP_HOST);
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData, sendData.length, group, SSDP_PORT
                    );

                    socket.send(sendPacket);

                    byte[] receiveData = new byte[8192];
                    long endTime = System.currentTimeMillis() + TIMEOUT;

                    while (System.currentTimeMillis() < endTime) {
                        try {
                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            socket.receive(receivePacket);

                            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                            String deviceIp = receivePacket.getAddress().getHostAddress();

                            DlnaDevice device = parseDeviceDetails(response, deviceIp);
                            if (device != null && !containsDevice(devices, device.usn)) {
                                devices.add(device);
                                listener.onDeviceFound(device.toString(), deviceIp);
                            }

                        } catch (java.net.SocketTimeoutException e) {
                            break;
                        }
                    }
                }

                listener.onDiscoveryComplete(devices.stream().map(DlnaDevice::toString).collect(Collectors.toList()));

            } catch (Exception e) {
                Log.e(TAG, "SSDP discovery error", e);
                listener.onError("Discovery failed: " + e.getMessage());
            }
        }).start();
    }

    private static DlnaDevice parseDeviceDetails(String response, String ipAddress) {
        try {
            String[] lines = response.split("\r\n");
            String server = null;
            String location = null;
            String usn = null;

            for (String line : lines) {
                if (line.toUpperCase().startsWith("SERVER:")) {
                    server = line.substring(7).trim();
                } else if (line.toUpperCase().startsWith("LOCATION:")) {
                    location = line.substring(9).trim();
                } else if (line.toUpperCase().startsWith("USN:")) {
                    usn = line.substring(4).trim();
                }
            }

            if (server != null && location != null) {
                String friendlyName = extractFriendlyName(server);
                return new DlnaDevice(friendlyName, ipAddress, location, server, usn);
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing device details", e);
            return null;
        }
    }

    private static boolean containsDevice(List<DlnaDevice> devices, String usn) {
        for (DlnaDevice device : devices) {
            if (device.usn != null && device.usn.equals(usn)) {
                return true;
            }
        }
        return false;
    }

    public static void discoverDevices(DiscoveryListener listener) {
        new Thread(() -> {
            try {
                List<String> devices = new ArrayList<>();

                // SSDP M-SEARCH message
                String searchMessage =
                        "M-SEARCH * HTTP/1.1\r\n" +
                                "HOST: " + SSDP_HOST + ":" + SSDP_PORT + "\r\n" +
                                "MAN: \"ssdp:discover\"\r\n" +
                                "MX: 3\r\n" +
                                "ST: ssdp:all\r\n" +
                                "\r\n";

                byte[] sendData = searchMessage.getBytes();

                // Get local IP address
                String localIp = getLocalIpAddress();
                if (localIp == null) {
                    listener.onError("Cannot get local IP address");
                    return;
                }

                Log.d(TAG, "Starting SSDP discovery from: " + localIp);

                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(TIMEOUT);

                    InetAddress group = InetAddress.getByName(SSDP_HOST);
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData, sendData.length, group, SSDP_PORT
                    );

                    // Send discovery packet
                    socket.send(sendPacket);
                    Log.d(TAG, "SSDP discovery packet sent");

                    // Listen for responses
                    byte[] receiveData = new byte[8192];
                    long endTime = System.currentTimeMillis() + TIMEOUT;

                    while (System.currentTimeMillis() < endTime) {
                        try {
                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            socket.receive(receivePacket);

                            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                            String deviceIp = receivePacket.getAddress().getHostAddress();

                            Log.d(TAG, "Received response from: " + deviceIp);
                            Log.d(TAG, "Response: " + response);

                            // Parse device info from response
                            String deviceInfo = parseDeviceInfo(response, deviceIp);
                            if (deviceInfo != null && !devices.contains(deviceInfo)) {
                                devices.add(deviceInfo);
                                listener.onDeviceFound(deviceInfo, deviceIp);
                            }

                        } catch (java.net.SocketTimeoutException e) {
                            // Timeout is expected, continue
                            break;
                        }
                    }
                }

                Log.d(TAG, "SSDP discovery completed. Found: " + devices.size() + " devices");
                listener.onDiscoveryComplete(devices);

            } catch (Exception e) {
                Log.e(TAG, "SSDP discovery error", e);
                listener.onError("Discovery failed: " + e.getMessage());
            }
        }).start();
    }

    private static String parseDeviceInfo(String response, String ipAddress) {
        try {
            String[] lines = response.split("\r\n");
            String server = null;
            String location = null;
            String usn = null;

            for (String line : lines) {
                if (line.toUpperCase().startsWith("SERVER:")) {
                    server = line.substring(7).trim();
                } else if (line.toUpperCase().startsWith("LOCATION:")) {
                    location = line.substring(9).trim();
                } else if (line.toUpperCase().startsWith("USN:")) {
                    usn = line.substring(4).trim();
                }
            }

            if (server != null) {
                // Extract friendly name if available
                String friendlyName = extractFriendlyName(server);
                return friendlyName + " (" + ipAddress + ")";
            }

            return "Unknown Device (" + ipAddress + ")";

        } catch (Exception e) {
            Log.e(TAG, "Error parsing device info", e);
            return "Device (" + ipAddress + ")";
        }
    }

    private static String extractFriendlyName(String server) {
        // Try to extract a friendly name from the server string
        if (server.contains("/")) {
            String[] parts = server.split("/");
            if (parts.length > 1) {
                return parts[0].trim();
            }
        }
        return server;
    }

    private static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error getting local IP", e);
        }
        return null;
    }
}