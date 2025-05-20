package com.omerflex.service.network;

import android.content.Context;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Singleton manager for OkHttpClient instances.
 * Provides centralized configuration and reuse of HTTP clients.
 */
public class HttpClientManager {
    private static final String TAG = "HttpClientManager";
    
    // Default timeouts
    private static final int CONNECT_TIMEOUT = 15;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;
    
    // Cache size: 10 MB
    private static final long CACHE_SIZE = 10 * 1024 * 1024;
    
    // Singleton instance
    private static HttpClientManager instance;
    
    // Default client
    private OkHttpClient defaultClient;
    
    // Client with longer timeouts for media operations
    private OkHttpClient mediaClient;
    
    private HttpClientManager(Context context) {
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
        File cacheDir = new File(context.getCacheDir(), "http_cache");
        Cache cache = new Cache(cacheDir, CACHE_SIZE);
        
        // Create logging interceptor for debug builds
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        
        // Build default client
        defaultClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .cache(cache)
                .build();
        
        // Build media client with longer timeouts
        mediaClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT * 2, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT * 2, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT * 2, TimeUnit.SECONDS)
                .cache(cache)
                .build();
    }
    
    /**
     * Get the default OkHttpClient instance
     * @return Default OkHttpClient
     */
    public OkHttpClient getDefaultClient() {
        return defaultClient;
    }
    
    /**
     * Get the OkHttpClient instance configured for media operations
     * @return Media OkHttpClient
     */
    public OkHttpClient getMediaClient() {
        return mediaClient;
    }
    
    /**
     * Create a custom client with specific headers
     * @param builder Builder with custom configuration
     * @return Custom OkHttpClient
     */
    public OkHttpClient getCustomClient(OkHttpClient.Builder builder) {
        if (builder == null) {
            return defaultClient;
        }
        return builder.build();
    }
}