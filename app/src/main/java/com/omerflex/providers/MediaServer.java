package com.omerflex.providers;

import android.util.Log;
import com.omerflex.entity.Movie;
import com.omerflex.service.utils.NetworkUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MediaServer extends NanoHTTPD {
    private static final String TAG = "MediaServer";
    private static MediaServer instance;
    private Movie currentMovie;
    private Map<String, String> headers;

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

        try {
            String originalUrl = currentMovie.getVideoUrl().split("\\|")[0];
            String remoteUrl;

            if ("/video".equals(uri)) {
                remoteUrl = originalUrl;
            } else {
                // Handle HLS relative paths
                int lastSlash = originalUrl.lastIndexOf('/');
                if (lastSlash != -1) {
                    String baseUrl = originalUrl.substring(0, lastSlash + 1);
                    remoteUrl = baseUrl + (uri.startsWith("/") ? uri.substring(1) : uri);
                } else {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
                }
            }
            return proxyRequest(session, remoteUrl);

        } catch (Exception e) {
            Log.e(TAG, "Error determining remote URL", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal server error");
        }
    }

    private Response proxyRequest(IHTTPSession session, String remoteUrl) {
        HttpURLConnection connection = null;
        try {
            Log.d(TAG, "Proxying request for: " + remoteUrl);

            URL url = new URL(remoteUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Add original headers from the movie object
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            // Copy range header from the client request
            String rangeHeader = session.getHeaders().get("range");
            if (rangeHeader != null) {
                connection.setRequestProperty("Range", rangeHeader);
                Log.d(TAG, "Forwarding range header: " + rangeHeader);
            }

            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            long contentLength = -1;
            String contentLengthHeader = connection.getHeaderField("Content-Length");
            if (contentLengthHeader != null) {
                try {
                    contentLength = Long.parseLong(contentLengthHeader);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse Content-Length header: " + contentLengthHeader);
                }
            }

            Log.d(TAG, "Original server response: " + responseCode +
                    ", Type: " + contentType + ", Length: " + contentLength);

            Response.IStatus status = Response.Status.lookup(responseCode);
            if (status == null) {
                final int finalResponseCode = responseCode;
                final String finalDescription = connection.getResponseMessage();
                status = new Response.IStatus() {
                    @Override
                    public String getDescription() {
                        return finalDescription;
                    }

                    @Override
                    public int getRequestStatus() {
                        return finalResponseCode;
                    }
                };
            }

            InputStream inputStream = null;
            try {
                inputStream = connection.getInputStream();
            } catch (IOException e) {
                inputStream = connection.getErrorStream();
            }

            Response response;
            if (contentLength != -1) {
                response = newFixedLengthResponse(status, contentType, inputStream, contentLength);
            } else {
                response = newChunkedResponse(status, contentType, inputStream);
            }

            // Copy all headers from original response to our response
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                if (entry.getKey() != null) {
                    for (String value : entry.getValue()) {
                        response.addHeader(entry.getKey(), value);
                    }
                }
            }

            response.addHeader("Access-Control-Allow-Origin", "*");
            Log.d(TAG, "Streaming started for " + remoteUrl);
            return response;

        } catch (Exception e) {
            Log.e(TAG, "Error serving request for " + remoteUrl, e);
            if (connection != null) {
                connection.disconnect();
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                    "Error: " + e.getMessage());
        }
    }
}
