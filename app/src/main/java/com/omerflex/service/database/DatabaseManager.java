package com.omerflex.service.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
// import android.database.sqlite.SQLiteDatabase; // No longer needed
import android.util.Log;
import android.content.Context; // Added for @ApplicationContext

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// import com.omerflex.OmerFlexApplication; // No longer needed for config
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.config.ConfigManager;
import com.omerflex.service.database.dao.IptvDao;
import com.omerflex.service.database.dao.MovieDao;
import com.omerflex.service.database.dao.MovieHistoryDao;
import com.omerflex.service.database.dao.ServerConfigDao;
import com.omerflex.service.logging.Logger;

import dagger.hilt.android.qualifiers.ApplicationContext; // Added for Hilt
import javax.inject.Inject; // Added for Hilt
import javax.inject.Singleton; // Added for Hilt

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Manager class for database operations.
 * Provides optimized access to the database with proper threading and caching.
 */
@Singleton
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    // DAOs will be injected
    private final MovieDao movieDao;
    private final ServerConfigDao serverConfigDao;
    private final MovieHistoryDao movieHistoryDao;
    private final IptvDao iptvDao;

    // Thread pool for database operations
    private final Executor diskExecutor;
    private final Handler mainThreadHandler;

    // Cache for frequently accessed data
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // Configuration
    private final boolean cacheEnabled;
    private final int maxCacheEntries;
    private final long cacheTtlMs;

    @Inject
    public DatabaseManager(@ApplicationContext Context context,
                           ThreadPoolManager threadPoolManager,
                           ConfigManager configManager,
                           MovieDao movieDao,
                           ServerConfigDao serverConfigDao,
                           MovieHistoryDao movieHistoryDao,
                           IptvDao iptvDao) {
        this.movieDao = movieDao;
        this.serverConfigDao = serverConfigDao;
        this.movieHistoryDao = movieHistoryDao;
        this.iptvDao = iptvDao;

        this.diskExecutor = threadPoolManager.getDiskExecutor();
        this.mainThreadHandler = new Handler(Looper.getMainLooper());

        // Get configuration from injected ConfigManager
        this.cacheEnabled = configManager.getBoolean("feature.enable_cache", true);
        this.maxCacheEntries = configManager.getInt("db.cache_size_entries", 500);
        this.cacheTtlMs = configManager.getInt("db.cache_ttl_ms", 300000); // 5 minutes default

        Logger.i(TAG, "DatabaseManager initialized with Hilt, cache " +
                (cacheEnabled ? "enabled (" + maxCacheEntries + " entries)" : "disabled"));
    }

    // New methods using DAOs (getInstance removed)

    public void getAllServerConfigs(java.util.function.Consumer<List<ServerConfig>> onSuccess, java.util.function.Consumer<Exception> onError) {
        diskExecutor.execute(() -> {
            try {
                List<ServerConfig> configs = serverConfigDao.getAllServerConfigs();
                if (onSuccess != null) {
                    mainThreadHandler.post(() -> onSuccess.accept(configs));
                }
            } catch (Exception e) {
                Logger.e(TAG, "Error getting all server configs", e);
                if (onError != null) {
                    mainThreadHandler.post(() -> onError.accept(e));
                }
            }
        });
    }

    public void saveMovie(Movie movie, java.util.function.Consumer<Void> onSuccess, java.util.function.Consumer<Exception> onError) {
        diskExecutor.execute(() -> {
            try {
                movieDao.insert(movie); // Assuming insert handles conflicts via @Insert strategy
                if (onSuccess != null) {
                    mainThreadHandler.post(() -> onSuccess.accept(null));
                }
                if (cacheEnabled) { // Example of cache invalidation
                    clearCache(); // Or more specific cache clearing for movies
                }
            } catch (Exception e) {
                Logger.e(TAG, "Error saving movie", e);
                if (onError != null) {
                    mainThreadHandler.post(() -> onError.accept(e));
                }
            }
        });
    }

    @Nullable
    public Movie getMovieByVideoUrlSync(String videoUrl) {
        String cacheKey = "movie_" + videoUrl;
        if (cacheEnabled) {
            CacheEntry entry = cache.get(cacheKey);
            if (entry != null && !entry.isExpired()) {
                Logger.d(TAG, "Cache hit for key: " + cacheKey);
                return (Movie) entry.getValue();
            }
        }

        try {
            // This assumes getMovieByVideoUrlSync is called from a background thread
            // or Room is configured with allowMainThreadQueries (which we are avoiding for now).
            Movie movie = movieDao.getMovieByVideoUrl(videoUrl);
            if (movie != null && cacheEnabled) {
                cache.put(cacheKey, new CacheEntry(movie, System.currentTimeMillis() + cacheTtlMs));
            }
            return movie;
        } catch (Exception e) {
            Logger.e(TAG, "Error getting movie by video URL sync", e);
            return null;
        }
    }


    /**
     * Get a cached value or load it from database if not found
     * The CacheLoader.load() method will be executed on the calling thread.
     * If DB operations are involved in loader.load(), ensure this method is called from a background thread.
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
     * Reset the singleton instance (for testing purposes) - Removed as Hilt manages lifecycle
     */
    // public static synchronized void reset() { ... }

    // DatabaseOperation interface is removed.

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