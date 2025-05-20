package com.omerflex;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import androidx.multidex.MultiDex;

import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.database.DatabaseManager;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;
import com.omerflex.service.network.HttpClientManager;

/**
 * Custom Application class for OmerFlex.
 * Handles initialization of app-wide components and provides access to them.
 */
public class OmerFlexApplication extends Application {
    private static final String TAG = "OmerFlexApplication";

    private static OmerFlexApplication instance;

    // Managers
    private HttpClientManager httpClientManager;
    private ThreadPoolManager threadPoolManager;
    private DatabaseManager databaseManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize components
        initializeComponents();

        // Enable strict mode for debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode();
        }
    }

    /**
     * Initialize application components
     */
    private void initializeComponents() {
        try {
            // Initialize logging system first
            initializeLogging();

            // Initialize HTTP client manager
            httpClientManager = HttpClientManager.getInstance(this);

            // Initialize thread pool manager
            threadPoolManager = ThreadPoolManager.getInstance();

            // Initialize database manager
            databaseManager = DatabaseManager.getInstance(this);

            // Initialize other components as needed

            Logger.i(TAG, "Application components initialized successfully");
        } catch (Exception e) {
            // Fallback to Android's Log since our Logger might not be initialized
            Log.e(TAG, "Error initializing application components", e);
        }
    }

    /**
     * Initialize the logging system
     */
    private void initializeLogging() {
        // Set log level based on build type
        if (BuildConfig.DEBUG) {
            Logger.setLogLevel(Logger.VERBOSE);
        } else {
            Logger.setLogLevel(Logger.INFO);
        }

        // Enable caller information in logs for debug builds
        Logger.setIncludeCallerInfo(BuildConfig.DEBUG);

        Logger.d(TAG, "Logging system initialized");
    }

    /**
     * Enable StrictMode for detecting potential issues in debug builds
     */
    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }

    /**
     * Get the application instance
     * @return OmerFlexApplication instance
     */
    public static OmerFlexApplication getInstance() {
        return instance;
    }

    /**
     * Get the HTTP client manager
     * @return HttpClientManager instance
     */
    public HttpClientManager getHttpClientManager() {
        return httpClientManager;
    }

    /**
     * Get the thread pool manager
     * @return ThreadPoolManager instance
     */
    public ThreadPoolManager getThreadPoolManager() {
        return threadPoolManager;
    }

    /**
     * Get the database manager
     * @return DatabaseManager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Enable multidex support
        MultiDex.install(this);
    }

    @Override
    public void onTerminate() {
        // Clean up resources
        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }

        super.onTerminate();
    }
}
