package com.omerflex.service;

import android.content.Context;
import android.util.Log;
import android.webkit.CookieManager;

import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.AkwamServer;
import com.omerflex.server.ArabSeedServer;
import com.omerflex.server.CimaNowServer;
import com.omerflex.server.FaselHdServer;
import com.omerflex.server.ImdbServer;
import com.omerflex.server.IptvServer;
import com.omerflex.server.KooraServer;
import com.omerflex.server.LarozaServer;
import com.omerflex.server.MyCimaServer;
import com.omerflex.server.OldAkwamServer;
import com.omerflex.server.OmarServer;
import com.omerflex.service.database.MovieDbHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConfigManager {

    private static final String TAG = "ServerConfigManager";
    private static Context applicationContext;
    private static final Map<String, ServerConfig> serversConfigs = new ConcurrentHashMap<>();
    private static final Map<String, AbstractServer> servers = new ConcurrentHashMap<>();

    public static void initialize(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
    }

    public static Context getContext() {
        return applicationContext;
    }

    public static ServerConfig getConfig(String serverId) {
        return serversConfigs.get(serverId);
    }

    public static boolean updateConfig(ServerConfig newConfig) {
        ServerConfig existing = serversConfigs.get(newConfig.getName());
        if (existing != null) {
            existing.updateFrom(newConfig);
            Log.d(TAG, "Config updated: " + newConfig.getName());
            return true;
        }
        Log.d(TAG, "error: fail updating Config: " + newConfig.getName());
        return false;
    }

    // Update with additional DB and cookie handling
    public static boolean updateConfig(ServerConfig newConfig, MovieDbHelper dbHelper) {
        if (!updateConfig(newConfig)) return false;

        // Cookie management
        if (newConfig.getStringCookies() != null) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);

            if (cookieManager.getCookie(newConfig.getUrl()) == null) {
                // Always update cookies if config changed
                cookieManager.setCookie(newConfig.getUrl(), newConfig.getStringCookies());
                cookieManager.flush();
            }
        }

        dbHelper.saveServerConfig(newConfig);
        return true;
    }

    public static boolean addConfig(ServerConfig newConfig) {
        if (newConfig == null) {
            Log.w(TAG, "addConfig: Attempted to add a null config.");
            return false;
        }

        String configName = newConfig.getName();
        if (configName == null || configName.trim().isEmpty()) {
            Log.w(TAG, "addConfig: Config name is null or empty.");
            return false;
        }

        // Check if the config already exists
        ServerConfig existingConfig = serversConfigs.get(configName);
        if (existingConfig != null) {
            Log.d(TAG, "addConfig: Config with name " + configName + " already exists. Updating instead.");
            return updateConfig(newConfig); // Update the existing config
        }

        // Add the new config
        serversConfigs.put(configName, newConfig);
        Log.d(TAG, "addConfig: Added new config for " + configName);

        return true;
    }

    public static boolean addConfig(ServerConfig newConfig, MovieDbHelper dbHelper) {
        boolean result = addConfig(newConfig);
        if (result) {
            dbHelper.saveServerConfig(newConfig);
        }
        AbstractServer server = createServerInstance(newConfig.getName());
        if (server != null){
            servers.put(newConfig.getName(), server);
        }
        return result;
    }

    public static Map<String, AbstractServer> getServers(MovieDbHelper dbHelper) {
        if (servers.isEmpty()) {
            initializeServersFromDB(dbHelper);
        }
        return Collections.unmodifiableMap(servers);
    }

    private static void initializeServersFromDB(MovieDbHelper dbHelper) {
        servers.clear();
        serversConfigs.clear();

        for (ServerConfig config : dbHelper.getAllServerConfigs()) {
            addConfig(config);
            AbstractServer server = createServerInstance(config.getName());
            if (server != null) {
                servers.put(server.getServerId(), server);
            }
        }

        if (servers.isEmpty()) {
            DefaultServersConfig.initializeDefaultServers(dbHelper);
        }
    }

    public static AbstractServer getServer(String serverId) {
        return servers.get(serverId);
    }

    public static void updateServer(AbstractServer server) {
        servers.put(server.getServerId(), server);
    }

    private static AbstractServer determineServer(String serverId) {
        switch (serverId) {
            case Movie.SERVER_MyCima:
                return new MyCimaServer();
            case Movie.SERVER_CimaNow:
                return new CimaNowServer();
            case Movie.SERVER_ARAB_SEED:
                return new ArabSeedServer();
            case Movie.SERVER_FASELHD:
                return new FaselHdServer();
            case Movie.SERVER_AKWAM:
                return new AkwamServer();
            case Movie.SERVER_OLD_AKWAM:
                return new OldAkwamServer();
            case Movie.SERVER_IPTV:
                return new IptvServer();
            case Movie.SERVER_OMAR:
                return new OmarServer();
            case Movie.SERVER_KOORA_LIVE:
                return new KooraServer();
            case Movie.SERVER_LAROZA:
                return new LarozaServer();
        }
        return null;
    }

    private static AbstractServer createServerInstance(String serverId) {
        switch (serverId) {
            case Movie.SERVER_MyCima:
                return new MyCimaServer();
            case Movie.SERVER_CimaNow:
                return new CimaNowServer();
            case Movie.SERVER_ARAB_SEED:
                return new ArabSeedServer();
            case Movie.SERVER_FASELHD:
                return new FaselHdServer();
            case Movie.SERVER_AKWAM:
                return new AkwamServer();
            case Movie.SERVER_OLD_AKWAM:
                return new OldAkwamServer();
            case Movie.SERVER_IPTV:
                return new IptvServer();
            case Movie.SERVER_OMAR:
                return new OmarServer();
            case Movie.SERVER_KOORA_LIVE:
                return new KooraServer();
            case Movie.SERVER_LAROZA:
                return new LarozaServer();
            case Movie.SERVER_IMDB:
                return new ImdbServer();
        }
        return null;
    }
}
