package com.omerflex.service;

import android.app.Activity;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;

import com.omerflex.entity.Movie;
import com.omerflex.server.AkwamServer;
import com.omerflex.server.ArabSeedServer;
import com.omerflex.server.CimaClubServer;
import com.omerflex.server.FaselHdController;
import com.omerflex.server.IptvServer;
import com.omerflex.server.OldAkwamServer;
import com.omerflex.server.OmarServer;
import com.omerflex.service.database.MovieDbHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.omerflex.entity.dto.CookieDTO;
import com.omerflex.entity.dto.ServerConfig;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.MyCimaServer;

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

public class ServerManager {

    private static final String TAG = "ServerManager";

    private final String remoteServersConfigUrl = "https://raw.githubusercontent.com/alyabroudy1/omerFlex-php/main/servers.json";

    Activity activity;
    Fragment fragment;
    MovieDbHelper dbHelper;
    //    private static ServerManager instance;
//
    private ArrayList<AbstractServer> servers;

    public ServerManager(Activity activity, Fragment fragment) {
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

    public ArrayList<AbstractServer> initializeServersFromDB() {
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
                config.url = serverCookie.referer;
                config.name = serverCookie.name;
                config.date = serverCookie.date.toString();
                server.setReferer(serverCookie.referer);
                server.setConfig(config);
                server.setHeaders(getMappedHeaders(serverCookie.headers));
                server.setCookies(serverCookie.cookie);
//                Log.d(TAG, "generateServers: adding server:"+server.getLabel());
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
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Log.d(TAG, "addServerConfigsToDB: ");
        Date date = null;
        try {
            date = format.parse("2024-02-22T12:30:00");
        } catch (ParseException e) {
            date = new Date();
        }

//        Log.d(TAG, "addServerConfigsToDB: date: "+date.toString());

        //        ### fasel ###
        ServerConfig faselCDTO = new ServerConfig();
        faselCDTO.name = Movie.SERVER_FASELHD;
        faselCDTO.url = "https://faselhd.center";

        AbstractServer faselhd = FaselHdController.getInstance(fragment, activity);
        faselhd.setConfig(faselCDTO);
        dbHelper.saveServerConfigAsCookieDTO(faselCDTO, date);
        servers.add(faselhd);

//        ### Mycima ###
        ServerConfig mycimaCDTO = new ServerConfig();
        mycimaCDTO.name = Movie.SERVER_MyCima;
        mycimaCDTO.url = "https://mycima.io";

        AbstractServer mycima = MyCimaServer.getInstance(activity, fragment);
        mycima.setConfig(mycimaCDTO);
        dbHelper.saveServerConfigAsCookieDTO(mycimaCDTO, date);
        servers.add(mycima);




        //### old_Akwam ###
        ServerConfig oldAkwamCDTO = new ServerConfig();
        oldAkwamCDTO.name = Movie.SERVER_OLD_AKWAM;
        oldAkwamCDTO.url = "https://ak.sv/old";

        AbstractServer oldAkwam = OldAkwamServer.getInstance(activity, fragment);
        oldAkwam.setConfig(oldAkwamCDTO);
        dbHelper.saveServerConfigAsCookieDTO(oldAkwamCDTO, date);
        servers.add(oldAkwam);


//        //### arabseed ###
        ServerConfig arabseedCDTO = new ServerConfig();
        arabseedCDTO.name = Movie.SERVER_ARAB_SEED;
        arabseedCDTO.url = "https://arabseed.show";

        AbstractServer arabseed = ArabSeedServer.getInstance(fragment, activity);
        arabseed.setConfig(arabseedCDTO);
        dbHelper.saveServerConfigAsCookieDTO(arabseedCDTO, date);
        servers.add(arabseed);



        //### cimaclub ###
        ServerConfig cimaclubCDTO = new ServerConfig();
        cimaclubCDTO.name = Movie.SERVER_CIMA_CLUB;
        cimaclubCDTO.url = "https://cimaclub.top";

        AbstractServer cimaclub = CimaClubServer.getInstance(fragment, activity);
        cimaclub.setConfig(cimaclubCDTO);
        dbHelper.saveServerConfigAsCookieDTO(cimaclubCDTO, date);
        servers.add(cimaclub);

//
////        //### watanflix ###
////        ServerConfig watanflixCDTO = new ServerConfig();
////        watanflixCDTO.name = Movie.SERVER_WATAN_FLIX;
////        watanflixCDTO.url = "https://watanflix.com";
////
////        AbstractServer watanflix = WatanFlixController.getInstance(fragment, activity);
////        watanflix.setConfig(watanflixCDTO);
////        dbHelper.saveServerConfigAsCookieDTO(watanflixCDTO, date);
////        servers.add(watanflix);
////
////
//        ### Omar ###
        ServerConfig omarCDTO = new ServerConfig();
        omarCDTO.name = Movie.SERVER_OMAR;
        omarCDTO.url = "http://194.164.53.40/movie";

        AbstractServer omar = OmarServer.getInstance(activity, fragment);
        omar.setConfig(omarCDTO);
        dbHelper.saveServerConfigAsCookieDTO(omarCDTO, date);
        servers.add(omar);

        Log.d(TAG, "addServerConfigsToDB: servers.dize: "+servers.size());


//        ### Akwam ###
        ServerConfig akwamCDTO = new ServerConfig();
        akwamCDTO.name = Movie.SERVER_AKWAM;
        akwamCDTO.url = "https://ak.sv";

        AbstractServer akwam = AkwamServer.getInstance(activity, fragment);
        akwam.setConfig(akwamCDTO);
        dbHelper.saveServerConfigAsCookieDTO(akwamCDTO, date);
        servers.add(akwam);


        //### Iptv ###
        ServerConfig iptvDTO = new ServerConfig();
        iptvDTO.name = Movie.SERVER_IPTV;
        iptvDTO.url = "https://drive.google.com/drive/folders/1lHoE-WD43FGr9kHAYoo-11HrPHgUOQMa?usp=sharing";

        AbstractServer iptv = IptvServer.getInstance(activity, fragment);
        iptv.setConfig(iptvDTO);
        dbHelper.saveServerConfigAsCookieDTO(iptvDTO, date);
        servers.add(iptv);

        return servers;
    }

    /**
     * initialize servers from remove servers.json
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
                            ServerConfig serverConfig = gson.fromJson(serverObject, ServerConfig.class);
//                            Log.d(TAG, "updateServerConfig: serverConfig:"+serverConfig);

                            updateServerConfig(serverConfig);
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

    private void updateServerConfig(ServerConfig serverConfig) {
//        Log.d(TAG, "updateServerConfig: " + serverConfig);
        if (serverConfig == null || serverConfig.name == null || serverConfig.name.equals("")) {
            return;
        }

//        Log.d(TAG, "updateServerConfig2: " + serverConfig);
        try {
            CookieDTO cookieDTO = dbHelper.getCookieDto(serverConfig.name);
            if (cookieDTO == null){
                return;
            }
//            Log.d(TAG, "updateServerConfig3:cookieDTO: " + cookieDTO);

            if (serverConfig.url != null && !serverConfig.url.equals("")) {
                if (cookieDTO.date != null) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    try {
                        Date githubDate = format.parse(serverConfig.date);
                        //if github date is newer then take it
                        boolean isNewDate = githubDate.getTime() > cookieDTO.date.getTime();
                        Log.d(TAG, "updateServerConfig:"+serverConfig.name+", g:"+ githubDate + ", db:"+cookieDTO.date+", isNewDate:"+isNewDate);
                        if (isNewDate){
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
            case Movie.SERVER_MyCima:
                return MyCimaServer.getInstance(activity, fragment);
            case Movie.SERVER_AKWAM:
                return AkwamServer.getInstance(activity, fragment);
            case Movie.SERVER_OLD_AKWAM:
                return OldAkwamServer.getInstance(activity, fragment);
            case Movie.SERVER_FASELHD:
                return FaselHdController.getInstance(fragment, activity);
//            case Movie.SERVER_CIMA4U:
//                return Cima4uController.getInstance(fragment, activity);
//            case Movie.SERVER_SHAHID4U:
//                return Shahid4uController.getInstance(fragment, activity);
////            case Movie.SERVER_SERIES_TIME:
////                return new SeriesTimeController(listRowAdapter, activity);
            case Movie.SERVER_CIMA_CLUB:
                return CimaClubServer.getInstance(fragment, activity);
            case Movie.SERVER_ARAB_SEED:
                return ArabSeedServer.getInstance(fragment, activity);
            case Movie.SERVER_IPTV:
                return IptvServer.getInstance(activity, fragment);
            case Movie.SERVER_OMAR:
                return OmarServer.getInstance(activity, fragment);
//            case Movie.SERVER_WATAN_FLIX:
//                return WatanFlixController.getInstance(fragment, activity);
//            case Movie.SERVER_KOORA_LIVE:
//                return new KooraLiveController(listRowAdapter, activity);
        }
        return null;
    }

    public static AbstractServer determineServer(Movie movie, ArrayObjectAdapter listRowAdapter, Activity activity, Fragment fragment) {
        switch (movie.getStudio()) {
            case Movie.SERVER_MyCima:
                return MyCimaServer.getInstance(activity, fragment);
            case Movie.SERVER_AKWAM:
                return AkwamServer.getInstance(activity, fragment);
            case Movie.SERVER_OLD_AKWAM:
                return OldAkwamServer.getInstance(activity, fragment);
            case Movie.SERVER_FASELHD:
                return FaselHdController.getInstance(fragment, activity);
//            case Movie.SERVER_CIMA4U:
//                return Cima4uController.getInstance(fragment, activity);
//            case Movie.SERVER_SHAHID4U:
//                return Shahid4uController.getInstance(fragment, activity);
//            case Movie.SERVER_SERIES_TIME:
//                return new SeriesTimeController(listRowAdapter, activity);
            case Movie.SERVER_CIMA_CLUB:
                return CimaClubServer.getInstance(fragment, activity);
            case Movie.SERVER_ARAB_SEED:
                return ArabSeedServer.getInstance(fragment, activity);
            case Movie.SERVER_IPTV:
                return IptvServer.getInstance(activity, fragment);
            case Movie.SERVER_OMAR:
                return OmarServer.getInstance(activity, fragment);
//            case Movie.SERVER_WATAN_FLIX:
//                return WatanFlixController.getInstance(fragment, activity);
//            case Movie.SERVER_KOORA_LIVE:
//                return new KooraLiveController(listRowAdapter, activity);
        }
        return null;
    }
}
