package com.omerflex.service.network;

import android.content.Context;

import androidx.annotation.NonNull;

import com.omerflex.OmerFlexApplication;
import com.omerflex.service.config.ConfigManager;
import com.omerflex.service.logging.Logger;
import com.omerflex.service.utils.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Singleton manager for OkHttpClient instances.
 * Provides centralized configuration and reuse of HTTP clients with retry policies.
 */
public class HttpClientManager {
    private static final String TAG = "HttpClientManager";
    
    // Singleton instance
    private static volatile HttpClientManager instance;

    // Clients
    private OkHttpClient defaultClient;
    private OkHttpClient mediaClient;
    private OkHttpClient shortTimeoutClient;

    // Cache
    private Cache cache;

    // Config
    private final ConfigManager configManager;

    private HttpClientManager(Context context) {
        configManager = OmerFlexApplication.getInstance().getConfigManager();
        initializeClients(context);
    }
    
    /**
     * Get the singleton instance of HttpClientManager
     * @param context Application context
     * @return HttpClientManager instance
     */
    public static synchronized HttpClientManager getInstance(Context context) {
        if (instance == null) {
            instance = new HttpClientManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize HTTP clients with appropriate configurations
     * @param context Application context
     */
    private void initializeClients(Context context) {
        // Create cache directory
        int cacheSizeMb = configManager.getInt("network.cache_size_mb", 10);
        long cacheSize = cacheSizeMb * 1024 * 1024L;

        File cacheDir = new File(context.getCacheDir(), "http_cache");
        cache = new Cache(cacheDir, cacheSize);
        
        // Create logging interceptor for debug builds
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Logger.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        // Get configuration values
        int connectTimeout = configManager.getInt("network.connect_timeout_ms", 15000);
        int readTimeout = configManager.getInt("network.read_timeout_ms", 30000);
        int writeTimeout = configManager.getInt("network.write_timeout_ms", 30000);

        // Create retry interceptor
        RetryInterceptor retryInterceptor = new RetryInterceptor(
                configManager.getInt("network.retry_count", 3),
                configManager.getInt("network.retry_delay_ms", 1000)
        );

        // Build default client
        defaultClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(retryInterceptor)
                .build();
        
        // Build media client with longer timeouts
        mediaClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout * 2, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout * 2, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout * 2, TimeUnit.MILLISECONDS)
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(retryInterceptor)
                .build();

        // Build short timeout client for quick operations
        shortTimeoutClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout / 3, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout / 3, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout / 3, TimeUnit.MILLISECONDS)
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .build(); // No retry for short timeout client

        Logger.i(TAG, "HTTP clients initialized with cache size: " + cacheSizeMb + "MB");
    }
    
    /**
     * Reload client configurations from ConfigManager
     */
    public synchronized void reloadConfig() {
        Logger.d(TAG, "Reloading HTTP client configurations");
        Context context = OmerFlexApplication.getAppContext();
        initializeClients(context);
    }

    /**
     * Get the default OkHttpClient instance
     * @return Default OkHttpClient
     */
    @NonNull
    public OkHttpClient getDefaultClient() {
        return defaultClient;
    }
    
    /**
     * Get the OkHttpClient instance configured for media operations
     * @return Media OkHttpClient
     */
    @NonNull
    public OkHttpClient getMediaClient() {
        return mediaClient;
    }
    
    /**
     * Get the OkHttpClient instance configured for quick operations
     * @return Short timeout OkHttpClient
     */
    @NonNull
    public OkHttpClient getShortTimeoutClient() {
        return shortTimeoutClient;
    }

    /**
     * Create a custom client with specific headers
     * @param builder Builder with custom configuration
     * @return Custom OkHttpClient
     */
    @NonNull
    public OkHttpClient getCustomClient(OkHttpClient.Builder builder) {
        if (builder == null) {
            return defaultClient;
        }
        return builder.build();
    }

    /**
     * Create an OkHttpClient.Builder pre-configured with common settings
     *
     * @return Configured builder
     */
    @NonNull
    public OkHttpClient.Builder newClientBuilder() {
        return defaultClient.newBuilder();
    }

    /**
     * Create a builder with offline mode enforced
     *
     * @return Builder configured for offline mode
     */
    @NonNull
    public OkHttpClient.Builder newOfflineClientBuilder() {
        return defaultClient.newBuilder()
                .addInterceptor(new OfflineCacheInterceptor());
    }

    /**
     * Clear the HTTP cache
     */
    public void clearCache() {
        try {
            if (cache != null) {
                cache.evictAll();
                Logger.i(TAG, "HTTP cache cleared");
            }
        } catch (IOException e) {
            Logger.e(TAG, "Error clearing HTTP cache", e);
        }
    }

    /**
     * Interceptor to handle retries for failed requests
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private final int retryDelayMs;

        RetryInterceptor(int maxRetries, int retryDelayMs) {
            this.maxRetries = maxRetries;
            this.retryDelayMs = retryDelayMs;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;

            int retryCount = 0;
            boolean retry;

            do {
                retry = false;

                try {
                    if (retryCount > 0) {
                        Logger.d(TAG, "Retrying request to " + request.url() +
                                " (attempt " + (retryCount + 1) + " of " + maxRetries + ")");

                        // Exponential backoff
                        Thread.sleep(retryDelayMs * (long) Math.pow(2, retryCount - 1));
                    }

                    response = chain.proceed(request);

                    // Retry on certain error codes
                    if (response != null &&
                            (response.code() == 408 || response.code() >= 500) &&
                            retryCount < maxRetries) {
                        retry = true;
                        response.close();
                    }

                } catch (IOException e) {
                    exception = e;

                    // Only retry on timeout or connection issues
                    if ((e instanceof SocketTimeoutException ||
                            e.getMessage() != null &&
                                    (e.getMessage().contains("connection") ||
                                            e.getMessage().contains("timeout"))) &&
                            retryCount < maxRetries) {
                        retry = true;
                        Logger.d(TAG, "I/O error for request to " + request.url() +
                                ": " + e.getMessage() + ", will retry");
                    } else {
                        throw e;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }

                retryCount++;

            } while (retry);

            if (response != null) {
                return response;
            } else {
                throw exception != null ? exception :
                        new IOException("Unknown error executing request");
            }
        }
    }

    /**
     * Interceptor to force cache use when offline
     */
    private static class OfflineCacheInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            // Force cache if we're configured to be offline
            if (!NetworkUtils.isNetworkAvailable()) {
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale(7, TimeUnit.DAYS)
                        .build();

                request = request.newBuilder()
                        .cacheControl(cacheControl)
                        .build();

                Logger.d(TAG, "Offline mode: using cached response for " + request.url());
            }

            return chain.proceed(request);
        }
    }
}