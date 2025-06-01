package com.omerflex.server;

import android.content.Context;
import android.net.Uri;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.omerflex.OmerFlexApplication;
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;
import com.omerflex.service.network.HttpClientManager;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.JSONObject;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class to optimize server operations and provide centralized functionality
 * for document fetching, caching, and other common server tasks.
 */
public class ServerOptimizer {
    private static final String TAG = "ServerOptimizer";
    private static final int MAX_REDIRECTS = 5;
    private static final int CACHE_SIZE = 100;
    private static final long CACHE_EXPIRY_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int CONNECTION_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    // Static instance for singleton access
    private static volatile ServerOptimizer instance;

    // Document cache
    private static final LruCache<String, CachedDocument> documentCache = new LruCache<>(CACHE_SIZE);

    // URL -> OkHttp Response cache to reduce requests
    private static final LruCache<String, CachedResponse> responseCache = new LruCache<>(20);

    // Dependencies
    private final ThreadPoolManager threadPoolManager;
    private final HttpClientManager httpClientManager;
    private final OkHttpClient okHttpClient;

    /**
     * Cache wrapper class with timestamp
     */
    private static class CachedDocument {
        final Document document;
        final long timestamp;

        CachedDocument(Document document) {
            this.document = document;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    /**
     * Cache wrapper for OkHttp responses
     */
    private static class CachedResponse {
        final String content;
        final Headers headers;
        final int statusCode;
        final long timestamp;

        CachedResponse(Response response, String content) throws IOException {
            this.content = content;
            this.headers = response.headers();
            this.statusCode = response.code();
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    /**
     * Private constructor to enforce singleton pattern
     */
    private ServerOptimizer(Context context) {
        OmerFlexApplication app = OmerFlexApplication.getInstance();
        this.threadPoolManager = app.getThreadPoolManager();
        this.httpClientManager = app.getHttpClientManager();
        this.okHttpClient = httpClientManager.getMediaClient();

        Logger.i(TAG, "ServerOptimizer initialized");
    }

    /**
     * Initialize or get the server optimizer instance
     */
    public static synchronized ServerOptimizer initialize(Context context) {
        if (instance == null) {
            instance = new ServerOptimizer(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Get the singleton instance, throws exception if not initialized
     */
    public static ServerOptimizer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ServerOptimizer not initialized. Call initialize() first");
        }
        return instance;
    }

    /**
     * Get a document from cache or fetch it from the network
     *
     * @param url    The URL to fetch
     * @param config The server configuration
     * @return The fetched document or null if there was an error
     */
    public static Document getDocumentWithCache(String url, ServerConfig config) {
        // Try to get from cache first
        synchronized (documentCache) {
            CachedDocument cached = documentCache.get(url);
            if (cached != null && !cached.isExpired()) {
                Logger.d(TAG, "Document cache hit for URL: " + url);
                return cached.document;
            }
        }

        // Try the response cache first
        String content = null;
        synchronized (responseCache) {
            CachedResponse cachedResponse = responseCache.get(url);
            if (cachedResponse != null && !cachedResponse.isExpired()) {
                Logger.d(TAG, "Response cache hit for URL: " + url);
                content = cachedResponse.content;
            }
        }

        Document doc = null;

        // If we have cached content, parse it
        if (content != null) {
            try {
                doc = Jsoup.parse(content, url);
                cacheDocument(url, doc);
                return doc;
            } catch (Exception e) {
                Logger.e(TAG, "Error parsing cached content for URL: " + url, e);
            }
        }

        // Not in cache, fetch from network
        if (instance != null && instance.okHttpClient != null) {
            doc = instance.fetchWithOkHttp(url, config);
        } else {
            doc = fetchWithJsoup(url, config);
        }

        // Cache successful results
        if (doc != null) {
            cacheDocument(url, doc);
        }

        return doc;
    }

    /**
     * Cache a document for future use
     */
    private static void cacheDocument(String url, Document doc) {
        synchronized (documentCache) {
            documentCache.put(url, new CachedDocument(doc));
        }
    }

    /**
     * Fetch document using OkHttp with connection pooling
     */
    private Document fetchWithOkHttp(String url, ServerConfig config) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml");

        // Add headers from config
        if (config != null && config.getHeaders() != null) {
            for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    requestBuilder.header(key, value);
                }
            }

            // Add cookies
            if (config.getMappedCookies() != null && !config.getMappedCookies().isEmpty()) {
                StringBuilder cookieHeader = new StringBuilder();
                for (Map.Entry<String, String> entry : config.getMappedCookies().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key != null && value != null) {
                        if (cookieHeader.length() > 0) {
                            cookieHeader.append("; ");
                        }
                        cookieHeader.append(key).append("=").append(value);
                    }
                }

                if (cookieHeader.length() > 0) {
                    requestBuilder.header("Cookie", cookieHeader.toString());
                }
            }
        }

        String currentUrl = url;
        int redirectCount = 0;

        while (redirectCount < MAX_REDIRECTS) {
            try {
                // Update URL in case of redirect
                Request request = requestBuilder.url(currentUrl).build();
                Response response = okHttpClient.newCall(request).execute();

                try {
                    int statusCode = response.code();

                    if (statusCode == HttpURLConnection.HTTP_OK && response.body() != null) {
                        String html = response.body().string();

                        // Cache the response
                        synchronized (responseCache) {
                            responseCache.put(currentUrl, new CachedResponse(response, html));
                        }

                        return Jsoup.parse(html, currentUrl);
                    } else if (isRedirect(statusCode)) {
                        String location = response.header("Location");
                        if (location == null || location.isEmpty()) {
                            Logger.w(TAG, "Redirect without Location header: " + currentUrl);
                            if (response.body() != null) {
                                return Jsoup.parse(response.body().string(), currentUrl);
                            }
                            return null;
                        }

                        currentUrl = resolveRedirectUrl(currentUrl, location);
                        redirectCount++;
                        Logger.d(TAG, "Following redirect to: " + currentUrl);

                        // Check if redirected URL is in cache
                        synchronized (documentCache) {
                            CachedDocument cached = documentCache.get(currentUrl);
                            if (cached != null && !cached.isExpired()) {
                                Logger.d(TAG, "Cache hit after redirect for URL: " + currentUrl);
                                return cached.document;
                            }
                        }
                    } else {
                        Logger.e(TAG, "Unexpected status " + statusCode + " for " + currentUrl);
                        if (response.body() != null) {
                            return Jsoup.parse(response.body().string(), currentUrl);
                        }
                        return null;
                    }
                } finally {
                    if (response.body() != null) {
                        response.close();
                    }
                }
            } catch (IOException e) {
                Logger.e(TAG, "Network error for URL: " + currentUrl, e);
                return null;
            }
        }

        Logger.w(TAG, "Too many redirects (" + MAX_REDIRECTS + ") for: " + url);
        return null;
    }

    /**
     * Fallback method to fetch with Jsoup
     */
    private static Document fetchWithJsoup(String url, ServerConfig config) {
        int redirectCount = 0;
        String currentUrl = url;

        try {
            while (redirectCount < MAX_REDIRECTS) {
                // Create connection with appropriate settings
                Connection connection = Jsoup.connect(currentUrl)
                        .followRedirects(false)
                        .timeout(CONNECTION_TIMEOUT)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true);

                if (config != null) {
                    // Add headers
                    if (config.getHeaders() != null) {
                        connection.headers(config.getHeaders());
                    }

                    // Add cookies
                    if (config.getMappedCookies() != null) {
                        connection.cookies(config.getMappedCookies());
                    }
                }

                Connection.Response response = connection.execute();
                int statusCode = response.statusCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    return response.parse();
                } else if (isRedirect(statusCode)) {
                    String location = response.header("Location");
                    if (location == null || location.isEmpty()) {
                        Logger.w(TAG, "Redirect without Location header: " + currentUrl);
                        return response.parse();
                    }

                    currentUrl = resolveRedirectUrl(currentUrl, location);
                    redirectCount++;
                    Logger.d(TAG, "Following redirect to: " + currentUrl);
                } else {
                    Logger.e(TAG, "Unexpected status " + statusCode + " for " + currentUrl);
                    return statusCode == HttpURLConnection.HTTP_NOT_FOUND ? null : response.parse();
                }
            }

            Logger.w(TAG, "Too many redirects (" + MAX_REDIRECTS + ") for: " + url);
        } catch (IOException e) {
            Logger.e(TAG, "Network error for URL: " + currentUrl, e);
        } catch (Exception e) {
            Logger.e(TAG, "Unexpected error processing URL: " + currentUrl, e);
        }

        return null;
    }

    /**
     * Check if HTTP status code is a redirect
     */
    private static boolean isRedirect(int statusCode) {
        return statusCode >= HttpURLConnection.HTTP_MOVED_PERM
                && statusCode < HttpURLConnection.HTTP_BAD_REQUEST;
    }

    /**
     * Resolve redirect URL with proper handling of relative URLs
     */
    private static String resolveRedirectUrl(String baseUrl, String location) throws MalformedURLException {
        if (location.startsWith("http")) {
            return location;
        }
        URL base = new URL(baseUrl);
        return new URL(base, location).toString();
    }

    /**
     * Extract JSON data from string
     */
    public static JSONObject extractJson(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (Exception e) {
            Logger.e(TAG, "Error parsing JSON: " + jsonString, e);
            return null;
        }
    }

    /**
     * Clear all caches
     */
    public static void clearCache() {
        synchronized (documentCache) {
            documentCache.evictAll();
        }

        synchronized (responseCache) {
            responseCache.evictAll();
        }

        Logger.i(TAG, "ServerOptimizer caches cleared");
    }

    /**
     * Check if a URL is safe to fetch (not malicious)
     *
     * @param url The URL to check
     * @return true if the URL is safe, false otherwise
     */
    public static boolean isSafeUrl(@NonNull String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();

            // Check if the URL has a valid scheme
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                Logger.w(TAG, "Invalid URL scheme: " + url);
                return false;
            }

            // Check if the URL has a host
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                Logger.w(TAG, "Invalid URL host: " + url);
                return false;
            }

            // Check for potentially malicious URLs
            String urlLower = url.toLowerCase();
            if (urlLower.contains("javascript:") || urlLower.contains("data:")) {
                Logger.w(TAG, "Potentially malicious URL: " + url);
                return false;
            }

            return true;
        } catch (Exception e) {
            Logger.e(TAG, "Error checking URL safety: " + url, e);
            return false;
        }
    }

    /**
     * Extract all movie elements from a document with specified selector
     *
     * @param doc       The document to extract from
     * @param selector  The CSS selector for movie elements
     * @param processor The processor to extract movie details
     * @return List of extracted movies
     */
    public static ArrayList<Movie> extractMovies(@NonNull Document doc, @NonNull String selector,
                                                 @NonNull MovieElementProcessor processor) {
        ArrayList<Movie> movies = new ArrayList<>();

        try {
            Elements elements = doc.select(selector);
            if (elements.isEmpty()) {
                Logger.d(TAG, "No movie elements found with selector: " + selector);
                return movies;
            }

            for (Element element : elements) {
                Movie movie = processor.processElement(element);
                if (movie != null) {
                    movies.add(movie);
                }
            }

            Logger.d(TAG, "Extracted " + movies.size() + " movies with selector: " + selector);
        } catch (Exception e) {
            Logger.e(TAG, "Error extracting movies with selector: " + selector, e);
        }

        return movies;
    }

    /**
     * Interface for processing movie elements
     */
    public interface MovieElementProcessor {
        @Nullable
        Movie processElement(@NonNull Element element);
    }
}
