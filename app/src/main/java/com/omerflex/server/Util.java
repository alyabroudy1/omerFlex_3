package com.omerflex.server;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class Util {
    private static final String TAG = "Util";

    public static String extractDomain(String videoUrl, boolean withSchema) {
        String fullDomain = "";
        try {
            URL url = new URL(videoUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            if (!withSchema){
                return host;
            }
                fullDomain = protocol + "://" + host + "/";

        } catch (Exception e) {

        }
        Log.d(TAG, "extractDomain: " + fullDomain);
        return fullDomain;
    }

    public static String getUrlPathOnly(String url) {
        try {
            URI uri = new URI(url);
            // Get the path part of the URL without the domain
            String path = uri.getPath();
            return path;
        } catch (URISyntaxException e) {
            // Handle invalid URI syntax
            e.printStackTrace();
            return null;
        }
    }

}
