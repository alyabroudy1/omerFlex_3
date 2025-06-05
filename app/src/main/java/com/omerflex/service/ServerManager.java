package com.omerflex.service;

import android.app.Activity;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.omerflex.entity.ServerConfig;
import com.omerflex.entity.dto.ServerConfigDTO;
import com.omerflex.OmerFlexApplication; // Added for Application instance
import com.omerflex.service.concurrent.ThreadPoolManager; // Added for ThreadPoolManager
import com.omerflex.service.database.MovieDbHelper;
import com.omerflex.service.network.HttpClientManager; // Added for HttpClientManager

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.Executor; // Added for ThreadPoolManager
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerManager {

    private static final String TAG = "ServerManager";

    //    private final String remoteServersConfigUrl = "https://raw.githubusercontent.com/alyabroudy1/omerFlex-php/main/servers.json";
    private final String remoteServersConfigUrl = "https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/main/app/src/main/java/com/omerflex/server/servers.json";

    Activity activity;
    Fragment fragment;
    MovieDbHelper dbHelper;
    UpdateService updateService;
    private HttpClientManager httpClientManager;
    private ThreadPoolManager threadPoolManager; // Added field
    //    private static ServerManager instance;
//

    public ServerManager(Activity activity, Fragment fragment, UpdateService updateService) {
        this.activity = activity;
        this.fragment = fragment;
        this.dbHelper = MovieDbHelper.getInstance(activity); // This should later be replaced by DatabaseManager/DAOs
        this.updateService = updateService;

        // Initialize HttpClientManager
        if (activity != null) {
            this.httpClientManager = OmerFlexApplication.getInstance().getHttpClientManager();
            this.threadPoolManager = OmerFlexApplication.getInstance().getThreadPoolManager();
            Log.d(TAG, "ServerManager: HttpClientManager & ThreadPoolManager initialized via Activity context.");
        } else if (fragment != null && fragment.getContext() != null) {
             this.httpClientManager = OmerFlexApplication.getInstance().getHttpClientManager();
             this.threadPoolManager = OmerFlexApplication.getInstance().getThreadPoolManager();
             Log.d(TAG, "ServerManager: HttpClientManager & ThreadPoolManager initialized via Fragment context.");
        } else {
            Log.w(TAG, "ServerManager: Context not available, HttpClientManager & ThreadPoolManager could not be initialized.");
        }
        // Ensure threadPoolManager is initialized, even if context was initially null,
        // if it's critical for other operations and can be fetched later.
        // However, constructor is the best place.
        if (this.threadPoolManager == null) {
             Log.e(TAG, "ThreadPoolManager is still null after constructor attempts.");
        }

//        this.servers = new ArrayList<>();
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


    /**
     * initialize servers from remote servers.json
     */
    public void updateServers() {
        Log.d(TAG, "updateServers: ");

        if (this.threadPoolManager == null) {
            Log.e(TAG, "ThreadPoolManager not initialized in ServerManager. Attempting re-init.");
            // Attempt to re-initialize if context is available (e.g. activity)
            // This is a fallback, ideally it's set in constructor.
            if (this.activity != null || (this.fragment != null && this.fragment.getContext() != null)) {
                 this.threadPoolManager = OmerFlexApplication.getInstance().getThreadPoolManager();
                 if (this.threadPoolManager == null) {
                    Log.e(TAG, "ThreadPoolManager re-init failed. Cannot update servers.");
                    return;
                 }
                 Log.i(TAG, "ThreadPoolManager re-initialized in updateServers.");
            } else {
                 Log.e(TAG, "Context not available for ThreadPoolManager re-init. Cannot update servers.");
                 return;
            }
        }

        Executor executor = this.threadPoolManager.getNetworkExecutor(); // Use ThreadPoolManager

        executor.execute(() -> { // Changed from submit to execute as Executor doesn't have submit
            if (this.httpClientManager == null) {
                Log.e(TAG, "HttpClientManager not initialized in ServerManager. Cannot update servers.");
                 // Attempt to re-initialize if context is available from activity (might be risky if activity is not valid)
                if (this.activity != null) {
                     this.httpClientManager = OmerFlexApplication.getInstance().getHttpClientManager();
                     Log.i(TAG, "updateServers: Re-attempted HttpClientManager initialization inside executor.");
                }
                if (this.httpClientManager == null) { // Still null
                    Log.e(TAG, "HttpClientManager is still null inside executor. Aborting updateServers task.");
                    return;
                }
            }
            OkHttpClient client = this.httpClientManager.getDefaultClient();
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
                            updateServerConfig(serverConfigDTO);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException during server update: " + e.getMessage(), e);
                // e.printStackTrace(); // Avoid raw printStackTrace in production code
            } catch (Exception e) { // Catch broader exceptions during parsing or logic
                 Log.e(TAG, "Exception during server update: " + e.getMessage(), e);
            }
        });
        // executor.shutdown(); // REMOVE THIS LINE - ThreadPoolManager manages its own lifecycle
    }

    private void updateServerConfig(ServerConfigDTO githubServerConfigDTO) {
//        Log.d(TAG, "updateServerConfig: " + serverConfig);
        if (githubServerConfigDTO == null || githubServerConfigDTO.name == null || githubServerConfigDTO.name.equals("")) {
            return;
        }

//        Log.d(TAG, "updateServerConfig2: " + serverConfig);
        try {
//            CookieDTO cookieDTO = dbHelper.getCookieDto(serverConfigDTO.name);


            if (githubServerConfigDTO.name.equals("app") && updateService != null) {
                Log.d(TAG, "updateServerConfig: server config app update");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateService.checkForUpdates(githubServerConfigDTO);

                    }
                });
//                updateService.checkForUpdates(serverConfig, githubServerConfigDTO);
                return;
            }

            // todo check if this the wanted behaviour to ignore server they are not in the default config
            ServerConfig serverConfig = dbHelper.getServerConfig(githubServerConfigDTO.name);
            if (serverConfig == null) {
                return;
            }
//            Log.d(TAG, "updateServerConfig3:cookieDTO: " + cookieDTO);

            if (githubServerConfigDTO.url == null || githubServerConfigDTO.url.equals("")) {
                Log.d(TAG, "updateServerConfig: server config url empty: " + githubServerConfigDTO.name);
                return;
            }

            if (serverConfig.getCreatedAt() == null) {
                return;
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                Date githubDate = format.parse(githubServerConfigDTO.date);
                //if github date is newer then take it
                boolean isNewDate = githubDate.getTime() > serverConfig.getCreatedAt().getTime();
                Log.d(TAG,
                        "updateServerConfig:" +
                                githubServerConfigDTO.name +
                                ", isNewDate:" + isNewDate +
                                ", g:" + githubDate +
                                ", db:" + serverConfig.getCreatedAt() +
                                ", gUrl: " + githubServerConfigDTO.url +
                                ", dbUrl: " + serverConfig.getReferer()
                );
                if (isNewDate) {
//                            serverConfig.setCreatedAt(serverConfigDTO.date);
                    serverConfig.setCreatedAt(githubDate);
                    serverConfig.setName(githubServerConfigDTO.name);
                    serverConfig.setUrl(githubServerConfigDTO.url);
                    serverConfig.setReferer(githubServerConfigDTO.referer);
                    serverConfig.setLabel(githubServerConfigDTO.label);
                    serverConfig.setActive(githubServerConfigDTO.isActive);
//                            dbHelper.saveServerConfigAsCookieDTO(serverConfig, githubDate);
                    dbHelper.saveServerConfig(serverConfig);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                Log.d(TAG, "initializeServerCookies: error:" + e.getMessage());
            }


        } catch (Exception exception) {
            exception.printStackTrace();
            Log.d(TAG, "updateServerConfig:error: " + exception.getMessage());
        }
    }

    private Map<String, String> getMappedHeaders(String headers) {
        Map<String, String> headersMap = new HashMap<>();
        if (headers != null && !headers.equals("")) {
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


//    public AbstractServer determineServer(String serverName) {
//        if (serverName == null) {
//            return null;
//        }
//        Log.d(TAG, "determineServer: " + serverName);
//        switch (serverName) {
//            case Movie.SERVER_MyCima:
//                return new MyCimaServer();
//            case Movie.SERVER_FASELHD:
//                return new FaselHdServer();
////                return MyCimaServer.getInstance(activity, fragment);
////            case Movie.SERVER_AKWAM:
////                return AkwamServer.getInstance(activity, fragment);
////            case Movie.SERVER_OLD_AKWAM:
////                return OldAkwamServer.getInstance(activity, fragment);
////            case Movie.SERVER_CIMA4U:
////                return Cima4uController.getInstance(fragment, activity);
////            case Movie.SERVER_SHAHID4U:
////                return Shahid4uController.getInstance(fragment, activity);
//////            case Movie.SERVER_SERIES_TIME:
//////                return new SeriesTimeController(listRowAdapter, activity);
////            case Movie.SERVER_CIMA_CLUB:
////                return CimaClubServer.getInstance(fragment, activity);
////            case Movie.SERVER_ARAB_SEED:
////                return ArabSeedServer.getInstance(fragment, activity);
////            case Movie.SERVER_IPTV:
////                return IptvServer.getInstance(activity, fragment);
////            case Movie.SERVER_OMAR:
////                return OmarServer.getInstance(activity, fragment);
////            case Movie.SERVER_WATAN_FLIX:
////                return WatanFlixController.getInstance(fragment, activity);
////            case Movie.SERVER_KOORA_LIVE:
////                return new KooraLiveController(listRowAdapter, activity);
//        }
//        return null;
//    }

//    public static AbstractServer determineServer(Movie movie, ArrayObjectAdapter listRowAdapter, Activity activity, Fragment fragment) {
//        switch (movie.getStudio()) {
//            case Movie.SERVER_MyCima:
//                return new MyCimaServer();
//            case Movie.SERVER_FASELHD:
//                return new FaselHdServer();
//                        case Movie.SERVER_OMAR:
//                return new OmarServer();
////            case Movie.SERVER_AKWAM:
////                return AkwamServer.getInstance(activity, fragment);
////            case Movie.SERVER_OLD_AKWAM:
////                return OldAkwamServer.getInstance(activity, fragment);
////            case Movie.SERVER_CIMA4U:
////                return Cima4uController.getInstance(fragment, activity);
////            case Movie.SERVER_SHAHID4U:
////                return Shahid4uController.getInstance(fragment, activity);
////            case Movie.SERVER_SERIES_TIME:
////                return new SeriesTimeController(listRowAdapter, activity);
////            case Movie.SERVER_CIMA_CLUB:
////                return CimaClubServer.getInstance(fragment, activity);
////            case Movie.SERVER_ARAB_SEED:
////                return ArabSeedServer.getInstance(fragment, activity);
////            case Movie.SERVER_IPTV:
////                return IptvServer.getInstance(activity, fragment);
////            case Movie.SERVER_WATAN_FLIX:
////                return WatanFlixController.getInstance(fragment, activity);
////            case Movie.SERVER_KOORA_LIVE:
////                return new KooraLiveController(listRowAdapter, activity);
//            //todo: very important handle unknown servers
//            default:
//                return new MyCimaServer();
//        }
//    }
}
