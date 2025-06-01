package com.omerflex.server;

import com.omerflex.service.logging.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for common server operations like URL manipulation,
 * domain extraction, and other shared functionality.
 */
public class ServerUtility {
    private static final String TAG = "ServerUtility";

    /**
     * Extract the domain from a URL
     * @param url The URL to extract domain from
     * @param includeProtocol Whether to include the protocol (http/https)
     * @param includeWww Whether to include www in the extracted domain
     * @return The extracted domain
     */
    public static String extractDomain(String url, boolean includeProtocol, boolean includeWww) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            URL urlObj = new URL(url);
            String domain = urlObj.getHost();

            if (domain != null) {
                if (!includeWww && domain.startsWith("www.")) {
                    domain = domain.substring(4);
                }

                if (includeProtocol) {
                    return urlObj.getProtocol() + "://" + domain;
                }
                return domain;
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error extracting domain from URL: " + url, e);
        }

        // Fallback to regex
        String pattern = includeWww ?
                "^(?:https?://)?([^:/\\n?]+)" :
                "^(?:https?://)?(?:www\\.)?([^:/\\n?]+)";

        Matcher matcher = Pattern.compile(pattern).matcher(url);
        if (matcher.find()) {
            return includeProtocol ? getProtocol(url) + "://" + matcher.group(1) : matcher.group(1);
        }

        return "";
    }

    /**
     * Get the protocol (http/https) from a URL
     * @param url The URL
     * @return The protocol with "://" or "https://" if not found
     */
    public static String getProtocol(String url) {
        if (url == null || url.isEmpty()) {
            return "https://";
        }

        if (url.startsWith("http://")) {
            return "http://";
        }

        return "https://";
    }

    /**
     * Get a valid referer URL from a URL
     * @param url The URL to build referer from
     * @return The referer URL
     */
    public static String getValidReferer(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            URL urlObj = new URL(url);
            return urlObj.getProtocol() + "://" + urlObj.getHost();
        } catch (MalformedURLException e) {
            Logger.e(TAG, "Error creating referer from URL: " + url, e);
            return url;
        }
    }

    /**
     * Check if URL is for a video file
     * @param url The URL to check
     * @return true if it's a video file URL
     */
    public static boolean isVideoUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".mp4") ||
                lowerUrl.endsWith(".m3u8") ||
                lowerUrl.endsWith(".mkv") ||
                lowerUrl.endsWith(".avi") ||
                lowerUrl.endsWith(".mov") ||
                lowerUrl.contains("stream") ||
                lowerUrl.contains("video") ||
                lowerUrl.contains("player") ||
                lowerUrl.contains("embed");
    }

    /**
     * Check if a URL is an HLS streaming URL
     * @param url The URL to check
     * @return true if it's an HLS URL
     */
    public static boolean isHlsUrl(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".m3u8") ||
                lowerUrl.contains("playlist.m3u") ||
                lowerUrl.contains("manifest") ||
                lowerUrl.contains("hls");
    }

    /**
     * Resolve a potentially relative URL to absolute using a base URL
     * @param baseUrl The base URL
     * @param url The URL to resolve (may be relative)
     * @return The absolute URL
     */
    public static String resolveUrl(String baseUrl, String url) {
        if (url == null || url.isEmpty()) {
            return baseUrl;
        }

        if (url.startsWith("http")) {
            return url;
        }

        try {
            URL base = new URL(baseUrl);
            return new URL(base, url).toString();
        } catch (MalformedURLException e) {
            Logger.e(TAG, "Error resolving URL: " + url + " with base: " + baseUrl, e);
            return url;
        }
    }
}