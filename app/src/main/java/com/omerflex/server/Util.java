package com.omerflex.server;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private static final String TAG = "Util";

    public static String extractDomain(String videoUrl, boolean withSchema, boolean endSlash) {
        String fullDomain = "";
        try {
            URL url = new URL(videoUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            if (!withSchema){
                return host;
            }
            String endPart = "";
            if (endSlash){
                endPart = "/";
            }
                fullDomain = protocol + "://" + host + endPart;

        } catch (Exception e) {
            Log.d(TAG, "error: extractDomain: "+e.getMessage());
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

    public static String getValidReferer(String referer) {
        String result = referer;
        if (referer != null) {
            Pattern pattern = Pattern.compile("(https?://[^/]+)");
            Matcher matcher = pattern.matcher(referer);
            if (matcher.find()) {
                result = matcher.group(1);
            }
        }
        Log.d(TAG, "getValidReferer: " + result + ", " + referer);
        return result;
    }

    public static Map<String, String> getMapCookies(String cookies) {
        Map<String, String> cookiesHash = new HashMap<>();
        if (cookies != null) {
            //split the String by a comma
            String parts[] = cookies.split(";");

            //iterate the parts and add them to a map
            for (String part : parts) {

                //split the employee data by : to get id and name
                String empdata[] = part.split("=");

                String strId = empdata[0].trim();
                String strName = empdata[1].trim();

                //add to map
                cookiesHash.put(strId, strName);
            }

        }
        return cookiesHash;
    }

    public static String generateHeadersForVideoUrl(Map<String, String> headers) {
        String headerString = "";
        if (headers != null && !headers.isEmpty()){
            try {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
//                                if (entry.getKey().equals("User-Agent")){
//                                    continue;
//                                }
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    sb.append("&");
                }
                // Remove the last "&" character
                sb.deleteCharAt(sb.length() - 1);
                headerString = sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "onLoadResource: error building headers for the video: " + e.getMessage());
                return "";
            }

            if (!headerString.isEmpty()){
                return  "|" + headerString;
            }
        }
        return "";
    }
}
