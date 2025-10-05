package com.omerflex.providers;

import android.util.Log;
import com.omerflex.entity.Movie;
import com.omerflex.service.utils.NetworkUtils;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

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
            stopServer(); // Stop existing server
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
        try {
            stop();
            Log.d(TAG, "Media server stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping media server", e);
        }
    }

    public boolean isRunning() {
        return wasStarted();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();

        Log.d(TAG, "Request: " + method + " " + uri);

        if ("/video".equals(uri) && Method.GET.equals(session.getMethod())) {
            return serveVideo(session);
        } else if ("/".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html",
                    "<html><body><h1>Media Server</h1><p>Server is running</p></body></html>");
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
        }
    }

    private Response serveVideo(IHTTPSession session) {
        try {
            String videoUrl = currentMovie.getVideoUrl().split("\\|")[0];
            Log.d(TAG, "Proxying video: " + videoUrl);

            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Add original headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            // Copy range header if present
            String rangeHeader = session.getHeaders().get("range");
            if (rangeHeader != null) {
                connection.setRequestProperty("Range", rangeHeader);
                Log.d(TAG, "Forwarding range header: " + rangeHeader);
            }

            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            long contentLength = connection.getContentLengthLong();

            Log.d(TAG, "Original server response: " + responseCode +
                    ", Type: " + contentType + ", Length: " + contentLength);

            // Create response
            Response.Status status = responseCode == 206 ?
                    Response.Status.PARTIAL_CONTENT : Response.Status.OK;

            Response response = newChunkedResponse(status, contentType, connection.getInputStream());

            // Set headers
            response.addHeader("Accept-Ranges", "bytes");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            response.addHeader("Access-Control-Allow-Headers", "Range");

            // Handle content range
            String contentRange = connection.getHeaderField("Content-Range");
            if (contentRange != null) {
                response.addHeader("Content-Range", contentRange);
            }

            Log.d(TAG, "Video streaming started");
            return response;

        } catch (Exception e) {
            Log.e(TAG, "Error serving video", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                    "Error: " + e.getMessage());
        }
    }
}