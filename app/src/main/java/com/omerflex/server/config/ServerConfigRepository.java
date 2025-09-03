package com.omerflex.server.config;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieRepository;
import com.omerflex.entity.ServerConfig;
import com.omerflex.entity.dto.ServerConfigDTO;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.ServerFactory;
import com.omerflex.dao.ServerConfigDao;
import com.omerflex.db.AppDatabase;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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


    private ServerConfigRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        this.serverConfigDao = db.serverConfigDao();
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
            cache.put(newConfig.getName(), newConfig);
        } else {
            serverConfigDao.insert(newConfig);
            cache.remove(newConfig.getName());
        }
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

    public void checkForRemoteUpdates() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(REMOTE_CONFIG_URL).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(response.body().string(), JsonObject.class);
                        JsonArray serversArray = jsonObject.getAsJsonArray("servers");
                        for (JsonElement serverElement : serversArray) {
                            JsonObject serverObject = serverElement.getAsJsonObject();
                            ServerConfigDTO serverConfigDTO = gson.fromJson(serverObject, ServerConfigDTO.class);
                            updateServerConfigIfNeeded(serverConfigDTO);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error fetching remote server configs", e);
            }
        });
        executor.shutdown();
    }

    private void updateServerConfigIfNeeded(ServerConfigDTO githubServerConfigDTO) {
        if (githubServerConfigDTO == null || githubServerConfigDTO.name == null) {
            return;
        }

        ServerConfig existingConfig = serverConfigDao.findByName(githubServerConfigDTO.name);
        if (existingConfig == null) {
            // This is a new server, add it.
            ServerConfig newConfig = new ServerConfig();
            newConfig.setName(githubServerConfigDTO.name);
            newConfig.setUrl(githubServerConfigDTO.url);
            newConfig.setReferer(githubServerConfigDTO.referer);
            newConfig.setLabel(githubServerConfigDTO.label);
            newConfig.setActive(githubServerConfigDTO.isActive);
            try {
                newConfig.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(githubServerConfigDTO.date));
            } catch (ParseException e) {
                newConfig.setCreatedAt(new Date());
            }
            updateConfig(newConfig);
            return;
        }

        try {
            Date githubDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(githubServerConfigDTO.date);
            if (githubDate.getTime() > existingConfig.getCreatedAt().getTime()) {
                existingConfig.setCreatedAt(githubDate);
                existingConfig.setUrl(githubServerConfigDTO.url);
                existingConfig.setReferer(githubServerConfigDTO.referer);
                existingConfig.setLabel(githubServerConfigDTO.label);
                existingConfig.setActive(githubServerConfigDTO.isActive);
                updateConfig(existingConfig);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date from remote config", e);
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