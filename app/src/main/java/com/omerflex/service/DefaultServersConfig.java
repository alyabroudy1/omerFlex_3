package com.omerflex.service;

import android.app.Activity;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.FaselHdServer;
import com.omerflex.service.database.MovieDbHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DefaultServersConfig {

    private static final String TAG = "DefaultServersConfig";
    public static ArrayList<AbstractServer> getDefaultServers(Activity activity, Fragment fragment, MovieDbHelper dbHelper){
        ArrayList<AbstractServer> servers = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Log.d(TAG, "addServerConfigsToDB: ");
        Date date = null;
        try {
            date = format.parse("2024-02-22T12:30:00");
        } catch (ParseException e) {
            date = new Date();
        }

//        Log.d(TAG, "addServerConfigsToDB: date: "+date.toString());


//        //      //### mycima ###
//        ServerConfig mycimaConfig = new ServerConfig();
//        mycimaConfig.setName(Movie.SERVER_MyCima);
//        mycimaConfig.setUrl("https://mycima.io");
//        mycimaConfig.setReferer("https://mycima.io/");
//        ServerConfigManager.addConfig(mycimaConfig);
//
//        AbstractServer mycima = new MyCimaServer(activity, fragment);
//        dbHelper.saveServerConfigAsCookieDTO(mycimaConfig, date);
//        servers.add(mycima);

        //        ### fasel ###
        ServerConfig faselConfig = new ServerConfig();
        faselConfig.setName(Movie.SERVER_FASELHD);
        faselConfig.setUrl("https://faselhd.center");
        faselConfig.setReferer("https://faselhd.center/");
        ServerConfigManager.addConfig(faselConfig);

        AbstractServer faselhd = new FaselHdServer(fragment, activity);
        dbHelper.saveServerConfigAsCookieDTO(faselConfig, date);
        servers.add(faselhd);

        // ### akwam ###
//        ServerConfig akwamConfig = new ServerConfig();
//        akwamConfig.setName(Movie.SERVER_AKWAM);
//        akwamConfig.setUrl("https://ak.sv");
//        akwamConfig.setReferer("https://ak.sv/");
//        ServerConfigManager.addConfig(akwamConfig);
//
//        AbstractServer akwam = AkwamServer.getInstance(activity, fragment);
//        dbHelper.saveServerConfigAsCookieDTO(akwamConfig, date);
//        servers.add(akwam);

//        //### arabseed ###
//        ServerConfig arabseedConfig = new ServerConfig();
//        arabseedConfig.setName(Movie.SERVER_ARAB_SEED);
//        arabseedConfig.setUrl("https://arabseed.show");
//        arabseedConfig.setReferer("https://arabseed.show/");
//        ServerConfigManager.addConfig(arabseedConfig);
//
//        AbstractServer arabseed = ArabSeedServer.getInstance(fragment, activity);
//        dbHelper.saveServerConfigAsCookieDTO(arabseedConfig, date);
//        servers.add(arabseed);
//
//        //### old_Akwam ###
//        ServerConfig oldAkwamConfig = new ServerConfig();
//        oldAkwamConfig.setName(Movie.SERVER_OLD_AKWAM);
//        oldAkwamConfig.setUrl("https://ak.sv/old");
//        oldAkwamConfig.setReferer("https://ak.sv/old/");
//        ServerConfigManager.addConfig(oldAkwamConfig);
//
//        AbstractServer oldAkwam = OldAkwamServer.getInstance(activity, fragment);
//        dbHelper.saveServerConfigAsCookieDTO(oldAkwamConfig, date);
//        servers.add(oldAkwam);
//
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
//        // ### omar ###
//        ServerConfig omarConfig = new ServerConfig();
//        omarConfig.setName(Movie.SERVER_OMAR);
//        omarConfig.setUrl("http://194.164.53.40/movie");
//        omarConfig.setReferer("http://194.164.53.40/");
//        ServerConfigManager.addConfig(omarConfig);
//
//        AbstractServer omar = OmarServer.getInstance(activity, fragment);
//        dbHelper.saveServerConfigAsCookieDTO(omarConfig, date);
//        servers.add(omar);
//
//        //### iptv ###
//        ServerConfig iptvConfig = new ServerConfig();
//        iptvConfig.setName(Movie.SERVER_IPTV);
//        iptvConfig.setUrl("https://drive.google.com/drive/folders/1lHoE-WD43FGr9kHAYoo-11HrPHgUOQMa?usp=sharing");
//        iptvConfig.setReferer("https://drive.google.com/");
//        ServerConfigManager.addConfig(iptvConfig);
//
//        AbstractServer iptv = IptvServer.getInstance(activity, fragment);
//        dbHelper.saveServerConfigAsCookieDTO(iptvConfig, date);
//        servers.add(iptv);

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


        Log.d(TAG, "addServerConfigsToDB: servers.size: "+servers.size());
        return servers;
    }
}
