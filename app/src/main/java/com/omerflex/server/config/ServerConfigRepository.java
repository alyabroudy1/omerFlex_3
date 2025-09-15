package com.omerflex.server.config;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.omerflex.dao.ServerConfigDao;
import com.omerflex.db.AppDatabase;
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.entity.dto.ServerConfigDTO;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.IptvServer;
import com.omerflex.server.ServerFactory;
import com.omerflex.service.UpdateService;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerConfigRepository {

    private static final String TAG = "ServerConfigRepository";
    private static final String REMOTE_CONFIG_URL = "https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/main/app/src/main/java/com/omerflex/server/servers.json";
    private static ServerConfigRepository instance;
    private final ServerConfigDao serverConfigDao;
    private final ConcurrentHashMap<String, ServerConfig> cache = new ConcurrentHashMap<>();
    private final MutableLiveData<Boolean> isInitialized = new MutableLiveData<>(false);
    private final Context appContext;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    // A more robust date format that handles ISO 8601 with 'Z' for UTC.
    private static final SimpleDateFormat remoteDateFormatWithZ;
    static {
        remoteDateFormatWithZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        remoteDateFormatWithZ.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    // Fallback for the old format without timezone.
    private static final SimpleDateFormat remoteDateFormatLegacy = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);


    private ServerConfigRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        this.serverConfigDao = db.serverConfigDao();
        this.appContext = context.getApplicationContext();
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new ServerConfigRepository(context);
        }
    }

    public static synchronized ServerConfigRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ServerConfigRepository must be initialized in the Application class");
        }
        return instance;
    }

    public LiveData<Boolean> getIsInitialized() {
        return isInitialized;
    }

    public ServerConfig getConfig(String serverName) {
        if (cache.containsKey(serverName)) {
            return cache.get(serverName);
        }
        ServerConfig config = serverConfigDao.findByName(serverName);
        if (config != null) {
            cache.put(serverName, config);
        }
        return config;
    }

    public void updateConfig(ServerConfig newConfig) {
        if (newConfig == null || newConfig.getName() == null) {
            return;
        }
        ServerConfig existingConfig = serverConfigDao.findByName(newConfig.getName());
        if (existingConfig != null) {
            newConfig.setId(existingConfig.getId());
            serverConfigDao.update(newConfig);
        } else {
            serverConfigDao.insert(newConfig);
        }
        // Corrected cache logic: always cache the latest version.
        cache.put(newConfig.getName(), newConfig);
    }

    public LiveData<List<ServerConfig>> getAllConfigs() {
        return serverConfigDao.getAll();
    }

    public List<ServerConfig> getAllConfigsList() {
        return serverConfigDao.getAllList();
    }

    public List<ServerConfig> getAllActiveConfigsList() {
        return serverConfigDao.getActiveServers();
    }

    public void initializeDbWithDefaults() {
        if (serverConfigDao.getAllList().isEmpty()) {
            DefaultServersConfig.initializeDefaultServers();
        }
        isInitialized.postValue(true);
    }

    public void checkForRemoteUpdates(UpdateService updateService) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(REMOTE_CONFIG_URL).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray serversArray = jsonObject.getAsJsonArray("servers");

                        Log.d(TAG, "Number of servers found in remote config: " + serversArray.size());

                        for (JsonElement serverElement : serversArray) {
                            try {
                                JsonObject serverObject = serverElement.getAsJsonObject();
                                ServerConfigDTO serverConfigDTO = gson.fromJson(serverObject, ServerConfigDTO.class);

                                if (serverConfigDTO == null || serverConfigDTO.name == null) {
                                    Log.w(TAG, "Skipping server with null DTO or name in object: " + serverObject);
                                    continue;
                                }

                                Log.d(TAG, "Processing remote config for: " + serverConfigDTO.name);
                                System.out.println("config for: " + serverConfigDTO);
                                if ("app".equals(serverConfigDTO.name)) {
                                    handleAppUpdate(updateService, serverConfigDTO);
                                }
                                else {
                                    updateServerConfigIfNeeded(serverConfigDTO);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to process a server element: " + serverElement.toString(), e);
                                // Continue to the next element
                            }
                        }
                    } else {
                        Log.e(TAG, "Remote config fetch failed: " + response.code() + " " + response.message());
                    }
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                Log.e(TAG, "Error fetching or parsing remote server configs", e);
            }
        });
        executor.shutdown();
    }

    private void handleIptvPlayListUpdate(ServerConfigDTO serverConfigDTO) {
        // clean old iptv list  and add new one
        Log.d(TAG, "handleIptvPlayListUpdate: deleting old iptv movies");
        AppDatabase.getDatabase(appContext).movieDao().deleteByStudio(Movie.SERVER_IPTV);
        Log.d(TAG, "handleIptvPlayListUpdate: old iptv movies deleted");

        IptvServer iptvServer = (IptvServer) ServerFactory.createServer(Movie.SERVER_IPTV);
        if (iptvServer != null) {
            Log.d(TAG, "handleIptvPlayListUpdate: loading new movies from: " + serverConfigDTO.url);
            iptvServer.loadChannelsFromUrl(serverConfigDTO.url, (category, movies) -> {
                Log.d(TAG, "handleIptvPlayListUpdate: loaded " + movies.size() + " movies");
            });
        }
    }

    private void handleAppUpdate(UpdateService updateService, ServerConfigDTO appConfigDto) {
        if (updateService != null) {
            Log.d(TAG, "Checking for app updates...");
            new Handler(Looper.getMainLooper()).post(() -> {
                updateService.checkForUpdates(appConfigDto);
            });
        }
    }

    private void updateServerConfigIfNeeded(ServerConfigDTO githubServerConfigDTO) {
        if (githubServerConfigDTO == null || githubServerConfigDTO.name == null || githubServerConfigDTO.date == null) {
            return;
        }


        Date githubDate;
        String dateStr = githubServerConfigDTO.date.trim(); // Trim the date string

        try {
            // Try parsing with the timezone format first
             githubDate = dateFormat.parse(githubServerConfigDTO.date);
        } catch (ParseException e) {
            // Fallback to the legacy format
            try {
                githubDate = remoteDateFormatLegacy.parse(dateStr);
            } catch (ParseException pe) {
                Log.e(TAG, "Error parsing date from remote config for " + githubServerConfigDTO.name + ". Date string was: '" + dateStr + "'", pe);
                return; // Skip update if date is unparseable
            }
            Log.d(TAG, "updateServerConfigIfNeeded:Error parsing ", e);
        }

        ServerConfig existingConfig = serverConfigDao.findByName(githubServerConfigDTO.name);
        if (existingConfig == null) {
            // This is a new server, add it.
            Log.d(TAG, "Server config not found locally: " + githubServerConfigDTO.name);
//            Log.d(TAG, "Adding new server: " + githubServerConfigDTO.name);
//            ServerConfig newConfig = new ServerConfig();
//            newConfig.setName(githubServerConfigDTO.name);
//            newConfig.setUrl(githubServerConfigDTO.url);
//            newConfig.setReferer(githubServerConfigDTO.referer);
//            newConfig.setLabel(githubServerConfigDTO.label);
//            newConfig.setActive(githubServerConfigDTO.isActive);
//            newConfig.setCreatedAt(githubDate); // Set date from remote
//            updateConfig(newConfig);
        } else {
            // Existing server, check if update is needed
            Date localDate = existingConfig.getCreatedAt();
//                Log.d(TAG, "updateServerConfigIfNeeded: githubServerConfigDTO: "+githubServerConfigDTO);
            if (localDate == null || githubDate.after(localDate)) {
                Log.d(TAG, "Updating existing server: " + githubServerConfigDTO.name + (localDate == null ? " (local date was null)" : ""));
                if (Movie.SERVER_IPTV.equals(githubServerConfigDTO.name)) {
                    handleIptvPlayListUpdate(githubServerConfigDTO);
                }

                existingConfig.setCreatedAt(githubDate);
                existingConfig.setUrl(githubServerConfigDTO.url);
                existingConfig.setReferer(githubServerConfigDTO.referer);
                existingConfig.setLabel(githubServerConfigDTO.label);
                existingConfig.setActive(githubServerConfigDTO.isActive);
                updateConfig(existingConfig);
            } else {
                Log.d(TAG, "Server " + githubServerConfigDTO.name + " is already up to date.");
            }
        }
    }

    public AbstractServer getServer(String serverName) {
       ServerConfig config = getConfig(serverName);
       if (config == null) {
           Log.d(TAG, "server not found: "+ serverName);
           return null;
       }
       return ServerFactory.createServer(config.getName());
    }


    // todo implement later
//    public AbstractServer getCurrentServer() {
//        Log.d(TAG, "getServer: Not implemented yet");
//        Movie selectedMovie = MovieRepository.getInstance(get);
//        if (config == null) {
//            Log.d(TAG, "server not found: "+ serverId);
//            return null;
//        }
//        return ServerFactory.createServer(config.getName());
//    }
}
