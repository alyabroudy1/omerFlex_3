package com.omerflex.providers;

import android.util.Log;
import com.omerflex.entity.Movie;
import com.omerflex.service.utils.NetworkUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

public class MediaServer extends NanoHTTPD {
    private static final String TAG = "MediaServer";
    private static MediaServer instance;
    private Movie currentMovie;
    private Map<String, String> headers;

    // Patterns to identify different file types
    private static final Pattern HLS_PATTERN = Pattern.compile("\\.m3u8", Pattern.CASE_INSENSITIVE);
    private static final Pattern TS_PATTERN = Pattern.compile("\\.ts", Pattern.CASE_INSENSITIVE);
    private static final Pattern MP4_PATTERN = Pattern.compile("\\.mp4", Pattern.CASE_INSENSITIVE);

    public static MediaServer getInstance() {
        if (instance == null) {
            instance = new MediaServer(8080);
        }
        return instance;
    }

    private MediaServer(int port) {
        super(port);
    }

    public String startServer(Movie movie, Map<String, String> headers) {
        this.currentMovie = movie;
        this.headers = headers;

        try {
            if (isRunning()) {
                stop();
            }
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            String localIp = NetworkUtils.getLocalIpAddress();
            String serverUrl = "http://" + localIp + ":" + getListeningPort() + "/video";

            Log.d(TAG, "Media server started: " + serverUrl);
            Log.d(TAG, "Original URL: " + movie.getVideoUrl());
            return serverUrl;

        } catch (IOException e) {
            Log.e(TAG, "Failed to start media server", e);
            return null;
        }
    }

    public void stopServer() {
        if (isRunning()) {
            stop();
            Log.d(TAG, "Media server stopped");
        }
    }

    public boolean isRunning() {
        return wasStarted();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Log.d(TAG, "Request: " + session.getMethod().name() + " " + uri);

        if (!Method.GET.equals(session.getMethod())) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method not allowed");
        }
        // Add to serve() method
        if ("/test".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Server is working!");
        }

        try {
            String originalUrl = currentMovie.getVideoUrl().split("\\|")[0];
            String remoteUrl;

            if ("/video".equals(uri)) {
                remoteUrl = originalUrl;
                Log.d(TAG, "Serving main video from: " + remoteUrl);
            } else {
                // Handle HLS segments and other relative URLs
                remoteUrl = buildRemoteUrl(originalUrl, uri);
                if (remoteUrl == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
                }
                Log.d(TAG, "Serving segment from: " + remoteUrl);
            }

            return proxyRequest(session, remoteUrl);

        } catch (Exception e) {
            Log.e(TAG, "Error serving request for URI: " + uri, e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal server error: " + e.getMessage());
        }
    }

    private String buildRemoteUrl(String originalUrl, String requestUri) {
        try {
            // Remove leading slash from request URI
            String relativePath = requestUri.startsWith("/") ? requestUri.substring(1) : requestUri;

            // If the request is for a full URL (shouldn't happen but handle it)
            if (relativePath.startsWith("http")) {
                return relativePath;
            }

            // Handle different URL patterns
            if (originalUrl.contains(".m3u8")) {
                // For HLS streams, build the full URL for segments
                int lastSlashIndex = originalUrl.lastIndexOf('/');
                if (lastSlashIndex != -1) {
                    String baseUrl = originalUrl.substring(0, lastSlashIndex + 1);
                    return baseUrl + relativePath;
                }
            } else if (originalUrl.contains("/hls/") || originalUrl.contains("/stream/")) {
                // For structured URLs, try to reconstruct the full path
                URL url = new URL(originalUrl);
                String path = url.getPath();
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1) {
                    String basePath = path.substring(0, lastSlash + 1);
                    return new URL(url.getProtocol(), url.getHost(), url.getPort(), basePath + relativePath).toString();
                }
            }

            // Fallback: assume the request URI is relative to the original URL's directory
            URL baseUrl = new URL(originalUrl);
            String basePath = baseUrl.getPath();
            int lastSlash = basePath.lastIndexOf('/');
            if (lastSlash != -1) {
                String newPath = basePath.substring(0, lastSlash + 1) + relativePath;
                return new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), newPath).toString();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error building remote URL for: " + requestUri, e);
        }

        return null;
    }

    private Response proxyRequest(IHTTPSession session, String remoteUrl) {
        HttpURLConnection connection = null;
        try {
            Log.d(TAG, "Proxying to: " + remoteUrl);

            URL url = new URL(remoteUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // Add original headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            // Copy important headers from client request
            Map<String, String> clientHeaders = session.getHeaders();
            String rangeHeader = clientHeaders.get("range");
            if (rangeHeader != null) {
                connection.setRequestProperty("Range", rangeHeader);
                Log.d(TAG, "Forwarding Range header: " + rangeHeader);
            }

            // Set user agent if not already set
            if (!headers.containsKey("User-Agent")) {
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            }

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Remote server response: " + responseCode);

            // Handle redirects
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                String location = connection.getHeaderField("Location");
                if (location != null) {
                    Log.d(TAG, "Following redirect to: " + location);
                    return proxyRequest(session, location);
                }
            }

            String contentType = getContentType(remoteUrl, connection.getContentType());
            long contentLength = connection.getContentLengthLong();

            InputStream inputStream;
            try {
                inputStream = connection.getInputStream();
            } catch (IOException e) {
                inputStream = connection.getErrorStream();
            }

            Response response;
            if (contentLength >= 0) {
                response = newFixedLengthResponse(
                        Response.Status.lookup(responseCode),
                        contentType,
                        inputStream,
                        contentLength
                );
            } else {
                response = newChunkedResponse(
                        Response.Status.lookup(responseCode),
                        contentType,
                        inputStream
                );
            }

            // Copy headers from remote response
            copyHeaders(connection, response);

            // Add CORS headers for browser compatibility
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Headers", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS");

            Log.d(TAG, "Successfully proxied: " + remoteUrl + " as " + contentType);
            return response;

        } catch (Exception e) {
            Log.e(TAG, "Error proxying request for: " + remoteUrl, e);
            if (connection != null) {
                connection.disconnect();
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                    "Proxy error: " + e.getMessage());
        }
    }

    public void testServerConnectivity() {
        new Thread(() -> {
            try {
                String localIp = NetworkUtils.getLocalIpAddress();
                String testUrl = "http://" + localIp + ":" + getListeningPort() + "/video";

                Log.d(TAG, "=== MEDIA SERVER CONNECTIVITY TEST ===");
                Log.d(TAG, "Server URL: " + testUrl);
                Log.d(TAG, "Is server running: " + isRunning());
                Log.d(TAG, "Port: " + getListeningPort());
                Log.d(TAG, "Network interfaces:");

                // List all network interfaces
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    if (intf.isUp()) {
                        Log.d(TAG, "Interface: " + intf.getDisplayName() + " - " + intf.getName());
                        List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                        for (InetAddress addr : addrs) {
                            Log.d(TAG, "  Address: " + addr.getHostAddress() +
                                    " (v" + (addr instanceof Inet4Address ? "4" : "6") + ")");
                        }
                    }
                }
                Log.d(TAG, "=== END CONNECTIVITY TEST ===");

            } catch (Exception e) {
                Log.e(TAG, "Connectivity test failed", e);
            }
        }).start();
    }

    private String getContentType(String url, String originalContentType) {
        // Determine content type based on URL and original content type
        if (HLS_PATTERN.matcher(url).find()) {
            return "application/vnd.apple.mpegurl";
        } else if (TS_PATTERN.matcher(url).find()) {
            return "video/mp2t";
        } else if (MP4_PATTERN.matcher(url).find()) {
            return "video/mp4";
        } else if (originalContentType != null) {
            return originalContentType;
        } else {
            return "application/octet-stream";
        }
    }

    private void copyHeaders(HttpURLConnection connection, Response response) {
        try {
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    for (String value : entry.getValue()) {
                        // Don't copy content encoding as we're handling the stream directly
                        if (!"Content-Encoding".equalsIgnoreCase(entry.getKey())) {
                            response.addHeader(entry.getKey(), value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error copying headers", e);
        }
    }
}