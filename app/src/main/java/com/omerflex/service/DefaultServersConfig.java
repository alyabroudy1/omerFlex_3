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
import com.omerflex.server.MyCimaServer;
import com.omerflex.server.OldAkwamServer;
import com.omerflex.server.OmarServer;
import com.omerflex.service.database.MovieDbHelper;

import java.util.ArrayList;

public class DefaultServersConfig {

    private static final String TAG = "DefaultServersConfig";
    public static ArrayList<AbstractServer> getDefaultServers(MovieDbHelper dbHelper){
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        Log.d(TAG, "addServerConfigsToDB: ");
//        Date date = null;
//        try {
//            date = format.parse("2024-02-22T12:30:00");
//        } catch (ParseException e) {
//            date = new Date();
//        }

//        Log.d(TAG, "addServerConfigsToDB: date: "+date.toString());


              //### mycima ###
        ServerConfig mycimaConfig = new ServerConfig();
        mycimaConfig.setName(Movie.SERVER_MyCima);
        mycimaConfig.setUrl("https://mycima.io");
        mycimaConfig.setReferer("https://mycima.io/");
        mycimaConfig.setActive(true);
//        ServerConfigManager.addConfig(mycimaConfig);

        AbstractServer mycima = new MyCimaServer();
//        dbHelper.saveServerConfigAsCookieDTO(mycimaConfig, date);
        ServerConfigManager.addConfig(mycimaConfig, dbHelper);
        ServerConfigManager.addServer(mycima);


        //         ### cimaNow ###
        ServerConfig cimaNowConfig = new ServerConfig();
        cimaNowConfig.setName(Movie.SERVER_CimaNow);
        cimaNowConfig.setActive(true);
        cimaNowConfig.setUrl("https://cimanow.cc");
        cimaNowConfig.setReferer("https://cimanow.cc/");

        AbstractServer cimaNowServer = new CimaNowServer();
        ServerConfigManager.addConfig(cimaNowConfig, dbHelper);
        ServerConfigManager.addServer(cimaNowServer);

        //### arabseed ###
        ServerConfig arabseedConfig = new ServerConfig();
        arabseedConfig.setName(Movie.SERVER_ARAB_SEED);
        arabseedConfig.setUrl("https://arabseed.show");
        arabseedConfig.setReferer("https://arabseed.show/");
        arabseedConfig.setActive(true);

        AbstractServer arabseedServer = new ArabSeedServer();

        ServerConfigManager.addConfig(arabseedConfig, dbHelper);
        ServerConfigManager.addServer(arabseedServer);

        //        ### fasel ###
        ServerConfig faselConfig = new ServerConfig();
        faselConfig.setActive(true);
        faselConfig.setName(Movie.SERVER_FASELHD);
        faselConfig.setUrl("https://faselhd.center");
        faselConfig.setReferer("https://faselhd.center/");
//        ServerConfigManager.addConfig(faselConfig, dbHelper);

        AbstractServer faselhd = new FaselHdServer();
//        dbHelper.saveServerConfigAsCookieDTO(faselConfig, date);
        ServerConfigManager.addConfig(faselConfig, dbHelper);
        ServerConfigManager.addServer(faselhd);

//         ### akwam ###
        ServerConfig akwamConfig = new ServerConfig();
        akwamConfig.setName(Movie.SERVER_AKWAM);
        akwamConfig.setActive(true);
        akwamConfig.setUrl("https://ak.sv");
        akwamConfig.setReferer("https://ak.sv/");

        AbstractServer akwamServer = new AkwamServer();
        ServerConfigManager.addConfig(akwamConfig, dbHelper);
        ServerConfigManager.addServer(akwamServer);


        //### old_Akwam ###
        ServerConfig oldAkwamConfig = new ServerConfig();
        oldAkwamConfig.setName(Movie.SERVER_OLD_AKWAM);
        oldAkwamConfig.setActive(true);
        oldAkwamConfig.setUrl("https://ak.sv/old");
        oldAkwamConfig.setReferer("https://ak.sv/old/");

        AbstractServer oldAkwamServer = new OldAkwamServer();
        ServerConfigManager.addConfig(oldAkwamConfig, dbHelper);
        ServerConfigManager.addServer(oldAkwamServer);

        //### iptv ###
        ServerConfig iptvConfig = new ServerConfig();
        iptvConfig.setName(Movie.SERVER_IPTV);
        iptvConfig.setUrl("https://drive.google.com/drive/folders/1lHoE-WD43FGr9kHAYoo-11HrPHgUOQMa?usp=sharing");
        iptvConfig.setReferer("https://drive.google.com/");
        iptvConfig.setActive(true);
//        ServerConfigManager.addConfig(iptvConfig);

        AbstractServer iptv = new IptvServer();
        ServerConfigManager.addConfig(iptvConfig, dbHelper);
        ServerConfigManager.addServer(iptv);

        // ### omar ###
        ServerConfig omarConfig = new ServerConfig();
        omarConfig.setName(Movie.SERVER_OMAR);
        omarConfig.setActive(true);
        omarConfig.setUrl("http://194.164.53.40/movie");
        omarConfig.setReferer("http://194.164.53.40/");
//        ServerConfigManager.addConfig(omarConfig);

        AbstractServer omarServer = new OmarServer();
        ServerConfigManager.addConfig(omarConfig, dbHelper);
        ServerConfigManager.addServer(omarServer);

        //        ### Koora ###
        ServerConfig kooraConfig = new ServerConfig();
        kooraConfig.setActive(true);
        kooraConfig.setName(Movie.SERVER_KOORA_LIVE);
        kooraConfig.setUrl("https://kooora.live-koora.live");
        kooraConfig.setReferer("https://kooora.live-koora.live/");
//        ServerConfigManager.addConfig(kooraConfig, dbHelper);

        AbstractServer koora = new KooraServer();
//        dbHelper.saveServerConfigAsCookieDTO(faselConfig, date);
        ServerConfigManager.addConfig(kooraConfig, dbHelper);
        ServerConfigManager.addServer(koora);

        // ### app update ###
//        ServerConfig appConfig = new ServerConfig();
//        appConfig.setName(Movie.SERVER_APP);
//        appConfig.setActive(true);
//        appConfig.setUrl("https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/main/app/omerFlex.apk");
//        appConfig.setReferer("https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/main/app/");
//
//        ServerConfigManager.addConfig(appConfig, dbHelper);

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
        return ServerConfigManager.getServers(dbHelper);
    }
}
