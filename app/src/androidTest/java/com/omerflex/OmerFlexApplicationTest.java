package com.omerflex;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.database.DatabaseManager;
import com.omerflex.service.network.HttpClientManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
public class OmerFlexApplicationTest {

    private OmerFlexApplication application;

    @Before
    public void setUp() {
        // Get the application instance
        application = (OmerFlexApplication) ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testApplicationInstance() {
        // Verify that the application instance is not null
        assertNotNull(application);
        
        // Verify that getInstance returns the same instance
        assertSame(application, OmerFlexApplication.getInstance());
    }

    @Test
    public void testHttpClientManager() {
        // Verify that the HttpClientManager is properly initialized
        HttpClientManager httpClientManager = application.getHttpClientManager();
        assertNotNull(httpClientManager);
        
        // Verify that getHttpClientManager always returns the same instance
        assertSame(httpClientManager, application.getHttpClientManager());
        
        // Verify that the default client is properly initialized
        assertNotNull(httpClientManager.getDefaultClient());
        
        // Verify that the media client is properly initialized
        assertNotNull(httpClientManager.getMediaClient());
    }

    @Test
    public void testThreadPoolManager() {
        // Verify that the ThreadPoolManager is properly initialized
        ThreadPoolManager threadPoolManager = application.getThreadPoolManager();
        assertNotNull(threadPoolManager);
        
        // Verify that getThreadPoolManager always returns the same instance
        assertSame(threadPoolManager, application.getThreadPoolManager());
        
        // Verify that the executors are properly initialized
        assertNotNull(threadPoolManager.getNetworkExecutor());
        assertNotNull(threadPoolManager.getDiskExecutor());
        assertNotNull(threadPoolManager.getLightweightExecutor());
        assertNotNull(threadPoolManager.getScheduledExecutor());
        assertNotNull(threadPoolManager.getMainThreadHandler());
    }

    @Test
    public void testDatabaseManager() {
        // Verify that the DatabaseManager is properly initialized
        DatabaseManager databaseManager = application.getDatabaseManager();
        assertNotNull(databaseManager);
        
        // Verify that getDatabaseManager always returns the same instance
        assertSame(databaseManager, application.getDatabaseManager());
        
        // Verify that the database helper is properly initialized
        assertNotNull(databaseManager.getDbHelper());
    }

    @Test
    public void testApplicationContext() {
        // Verify that the application context is properly set
        Context context = application.getApplicationContext();
        assertNotNull(context);
        
        // Verify that the application context is the same as the application
        assertEquals(application, context);
    }
}