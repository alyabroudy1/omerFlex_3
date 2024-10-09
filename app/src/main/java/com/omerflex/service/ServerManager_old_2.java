package com.omerflex.service;

import android.app.Activity;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.entity.dto.CookieDTO;
import com.omerflex.entity.dto.ServerConfigDTO;
import com.omerflex.server.AbstractServer;

import com.omerflex.service.database.MovieDbHelper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerManager_old_2 {

    private static final String TAG = "ServerManager";

    private final String remoteServersConfigUrl = "https://raw.githubusercontent.com/alyabroudy1/omerFlex-php/main/servers.json";

    Activity activity;
    Fragment fragment;
    MovieDbHelper dbHelper;
    //    private static ServerManager instance;
//
    private ArrayList<AbstractServer> servers;

    public ServerManager_old_2(Activity activity, Fragment fragment) {
        this.activity = activity;
        this.fragment = fragment;
        this.dbHelper = MovieDbHelper.getInstance(activity);
        this.servers = new ArrayList<>();
//        if (servers == null){
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            executor.submit(() -> {
//                initializeServers();
//            });
//            executor.shutdown();
//        }
    }

//    public static synchronized ServerManager getInstance(Activity activity, Fragment fragment) {
//        if (instance == null) {
//            instance = new ServerManager(activity, fragment);
//        } else {
//            if (activity != null) {
//                instance.activity = activity;
//            }
//            if (fragment != null) {
//                instance.fragment = fragment;
//            }
//        }
//        return instance;
//    }
//
//    /**
//     * in case to use a serverManger in different activity but want to have the same servers
//     * @param activity
//     * @param fragment
//     * @return
//     */
//    public static synchronized ServerManager getNewInstance(Activity activity, Fragment fragment) {
//        if (instance == null) {
//            return new ServerManager(activity, fragment);
//        } else {
//            ServerManager newInstance = new ServerManager(activity, fragment);
//            newInstance.setServers(instance.getServers());
//            return newInstance;
//        }
//    }

    private ArrayList<AbstractServer> initializeServersFromDB() {
        ArrayList<CookieDTO> serverConfigs = dbHelper.getAllCookieDto();
        if (serverConfigs.size() == 0){
            return addServerConfigsToDB();
        }
        return generateServers(serverConfigs);
    }

    private ArrayList<AbstractServer> generateServers(ArrayList<CookieDTO> serverConfigs) {
        for (CookieDTO serverCookie : serverConfigs){
            try {
//                Log.d(TAG, "generateServers: server: "+serverCookie.name);
                AbstractServer server = determineServer(serverCookie.name);
                if (server == null){
                    continue;
                }
                ServerConfig config = new ServerConfig();
                config.setUrl(serverCookie.referer);
                config.setReferer(serverCookie.referer);
                config.setName(serverCookie.name);
                config.setDate(serverCookie.date.toString());

                config.setHeaders(getMappedHeaders(serverCookie.headers));
                config.setStringCookies(serverCookie.cookie);
                ServerConfigManager.addConfig(config);
                Log.d(TAG, "generateServers: adding server:"+server.getLabel());
                this.servers.add(server);
//                Log.d(TAG, "generateServers: after adding servers.size: "+servers.size());
            }catch (Exception e){
                e.printStackTrace();
                Log.d(TAG, "generateServers: error: "+e.getMessage());
            }
        }
//        Log.d(TAG, "generateServers: servers.size: "+servers.size());
        return this.servers;
    }

    private ArrayList<AbstractServer> addServerConfigsToDB() {
        this.servers = DefaultServersConfig.getDefaultServers(dbHelper);
        return this.servers;
    }

    /**
     * initialize servers from remote servers.json
     *
     */
    public void updateServers() {
        Log.d(TAG, "updateServers: ");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(remoteServersConfigUrl)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        // Parse the JSON using Gson
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(response.body().string(), JsonObject.class);
                        JsonArray serversArray = jsonObject.getAsJsonArray("servers");
                        // Loop over every server
                        for (JsonElement serverElement : serversArray) {
                            JsonObject serverObject = serverElement.getAsJsonObject();

                            // Convert the JSON object to a ServerConfig object
                            ServerConfigDTO serverConfigDTO = gson.fromJson(serverObject, ServerConfigDTO.class);
//                            Log.d(TAG, "updateServerConfig: serverConfig:"+serverConfig);

                            updateServerConfig(serverConfigDTO);
//                                Log.d(TAG, "initializeServers: result:"+servers.size());
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }

    private void updateServerConfig(ServerConfigDTO serverConfigDTO) {
//        Log.d(TAG, "updateServerConfig: " + serverConfig);
        if (serverConfigDTO == null || serverConfigDTO.name == null || serverConfigDTO.name.equals("")) {
            return;
        }

//        Log.d(TAG, "updateServerConfig2: " + serverConfig);
        try {
            CookieDTO cookieDTO = dbHelper.getCookieDto(serverConfigDTO.name);
            if (cookieDTO == null){
                return;
            }
//            Log.d(TAG, "updateServerConfig3:cookieDTO: " + cookieDTO);

            if (serverConfigDTO.url != null && !serverConfigDTO.url.equals("")) {
                if (cookieDTO.date != null) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    try {
                        Date githubDate = format.parse(serverConfigDTO.date);
                        //if github date is newer then take it
                        boolean isNewDate = githubDate.getTime() > cookieDTO.date.getTime();
                        Log.d(TAG,
                                "updateServerConfig:"+
                                        serverConfigDTO.name+
                                        ", isNewDate:"+isNewDate+
                                ", g:"+ githubDate +
                                ", db:"+cookieDTO.date+
                                        ", gUrl: "+ serverConfigDTO.url +
                                        ", dbUrl: "+ cookieDTO.referer
                        );
                        if (isNewDate){
                            ServerConfig serverConfig = new ServerConfig();
                            serverConfig.setDate(serverConfigDTO.date);
                            serverConfig.setName(serverConfigDTO.name);
                            serverConfig.setUrl(serverConfigDTO.url);
                            serverConfig.setDisplayName(serverConfigDTO.displayName);
                            serverConfig.setWebName(serverConfigDTO.webName);
                            serverConfig.setDescription(serverConfigDTO.description);
                            serverConfig.setActive(serverConfigDTO.isActive);
                            dbHelper.saveServerConfigAsCookieDTO(serverConfig, githubDate);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.d(TAG, "initializeServerCookies: error:"+e.getMessage());
                    }
                }
            }
        }catch (Exception exception){
            exception.printStackTrace();
            Log.d(TAG, "updateServerConfig:error: "+exception.getMessage());
        }
    }

    private Map<String, String>  getMappedHeaders(String headers){
        Map<String, String>  headersMap = new HashMap<>();
        if (headers != null && !headers.equals("")){
            // Remove the curly braces from the string
            headers = headers.substring(1, headers.length() - 1);

// Split the string into key-value pairs
            String[] headerPairs = headers.split(", ");

// Create a HashMap to store the headers


// Iterate through the header pairs and populate the HashMap
            for (String pair : headerPairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    headersMap.put(key, value);
                }
            }
        }



        return headersMap;
    }

    private String getServersRemoteConfigJson() {
        //todo refactor it to client class
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(remoteServersConfigUrl)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                  return response.body().string();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<AbstractServer> getServers() {
        Log.d(TAG, "getServers: "+servers.toString());
        if (servers == null || servers.size() == 0){
//           return initializeServers();
            return initializeServersFromDB();
        }
        return servers;
    }
    public AbstractServer determineServer(String serverName) {
        if (serverName == null){
            return null;
        }
        Log.d(TAG, "determineServer: "+serverName);
        switch (serverName) {
//            case Movie.SERVER_MyCima:
//                return MyCimaServer.getInstance(activity, fragment);
//            case Movie.SERVER_AKWAM:
//                return AkwamServer.getInstance(activity, fragment);
//            case Movie.SERVER_OLD_AKWAM:
//                return OldAkwamServer.getInstance(activity, fragment);
//            case Movie.SERVER_FASELHD:
//                return FaselHdController.getInstance(fragment, activity);
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
//            case Movie.SERVER_IPTV:
//                return IptvServer.getInstance(activity, fragment);
//            case Movie.SERVER_OMAR:
//                return OmarServer.getInstance(activity, fragment);
//            case Movie.SERVER_WATAN_FLIX:
//                return WatanFlixController.getInstance(fragment, activity);
//            case Movie.SERVER_KOORA_LIVE:
//                return new KooraLiveController(listRowAdapter, activity);
        }
        return null;
    }

    public static AbstractServer determineServer(Movie movie, ArrayObjectAdapter listRowAdapter, Activity activity, Fragment fragment) {
        switch (movie.getStudio()) {
//            case Movie.SERVER_MyCima:
//                return MyCimaServer.getInstance(activity, fragment);
//            case Movie.SERVER_AKWAM:
//                return AkwamServer.getInstance(activity, fragment);
//            case Movie.SERVER_OLD_AKWAM:
//                return OldAkwamServer.getInstance(activity, fragment);
//            case Movie.SERVER_FASELHD:
//                return FaselHdController.getInstance(fragment, activity);
//            case Movie.SERVER_CIMA4U:
//                return Cima4uController.getInstance(fragment, activity);
//            case Movie.SERVER_SHAHID4U:
//                return Shahid4uController.getInstance(fragment, activity);
//            case Movie.SERVER_SERIES_TIME:
//                return new SeriesTimeController(listRowAdapter, activity);
//            case Movie.SERVER_CIMA_CLUB:
//                return CimaClubServer.getInstance(fragment, activity);
//            case Movie.SERVER_ARAB_SEED:
//                return ArabSeedServer.getInstance(fragment, activity);
//            case Movie.SERVER_IPTV:
//                return IptvServer.getInstance(activity, fragment);
//            case Movie.SERVER_OMAR:
//                return OmarServer.getInstance(activity, fragment);
//            case Movie.SERVER_WATAN_FLIX:
//                return WatanFlixController.getInstance(fragment, activity);
//            case Movie.SERVER_KOORA_LIVE:
//                return new KooraLiveController(listRowAdapter, activity);
        }
        return null;
    }
}
