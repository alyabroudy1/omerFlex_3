package com.omerflex.service;

import android.util.Log;
import android.webkit.CookieManager;

import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.entity.dto.CookieDTO;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.FaselHdServer;
import com.omerflex.server.IptvServer;
import com.omerflex.server.MyCimaServer;
import com.omerflex.server.OmarServer;
import com.omerflex.server.Util;
import com.omerflex.service.database.MovieDbHelper;

import java.util.ArrayList;

public class ServerConfigManager {

    private static final ArrayList<ServerConfig> serversConfigs = new ArrayList<>();
    private static final ArrayList<AbstractServer> servers = new ArrayList<>();
    private static final String TAG = "ServerConfigManager";

    public static ServerConfig getConfig(String serverId) {
        for (ServerConfig config : serversConfigs) {
            if (config.getName().equals(serverId)) {
                return config;
            }
        }
        return null;
    }

    public static boolean updateConfig(ServerConfig newConfig) {
        for (int i = 0; i < serversConfigs.size(); i++) {
            ServerConfig config = serversConfigs.get(i);
            if (config.getName().equals(newConfig.getName())) {
                serversConfigs.set(i, newConfig); // Replace the existing config with the new config
                return true;
            }
        }

        return false;
    }

    public static boolean updateConfig(ServerConfig newConfig, MovieDbHelper dbHelper) {
        Log.d(TAG, "updateConfig: " + newConfig);
        if (updateConfig(newConfig)) {
            if (newConfig.getStringCookies() != null) {
                // Ensure the cookies are flushed and applied
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.setCookie(newConfig.getUrl(), newConfig.getStringCookies());
                cookieManager.flush();
            }
            dbHelper.saveServerConfig(newConfig);
            return true;
        }

        return false;
    }

    public static boolean addConfig(ServerConfig newConfig, MovieDbHelper dbHelper) {
        addConfig(newConfig); // Add the new config
        dbHelper.saveServerConfig(newConfig);
        return true; // Return true indicating the config was added successfully
    }

    public static boolean addConfig(ServerConfig newConfig) {
        Log.d(TAG, "addConfig: " + newConfig);
        for (ServerConfig config : serversConfigs) {
            if (config.getName().equals(newConfig.getName())) {
                return updateConfig(newConfig); // Return false if the config with the specified name already exists
            }
        }
        serversConfigs.add(newConfig); // Add the new config
        return true; // Return true indicating the config was added successfully
    }

    public static ArrayList<AbstractServer> getServers(MovieDbHelper dbHelper) {
        Log.d(TAG, "getServers: " + servers);
        if (servers.isEmpty()) {
//           return initializeServers();
            return initializeServersFromDB(dbHelper);
        }
        return servers;
    }

    private static ArrayList<AbstractServer> initializeServersFromDB(MovieDbHelper dbHelper) {
//        ArrayList<CookieDTO> serverConfigs = dbHelper.getAllCookieDto();

        ArrayList<CookieDTO> serverConfigs = new ArrayList<>();
        Log.d(TAG, "initializeServersFromDB " + serverConfigs);
        if (serverConfigs.isEmpty()) {
            return DefaultServersConfig.getDefaultServers(dbHelper);
        }
        return generateServers(serverConfigs);
    }

    public static AbstractServer getServer(String serverId) {
        for (AbstractServer server : servers) {
            if (server.getServerId().equals(serverId)) {
                return server;
            }
        }
        return null;
    }


    public static boolean addServer(AbstractServer newServer) {
        Log.d(TAG, "addConfig: " + newServer);
        for (AbstractServer server : servers) {
            if (server.getServerId().equals(newServer.getServerId())) {
                return updateServer(newServer); // Return false if the config with the specified name already exists
            }
        }
        servers.add(newServer); // Add the new config
        return true; // Return true indicating the config was added successfully
    }

    public static boolean updateServer(AbstractServer newServer) {
        for (int i = 0; i < servers.size(); i++) {
            AbstractServer server = servers.get(i);
            if (server.getServerId().equals(newServer.getServerId())) {
                servers.set(i, newServer); // Replace the existing server with the new server
                return true;
            }
        }

        return false;
    }

    private static ArrayList<AbstractServer> generateServers(ArrayList<CookieDTO> serverConfigs) {
        Log.d(TAG, "generateServers ");
        for (CookieDTO serverCookie : serverConfigs) {
            try {
                Log.d(TAG, "generateServers: server: " + serverCookie.name);
                AbstractServer server = determineServer(serverCookie.name);
                if (server == null) {
                    continue;
                }
                ServerConfig config = new ServerConfig();
                config.setUrl(serverCookie.referer);
                config.setReferer(serverCookie.referer);
                config.setName(serverCookie.name);
                config.setDate(serverCookie.date.toString());

//                config.setHeaders(getMappedHeaders(serverCookie.headers));
                Log.d(TAG, "generateServers: adding server:" + config);
                config.setHeaders(Util.convertJsonToHashMap(serverCookie.headers));
                config.setStringCookies(serverCookie.cookie);
                ServerConfigManager.addConfig(config);
                ServerConfigManager.addServer(server);
//                Log.d(TAG, "generateServers: after adding servers.size: "+servers.size());
            } catch (Exception e) {
//                e.printStackTrace();
                Log.d(TAG, "generateServers: error: " + e.getMessage());
            }
        }
        Log.d(TAG, "generateServers: servers.size: " + servers.size());
        return servers;
    }

    private static AbstractServer determineServer(String serverId) {
        switch (serverId) {
            case Movie.SERVER_MyCima:
                return new MyCimaServer();
            case Movie.SERVER_FASELHD:
                return new FaselHdServer();
            case Movie.SERVER_OMAR:
                return new OmarServer();
            case Movie.SERVER_IPTV:
                return new IptvServer();
//                return MyCimaServer.getInstance(activity, fragment);
//            case Movie.SERVER_AKWAM:
//                return AkwamServer.getInstance(activity, fragment);
//            case Movie.SERVER_OLD_AKWAM:
//                return OldAkwamServer.getInstance(activity, fragment);
//            case Movie.SERVER_CIMA4U:
//                return Cima4uController.getInstance(fragment, activity);
//            case Movie.SERVER_SHAHID4U:
//                return Shahid4uController.getInstance(fragment, activity);
////            case Movie.SERVER_SERIES_TIME:
////                return new SeriesTimeController(listRowAdapter, activity);
//            case Movie.SERVER_CIMA_CLUB:
//                return CimaClubServer.getInstance(fragment, activity);
//            case Movie.SERVER_ARAB_SEED:
//                return ArabSeedServer.getInstance(fragment, activity);

//            case Movie.SERVER_WATAN_FLIX:
//                return WatanFlixController.getInstance(fragment, activity);
//            case Movie.SERVER_KOORA_LIVE:
//                return new KooraLiveController(listRowAdapter, activity);
        }
        return null;
    }
}
