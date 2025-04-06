package com.omerflex.service;

import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.AkwamServer;
import com.omerflex.server.ArabSeedServer;
import com.omerflex.server.CimaNowServer;
import com.omerflex.server.FaselHdServer;
import com.omerflex.server.IptvServer;
import com.omerflex.server.KooraServer;
import com.omerflex.server.LarozaServer;
import com.omerflex.server.MyCimaServer;
import com.omerflex.server.OldAkwamServer;
import com.omerflex.server.OmarServer;
import com.omerflex.service.database.MovieDbHelper;

import java.util.ArrayList;

public class DefaultServersConfig {

    private static final String TAG = "DefaultServersConfig";
    public static void initializeDefaultServers(MovieDbHelper dbHelper){
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        Log.d(TAG, "addServerConfigsToDB: ");
//        Date date = null;
//        try {
//            date = format.parse("2024-02-22T12:30:00");
//        } catch (ParseException e) {
//            date = new Date();
//        }

//        Log.d(TAG, "addServerConfigsToDB: date: "+date.toString());
//        ArrayList<ServerConfig> configList = new ArrayList<>();

              //### mycima ###
        ServerConfig mycimaConfig = new ServerConfig();
        mycimaConfig.setName(Movie.SERVER_MyCima);
        mycimaConfig.setUrl("https://mycima.io");
        mycimaConfig.setReferer("https://mycima.io/");
        mycimaConfig.setActive(true);

        ServerConfigManager.addConfig(mycimaConfig, dbHelper);

        //         ### cimaNow ###
        ServerConfig cimaNowConfig = new ServerConfig();
        cimaNowConfig.setName(Movie.SERVER_CimaNow);
        cimaNowConfig.setActive(true);
        cimaNowConfig.setUrl("https://cimanow.cc");
        cimaNowConfig.setReferer("https://cimanow.cc/");

        ServerConfigManager.addConfig(cimaNowConfig, dbHelper);


        //### arabseed ###
        ServerConfig arabseedConfig = new ServerConfig();
        arabseedConfig.setName(Movie.SERVER_ARAB_SEED);
        arabseedConfig.setUrl("https://arabseed.show");
        arabseedConfig.setReferer("https://arabseed.show/");
        arabseedConfig.setActive(true);

        ServerConfigManager.addConfig(arabseedConfig, dbHelper);

        //        ### fasel ###
        ServerConfig faselConfig = new ServerConfig();
        faselConfig.setActive(true);
        faselConfig.setName(Movie.SERVER_FASELHD);
        faselConfig.setUrl("https://www.faselhds.center");
        faselConfig.setReferer("https://www.faselhds.center/");

        ServerConfigManager.addConfig(faselConfig, dbHelper);

        //         ### laroza ###
        ServerConfig larozaConfig = new ServerConfig();
        larozaConfig.setName(Movie.SERVER_LAROZA);
        larozaConfig.setActive(true);
        larozaConfig.setUrl("https://www.laroza.now");
        larozaConfig.setReferer("https://www.laroza.now/");

        ServerConfigManager.addConfig(larozaConfig, dbHelper);

//         ### akwam ###
        ServerConfig akwamConfig = new ServerConfig();
        akwamConfig.setName(Movie.SERVER_AKWAM);
        akwamConfig.setActive(true);
        akwamConfig.setUrl("https://ak.sv");
        akwamConfig.setReferer("https://ak.sv/");

        ServerConfigManager.addConfig(akwamConfig, dbHelper);

        //### old_Akwam ###
        ServerConfig oldAkwamConfig = new ServerConfig();
        oldAkwamConfig.setName(Movie.SERVER_OLD_AKWAM);
        oldAkwamConfig.setActive(true);
        oldAkwamConfig.setUrl("https://www.ak.sv/old");
        oldAkwamConfig.setReferer("https://www.ak.sv/old/");

        ServerConfigManager.addConfig(oldAkwamConfig, dbHelper);

        //### iptv ###
        ServerConfig iptvConfig = new ServerConfig();
        iptvConfig.setName(Movie.SERVER_IPTV);
        iptvConfig.setUrl("https://drive.google.com/drive/folders/1lHoE-WD43FGr9kHAYoo-11HrPHgUOQMa?usp=sharing");
        iptvConfig.setReferer("https://drive.google.com/");
        iptvConfig.setActive(true);
//        ServerConfigManager.addConfig(iptvConfig);

        ServerConfigManager.addConfig(iptvConfig, dbHelper);

        // ### omar ###
        ServerConfig omarConfig = new ServerConfig();
        omarConfig.setName(Movie.SERVER_OMAR);
        omarConfig.setActive(true);
        omarConfig.setUrl("http://194.164.53.40/movie");
        omarConfig.setReferer("http://194.164.53.40/");
//        ServerConfigManager.addConfig(omarConfig);

        ServerConfigManager.addConfig(omarConfig, dbHelper);


        // ### paradiseHill ###
        ServerConfig paradiseHillConfig = new ServerConfig();
        paradiseHillConfig.setName(Movie.SERVER_PARADISE_HILL);
        paradiseHillConfig.setActive(true);
        paradiseHillConfig.setUrl("https://en.paradisehill.cc");
        paradiseHillConfig.setReferer("https://en.paradisehill.cc/");
//        ServerConfigManager.addConfig(omarConfig);

        ServerConfigManager.addConfig(paradiseHillConfig, dbHelper);


        // ### pornHub ###
        ServerConfig pornHubConfig = new ServerConfig();
        pornHubConfig.setName(Movie.SERVER_PORN_HUB);
        pornHubConfig.setActive(true);
        pornHubConfig.setUrl("https://pornhub.com");
        pornHubConfig.setReferer("https://pornhub.com/");
//        ServerConfigManager.addConfig(omarConfig);

        ServerConfigManager.addConfig(pornHubConfig, dbHelper);

        //        ### Koora ###
//        ServerConfig kooraConfig = new ServerConfig();
//        kooraConfig.setActive(false);
//        kooraConfig.setName(Movie.SERVER_KOORA_LIVE);
//        kooraConfig.setUrl("https://www.as-goal.cc");
////        kooraConfig.setUrl("https://kooora.live-koora.live");
////        kooraConfig.setReferer("https://kooora.live-koora.live/");
//        kooraConfig.setReferer("https://www.as-goal.cc/");
////        ServerConfigManager.addConfig(kooraConfig);
//
//        ServerConfigManager.addConfig(kooraConfig, dbHelper);

        // ### app update ###
//        ServerConfig appConfig = new ServerConfig();
//        appConfig.setName(Movie.SERVER_APP);
//        appConfig.setActive(true);
//        appConfig.setUrl("https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/main/app/omerFlex.apk");
//        appConfig.setReferer("https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/main/app/");
//
//        ServerConfigManager.addConfig(appConfig);

//        //### cimaclub ###
//        ServerConfig cimaclubConfig = new ServerConfig();
//        cimaclubConfig.setName(Movie.SERVER_CIMA_CLUB);
//        cimaclubConfig.setUrl("https://cimaclub.top");
//        cimaclubConfig.setReferer("https://cimaclub.top/");
//        ServerConfigManager.addConfig(cimaclubConfig);
//
//        AbstractServer cimaclub = CimaClubServer.getInstance(fragment, activity);
//        dbHelper.saveServerConfigAsCookieDTO(cimaclubConfig, date);
//        servers.add(cimaclub);
//

//
//

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


//        Log.d(TAG, "addServerConfigsToDB: servers.size: "+ServerConfigManager.getServers().size());
//        return configList;
    }
}
