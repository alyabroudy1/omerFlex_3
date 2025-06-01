package com.omerflex.service.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.omerflex.service.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central configuration manager for the application.
 * Manages all configurable settings with defaults and provides ability to override at runtime.
 */
public class ConfigManager {
    private static final String TAG = "ConfigManager";
    private static final String PREF_FILE_NAME = "omerflex_config";

    // Singleton instance
    private static volatile ConfigManager instance;

    // Shared preferences for persistent config
    private final SharedPreferences preferences;

    // In-memory cache of configuration values
    private final ConcurrentHashMap<String, Object> configCache = new ConcurrentHashMap<>();

    // Default configuration values
    private static final Map<String, Object> DEFAULT_CONFIG = new HashMap<String, Object>() {{
        // Network settings
        put("network.connect_timeout_ms", 15000);
        put("network.read_timeout_ms", 30000);
        put("network.write_timeout_ms", 30000);
        put("network.retry_count", 3);
        put("network.retry_delay_ms", 1000);
        put("network.cache_size_mb", 10);

        // Database settings
        put("db.max_query_timeout_ms", 5000);
        put("db.cache_size_entries", 500);

        // Media Player settings
        put("player.buffer_ms", 30000);
        put("player.seek_increment_ms", 15000);
        put("player.connection_timeout_ms", 60000);

        // Thread pool settings
        put("thread.core_pool_size", 4);
        put("thread.max_pool_size", 8);
        put("thread.keep_alive_seconds", 30);

        // Feature flags
        put("feature.enable_cache", true);
        put("feature.enable_offline", false);
        put("feature.enable_analytics", true);
    }};

    private ConfigManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        loadCachedValues();
    }

    /**
     * Get the singleton instance of ConfigManager
     *
     * @param context Application context
     * @return ConfigManager instance
     */
    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigManager(context);
        }
        return instance;
    }

    /**
     * Load saved values into the cache
     */
    private void loadCachedValues() {
        // First populate with defaults
        configCache.putAll(DEFAULT_CONFIG);

        // Then override with saved preferences
        for (Map.Entry<String, Object> entry : DEFAULT_CONFIG.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (defaultValue instanceof Integer) {
                configCache.put(key, preferences.getInt(key, (Integer) defaultValue));
            } else if (defaultValue instanceof Long) {
                configCache.put(key, preferences.getLong(key, (Long) defaultValue));
            } else if (defaultValue instanceof Float) {
                configCache.put(key, preferences.getFloat(key, (Float) defaultValue));
            } else if (defaultValue instanceof Boolean) {
                configCache.put(key, preferences.getBoolean(key, (Boolean) defaultValue));
            } else if (defaultValue instanceof String) {
                configCache.put(key, preferences.getString(key, (String) defaultValue));
            }
        }

        Logger.d(TAG, "Configuration loaded with " + configCache.size() + " values");
    }

    /**
     * Get an integer configuration value
     *
     * @param key          Configuration key
     * @param defaultValue Default value if not found
     * @return The configuration value
     */
    public int getInt(@NonNull String key, int defaultValue) {
        Object value = configCache.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return defaultValue;
    }

    /**
     * Get a boolean configuration value
     *
     * @param key          Configuration key
     * @param defaultValue Default value if not found
     * @return The configuration value
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        Object value = configCache.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Get a long configuration value
     *
     * @param key          Configuration key
     * @param defaultValue Default value if not found
     * @return The configuration value
     */
    public long getLong(@NonNull String key, long defaultValue) {
        Object value = configCache.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return defaultValue;
    }

    /**
     * Get a string configuration value
     *
     * @param key          Configuration key
     * @param defaultValue Default value if not found
     * @return The configuration value
     */
    public String getString(@NonNull String key, String defaultValue) {
        Object value = configCache.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    /**
     * Set or update a configuration value
     *
     * @param key   Configuration key
     * @param value New value
     */
    public void setValue(@NonNull String key, @NonNull Object value) {
        configCache.put(key, value);

        SharedPreferences.Editor editor = preferences.edit();
        if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        editor.apply();

        Logger.d(TAG, "Updated config: " + key + " = " + value);
    }

    /**
     * Reset a configuration value to its default
     *
     * @param key Configuration key
     */
    public void resetToDefault(@NonNull String key) {
        if (DEFAULT_CONFIG.containsKey(key)) {
            Object defaultValue = DEFAULT_CONFIG.get(key);
            setValue(key, defaultValue);
            Logger.d(TAG, "Reset config to default: " + key + " = " + defaultValue);
        } else {
            preferences.edit().remove(key).apply();
            configCache.remove(key);
            Logger.d(TAG, "Removed non-default config: " + key);
        }
    }

    /**
     * Reset all configuration values to defaults
     */
    public void resetAllToDefaults() {
        preferences.edit().clear().apply();
        configCache.clear();
        configCache.putAll(DEFAULT_CONFIG);
        Logger.d(TAG, "Reset all configs to defaults");
    }

    /**
     * Update multiple configuration values at once
     *
     * @param updates Map of updates to apply
     */
    public void bulkUpdate(@NonNull Map<String, Object> updates) {
        SharedPreferences.Editor editor = preferences.edit();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            configCache.put(key, value);

            if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
        }

        editor.apply();
        Logger.d(TAG, "Bulk updated " + updates.size() + " config values");
    }

    /**
     * Check if a configuration key exists
     *
     * @param key Configuration key
     * @return True if the key exists
     */
    public boolean hasKey(@NonNull String key) {
        return configCache.containsKey(key);
    }

    /**
     * Get all configuration values
     *
     * @return Map of all configuration values
     */
    public Map<String, Object> getAllValues() {
        return new HashMap<>(configCache);
    }
}