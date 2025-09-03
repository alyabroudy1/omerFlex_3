package com.omerflex.server.config;

import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;

public class DefaultServersConfig {
    public static void initializeDefaultServers(){

        ServerConfigRepository scm = ServerConfigRepository.getInstance();

        //### mycima ###
        ServerConfig mycimaConfig = new ServerConfig();
        mycimaConfig.setName(Movie.SERVER_MyCima);
        mycimaConfig.setUrl("https://mycima.io");
        mycimaConfig.setReferer("https://mycima.io/");
        mycimaConfig.setActive(true);

        scm.updateConfig(mycimaConfig);

//        //         ### cimaNow ###
//        ServerConfig cimaNowConfig = new ServerConfig();
//        cimaNowConfig.setName(Movie.SERVER_CimaNow);
//        cimaNowConfig.setActive(true);
//        cimaNowConfig.setUrl("https://cimanow.cc");
//        cimaNowConfig.setReferer("https://cimanow.cc/");
//
//        scm.updateConfig(cimaNowConfig);
//
//
//        //### arabseed ###
//        ServerConfig arabseedConfig = new ServerConfig();
//        arabseedConfig.setName(Movie.SERVER_ARAB_SEED);
//        arabseedConfig.setUrl("https://arabseed.show");
//        arabseedConfig.setReferer("https://arabseed.show/");
//        arabseedConfig.setActive(true);
//
//        scm.updateConfig(arabseedConfig);
//
//        //        ### fasel ###
//        ServerConfig faselConfig = new ServerConfig();
//        faselConfig.setActive(true);
//        faselConfig.setName(Movie.SERVER_FASELHD);
//        faselConfig.setUrl("https://www.faselhds.center");
//        faselConfig.setReferer("https://www.faselhds.center/");
//
//        scm.updateConfig(faselConfig);
//
//        //         ### laroza ###
//        ServerConfig larozaConfig = new ServerConfig();
//        larozaConfig.setName(Movie.SERVER_LAROZA);
//        larozaConfig.setActive(true);
//        larozaConfig.setUrl("https://www.laroza.now");
//        larozaConfig.setReferer("https://www.laroza.now/");
//
//        scm.updateConfig(larozaConfig);
//
//         ### akwam ###
//        ServerConfig akwamConfig = new ServerConfig();
//        akwamConfig.setName(Movie.SERVER_AKWAM);
//        akwamConfig.setActive(true);
//        akwamConfig.setUrl("https://ak.sv");
//        akwamConfig.setReferer("https://ak.sv/");
//
//        scm.updateConfig(akwamConfig);
//
//        //### old_Akwam ###
//        ServerConfig oldAkwamConfig = new ServerConfig();
//        oldAkwamConfig.setName(Movie.SERVER_OLD_AKWAM);
//        oldAkwamConfig.setActive(true);
//        oldAkwamConfig.setUrl("https://www.ak.sv/old");
//        oldAkwamConfig.setReferer("https://www.ak.sv/old/");
//
//        scm.updateConfig(oldAkwamConfig);
//
//        //### iptv ###
//        ServerConfig iptvConfig = new ServerConfig();
//        iptvConfig.setName(Movie.SERVER_IPTV);
//        iptvConfig.setUrl("https://drive.google.com/drive/folders/1lHoE-WD43FGr9kHAYoo-11HrPHgUOQMa?usp=sharing");
//        iptvConfig.setReferer("https://drive.google.com/");
//        iptvConfig.setActive(true);
//
//        scm.updateConfig(iptvConfig);
//
//        // ### omar ###
//        ServerConfig omarConfig = new ServerConfig();
//        omarConfig.setName(Movie.SERVER_OMAR);
//        omarConfig.setActive(true);
//        omarConfig.setUrl("http://194.164.53.40/movie");
//        omarConfig.setReferer("http://194.164.53.40/");
//
//        scm.updateConfig(omarConfig);
//
//
//        // ### Imdb ###
//        ServerConfig imdbConfig = new ServerConfig();
//        imdbConfig.setName(Movie.SERVER_IMDB);
//        imdbConfig.setActive(true);
//        imdbConfig.setUrl("https://api.themoviedb.org/3/");
//        imdbConfig.setReferer("https://api.themoviedb.org/3/");
//
//        scm.updateConfig(imdbConfig);

    }
}