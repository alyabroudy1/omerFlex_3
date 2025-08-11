package com.omerflex;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.multidex.MultiDex;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.database.DatabaseManager;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;
import com.omerflex.service.network.HttpClientManager;
import com.omerflex.service.config.ConfigManager;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom Application class for OmerFlex.
 * Handles initialization of app-wide components and provides access to them.
 */
public class OmerFlexApplication extends Application {
    private static final String TAG = "OmerFlexApplication";

    private static volatile OmerFlexApplication instance;

    // WeakReference to prevent context leaks
    private static WeakReference<Context> contextReference;

    // Managers - lazily initialized
    private volatile HttpClientManager httpClientManager;
    private volatile ThreadPoolManager threadPoolManager;
    private volatile DatabaseManager databaseManager;
    private volatile ConfigManager configManager;

    // Initialization flags
    private final AtomicBoolean isLoggingInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isConfigInitialized = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Initialize Firebase
        Log.d(TAG, "onCreate: initializing FirebaseApp");
        FirebaseApp app = FirebaseApp.initializeApp(this);
        if (app == null) {
            Log.e(TAG, "onCreate: FirebaseApp.initializeApp returned null!");
        } else {
            Log.d(TAG, "onCreate: FirebaseApp initialized");
        }




        int permissionStatus = ContextCompat.checkSelfPermission(
                getApplicationContext(),
                android.Manifest.permission.PROCESS_OUTGOING_CALLS
        );

        // Compare the status with PERMISSION_GRANTED
        Log.d(TAG, "onCreate: Permission xxx :" + (permissionStatus == PackageManager.PERMISSION_GRANTED));



        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.e(TAG, "No Firebase apps found!");
            } else {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                Log.d(TAG, "FirebaseDatabase instance retrieved successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting FirebaseDatabase instance", e);
        }


        contextReference = new WeakReference<>(getApplicationContext());

        // Initialize logging system first - this is critical
        initializeLogging();

        // Initialize the configuration manager - needed for other components
        initializeConfigManager();

        // Enable strict mode for debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode();
        }


        Logger.i(TAG, "Application initialized. Other components will be lazily initialized on demand.");
    }

    private boolean isDefaultProcess() {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
                if (processInfo.pid == pid) {
                    return getPackageName().equals(processInfo.processName);
                }
            }
        }
        return false;
    }


    /**
     * Initialize application components in a lazy manner
     */
    private void initializeConfigManager() {
        if (isConfigInitialized.compareAndSet(false, true)) {
            try {
                configManager = ConfigManager.getInstance(getApplicationContext());
                Logger.i(TAG, "Configuration manager initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing configuration manager", e);
                // Reset flag so we can try again
                isConfigInitialized.set(false);
            }
        }
    }

    /**
     * Initialize the logging system
     */
    private void initializeLogging() {
        if (isLoggingInitialized.compareAndSet(false, true)) {
            try {
                // Set log level based on build type
                if (BuildConfig.DEBUG) {
                    Logger.setLogLevel(Logger.VERBOSE);
                } else {
                    Logger.setLogLevel(Logger.INFO);
                }

                // Enable caller information in logs for debug builds
                Logger.setIncludeCallerInfo(BuildConfig.DEBUG);

                Logger.d(TAG, "Logging system initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing logging system", e);
                // Reset flag so we can try again
                isLoggingInitialized.set(false);
            }
        }
    }

    /**
     * Enable StrictMode for detecting potential issues in debug builds
     */
    private void enableStrictMode() {
        StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog();

        // API 23+ features
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            threadPolicyBuilder.detectResourceMismatches();
        }

        StrictMode.setThreadPolicy(threadPolicyBuilder.build());

        StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog();

        // API 23+ features 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            vmPolicyBuilder.detectCleartextNetwork();
        }

        // API 28+ features
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            vmPolicyBuilder.detectNonSdkApiUsage();
        }

        StrictMode.setVmPolicy(vmPolicyBuilder.build());

        Logger.i(TAG, "StrictMode enabled for debug build");
    }

    /**
     * Get the application instance
     * @return OmerFlexApplication instance
     */
    @NonNull
    public static OmerFlexApplication getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Application instance is not yet created");
        }
        return instance;
    }

    /**
     * Get application context safely
     * @return The application context
     */
    @NonNull
    public static Context getAppContext() {
        Context context = contextReference != null ? contextReference.get() : null;
        if (context == null) {
            throw new IllegalStateException("Application context is not available");
        }
        return context;
    }

    /**
     * Get the HTTP client manager (lazy initialization)
     * @return HttpClientManager instance
     */
    @NonNull
    public synchronized HttpClientManager getHttpClientManager() {
        if (httpClientManager == null) {
            httpClientManager = HttpClientManager.getInstance(getApplicationContext());
            Logger.i(TAG, "HttpClientManager initialized lazily");
        }
        return httpClientManager;
    }

    /**
     * Get the thread pool manager (lazy initialization)
     * @return ThreadPoolManager instance
     */
    @NonNull
    public synchronized ThreadPoolManager getThreadPoolManager() {
        if (threadPoolManager == null) {
            threadPoolManager = ThreadPoolManager.getInstance();
            Logger.i(TAG, "ThreadPoolManager initialized lazily");
        }
        return threadPoolManager;
    }

    /**
     * Get the database manager (lazy initialization)
     * @return DatabaseManager instance
     */
    @NonNull
    public synchronized DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            databaseManager = DatabaseManager.getInstance(getApplicationContext());
            Logger.i(TAG, "DatabaseManager initialized lazily");
        }
        return databaseManager;
    }

    /**
     * Get the configuration manager (lazy initialization)
     *
     * @return ConfigManager instance
     */
    @NonNull
    public synchronized ConfigManager getConfigManager() {
        if (configManager == null) {
            configManager = ConfigManager.getInstance(getApplicationContext());
            Logger.i(TAG, "ConfigManager initialized lazily");
        }
        return configManager;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Enable multidex support
        MultiDex.install(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // Release non-essential resources based on memory level
        if (level >= TRIM_MEMORY_MODERATE) {
            Logger.i(TAG, "Memory pressure detected (level " + level + "), releasing non-essential resources");
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Logger.w(TAG, "Low memory warning, releasing as many resources as possible");
    }

    @Override
    public void onTerminate() {
        // Clean up resources
        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }

        // Clear references
        contextReference.clear();
        instance = null;

        super.onTerminate();
    }
}