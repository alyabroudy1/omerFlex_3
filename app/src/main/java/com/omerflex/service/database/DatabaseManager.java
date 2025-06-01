package com.omerflex.service.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.omerflex.OmerFlexApplication;
import com.omerflex.entity.Movie;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.config.ConfigManager;
import com.omerflex.service.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Manager class for database operations.
 * Provides optimized access to the database with proper threading and caching.
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    // Singleton instance
    private static volatile DatabaseManager instance;

    // Database helper
    private final MovieDbHelper dbHelper;

    // Thread pool for database operations
    private final Executor diskExecutor;

    // Cache for frequently accessed data
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // Configuration
    private final boolean cacheEnabled;
    private final int maxCacheEntries;
    private final long cacheTtlMs;

    private DatabaseManager(Context context) {
        // Initialize the database helper
        dbHelper = MovieDbHelper.getInstance(context.getApplicationContext());

        // Get thread pool from ThreadPoolManager
        diskExecutor = ThreadPoolManager.getInstance().getDiskExecutor();

        // Get configuration
        ConfigManager configManager = OmerFlexApplication.getInstance().getConfigManager();
        cacheEnabled = configManager.getBoolean("feature.enable_cache", true);
        maxCacheEntries = configManager.getInt("db.cache_size_entries", 500);
        cacheTtlMs = configManager.getInt("db.cache_ttl_ms", 300000); // 5 minutes default

        Logger.i(TAG, "DatabaseManager initialized, cache " +
                (cacheEnabled ? "enabled (" + maxCacheEntries + " entries)" : "disabled"));
    }

    /**
     * Get the singleton instance of DatabaseManager
     * @param context Application context
     * @return DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Execute a database read operation asynchronously
     * @param operation Database operation to execute
     */
    public void executeRead(final DatabaseOperation operation) {
        diskExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getReadableDatabase();
                operation.execute(db);
            } catch (Exception e) {
                Logger.e(TAG, "Error executing database read operation", e);
                operation.onError(e);
            }
        });
    }

    /**
     * Execute a database write operation asynchronously
     * @param operation Database operation to execute
     */
    public void executeWrite(final DatabaseOperation operation) {
        diskExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                operation.execute(db);
                db.setTransactionSuccessful();
                // Invalidate cache after writes
                if (cacheEnabled) {
                    clearCache();
                }
            } catch (Exception e) {
                Logger.e(TAG, "Error executing database write operation", e);
                operation.onError(e);
            } finally {
                if (db != null && db.inTransaction()) {
                    db.endTransaction();
                }
            }
        });
    }

    /**
     * Execute a database read operation synchronously
     * @param operation Database operation to execute
     */
    public void executeReadSync(final DatabaseOperation operation) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getReadableDatabase();
            operation.execute(db);
        } catch (Exception e) {
            Logger.e(TAG, "Error executing database read operation", e);
            operation.onError(e);
        }
    }

    /**
     * Execute a database write operation synchronously
     * @param operation Database operation to execute
     */
    public void executeWriteSync(final DatabaseOperation operation) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            operation.execute(db);
            db.setTransactionSuccessful();
            // Invalidate cache after writes
            if (cacheEnabled) {
                clearCache();
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error executing database write operation", e);
            operation.onError(e);
        } finally {
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
        }
    }

    /**
     * Get the MovieDbHelper instance
     * @return MovieDbHelper instance
     */
    @NonNull
    public MovieDbHelper getDbHelper() {
        return dbHelper;
    }

    /**
     * Get a cached value or load it from database if not found
     *
     * @param key    Cache key
     * @param loader Loader function to execute if value not cached
     * @param <T>    Type of the cached value
     * @return The cached or loaded value
     */
    @Nullable
    public <T> T getWithCache(String key, CacheLoader<T> loader) {
        if (!cacheEnabled) {
            return loader.load();
        }

        // Check if value is in cache and not expired
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            Logger.d(TAG, "Cache hit for key: " + key);
            return (T) entry.getValue();
        }

        // If not in cache or expired, load the value
        Logger.d(TAG, "Cache miss for key: " + key + ", loading from database");
        T value = loader.load();

        if (value != null) {
            cache.put(key, new CacheEntry(value, System.currentTimeMillis() + cacheTtlMs));

            // Trim cache if it exceeds the maximum size
            if (cache.size() > maxCacheEntries) {
                trimCache();
            }
        }

        return value;
    }

    /**
     * Put a value in the cache
     *
     * @param key   Cache key
     * @param value Value to cache
     */
    public void putInCache(String key, Object value) {
        if (cacheEnabled && value != null) {
            cache.put(key, new CacheEntry(value, System.currentTimeMillis() + cacheTtlMs));
        }
    }

    /**
     * Remove a value from the cache
     *
     * @param key Cache key
     */
    public void removeFromCache(String key) {
        if (cacheEnabled) {
            cache.remove(key);
        }
    }

    /**
     * Clear the entire cache
     */
    public void clearCache() {
        if (cacheEnabled) {
            Logger.d(TAG, "Clearing database cache (" + cache.size() + " entries)");
            cache.clear();
        }
    }

    /**
     * Trim the cache by removing least recently accessed entries
     */
    private void trimCache() {
        int toRemove = cache.size() - maxCacheEntries;
        if (toRemove <= 0) return;

        Logger.d(TAG, "Trimming database cache, removing " + toRemove + " entries");

        // Find the oldest entries
        List<Map.Entry<String, CacheEntry>> entries = new ArrayList<>(cache.entrySet());
        Collections.sort(entries, (a, b) -> Long.compare(a.getValue().getExpiryTime(), b.getValue().getExpiryTime()));

        // Remove the oldest entries
        for (int i = 0; i < toRemove && i < entries.size(); i++) {
            cache.remove(entries.get(i).getKey());
        }
    }

    /**
     * Reset the singleton instance (for testing purposes)
     */
    public static synchronized void reset() {
        if (instance != null) {
            instance.clearCache();
            instance = null;
        }
    }

    /**
     * Interface for database operations
     */
    public interface DatabaseOperation {
        void execute(SQLiteDatabase db);
        default void onError(Exception e) {
            // Default implementation does nothing
        }
    }

    /**
     * Interface for loading cache values
     *
     * @param <T> Type of the value to load
     */
    public interface CacheLoader<T> {
        T load();
    }

    /**
     * Cache entry with expiry time
     */
    private static class CacheEntry {
        private final Object value;
        private final long expiryTime;

        CacheEntry(Object value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        Object getValue() {
            return value;
        }

        long getExpiryTime() {
            return expiryTime;
        }
    }
}