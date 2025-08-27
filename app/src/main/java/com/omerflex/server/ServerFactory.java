package com.omerflex.server;

import com.omerflex.entity.Movie;

public class ServerFactory {
    public static AbstractServer createServer(String serverName) {
        switch (serverName) {
//            case Movie.SERVER_MyCima:
//                return new MyCimaServer();
//            case "CimaNow":
//                return new CimaNowServer();
//            case "ArabSeed":
//                return new ArabSeedServer();
//            case "FaselHD":
//                return new FaselHDServer();
//            case "Laroza":
//                return new LarozaServer();
            case Movie.SERVER_AKWAM:
                return new AkwamServer();
//            case "OldAkwam":
//                return new OldAkwamServer();
//            case "Iptv":
//                return new IptvServer();
//            case "Omar":
//                return new OmarServer();
//            case "Imdb":
//                return new ImdbServer();
            default:
                throw new IllegalArgumentException("Unknown server name: " + serverName );
        }
    }
}
