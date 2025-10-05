package com.omerflex.service.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.omerflex.OmerFlexApplication;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Utility class for network-related operations.
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    /**
     * Check if the device has an active network connection.
     *
     * @return true if there is an active network connection, false otherwise
     */
    public static boolean isNetworkAvailable() {
        Context context = OmerFlexApplication.getAppContext();
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            Log.w(TAG, "ConnectivityManager is null, assuming no network");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                Log.d(TAG, "No active network");
                return false;
            }

            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                Log.d(TAG, "Active network has no capabilities");
                return false;
            }

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            // Legacy method for API < 23
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        Log.d("NetworkUtils", "Found local IP: " + ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("NetworkUtils", "Error getting network interfaces", e);
        }
        return null;
    }

    /**
     * Check if the current connection is a WiFi connection.
     *
     * @return true if connected to WiFi, false otherwise
     */
    public static boolean isWifiConnection() {
        Context context = OmerFlexApplication.getAppContext();
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return false;
            }

            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            // Legacy method for API < 23
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null &&
                    activeNetwork.getType() == ConnectivityManager.TYPE_WIFI &&
                    activeNetwork.isConnected();
        }
    }

    /**
     * Check if the current connection is a cellular connection.
     *
     * @return true if connected via cellular, false otherwise
     */
    public static boolean isCellularConnection() {
        Context context = OmerFlexApplication.getAppContext();
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return false;
            }

            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            // Legacy method for API < 23
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null &&
                    activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE &&
                    activeNetwork.isConnected();
        }
    }

    /**
     * Get a description of the current network connection.
     *
     * @return A string describing the current network connection
     */
    public static String getNetworkDescription() {
        if (!isNetworkAvailable()) {
            return "No network connection";
        }

        if (isWifiConnection()) {
            return "WiFi connection";
        }

        if (isCellularConnection()) {
            return "Cellular connection";
        }

        return "Other network connection";
    }
}