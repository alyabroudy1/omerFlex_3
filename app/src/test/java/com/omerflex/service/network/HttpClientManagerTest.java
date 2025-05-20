package com.omerflex.service.network;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class HttpClientManagerTest {

    private HttpClientManager httpClientManager;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        httpClientManager = HttpClientManager.getInstance(context);
    }

    @Test
    public void testGetInstance() {
        // Verify that getInstance returns a non-null instance
        assertNotNull(httpClientManager);
        
        // Verify that getInstance always returns the same instance
        HttpClientManager anotherInstance = HttpClientManager.getInstance(context);
        assertSame(httpClientManager, anotherInstance);
    }

    @Test
    public void testGetDefaultClient() {
        // Verify that getDefaultClient returns a non-null client
        OkHttpClient defaultClient = httpClientManager.getDefaultClient();
        assertNotNull(defaultClient);
        
        // Verify default client configuration
        assertEquals(15, defaultClient.connectTimeoutMillis() / 1000);
        assertEquals(30, defaultClient.readTimeoutMillis() / 1000);
        assertEquals(30, defaultClient.writeTimeoutMillis() / 1000);
        assertNotNull(defaultClient.cache());
    }

    @Test
    public void testGetMediaClient() {
        // Verify that getMediaClient returns a non-null client
        OkHttpClient mediaClient = httpClientManager.getMediaClient();
        assertNotNull(mediaClient);
        
        // Verify media client configuration (longer timeouts)
        assertEquals(30, mediaClient.connectTimeoutMillis() / 1000);
        assertEquals(60, mediaClient.readTimeoutMillis() / 1000);
        assertEquals(60, mediaClient.writeTimeoutMillis() / 1000);
        assertNotNull(mediaClient.cache());
    }

    @Test
    public void testGetCustomClient() {
        // Test with null builder
        OkHttpClient clientWithNullBuilder = httpClientManager.getCustomClient(null);
        assertNotNull(clientWithNullBuilder);
        assertSame(httpClientManager.getDefaultClient(), clientWithNullBuilder);
        
        // Test with custom builder
        OkHttpClient.Builder customBuilder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS);
        
        OkHttpClient customClient = httpClientManager.getCustomClient(customBuilder);
        assertNotNull(customClient);
        assertEquals(5, customClient.connectTimeoutMillis() / 1000);
        assertEquals(10, customClient.readTimeoutMillis() / 1000);
        assertEquals(10, customClient.writeTimeoutMillis() / 1000);
    }

    @Test
    public void testCacheConfiguration() {
        // Verify that cache is properly configured
        File expectedCacheDir = new File(context.getCacheDir(), "http_cache");
        
        // Verify cache exists in both clients
        assertNotNull(httpClientManager.getDefaultClient().cache());
        assertNotNull(httpClientManager.getMediaClient().cache());
    }
}