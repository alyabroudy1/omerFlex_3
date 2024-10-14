package com.omerflex.view;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.IptvServer;
import com.omerflex.server.OmarServer;
import com.omerflex.server.ServerInterface;
import com.omerflex.server.Util;
import com.omerflex.service.ServerConfigManager;
import com.omerflex.service.database.MovieDbHelper;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class MainViewControl extends SearchViewControl {
    public MainViewControl(Activity activity, Fragment fragment, MovieDbHelper dbHelper) {
        super(activity, fragment, dbHelper);
    }


    protected <T> void loadServerRow(AbstractServer server, String finalQuery) {
        //initialize homepage
        Log.d(TAG, "loadHomepageRows ");

//            ExecutorService executor2 = Executors.newSingleThreadExecutor();
//            executor2.submit(() -> {
        try {

            if (server instanceof IptvServer) {
                // return as it is loaded with loadOmarServerResult to manage orders
                return;
            }

            if (server instanceof OmarServer) {
                loadOmarServerResult(finalQuery, server);
                return;
            }
            T serverAdapter = generateCategory(server.getLabel(), new ArrayList<>(), true);

            ExecutorService executor2 = Executors.newSingleThreadExecutor();
            executor2.submit(() -> {
                try {
                    ArrayList<Movie> movies = server.getHomepageMovies(new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
                        @Override
                        public void onSuccess(ArrayList<Movie> result, String title) {
                            if (result.isEmpty()) {
                                Log.d(TAG, "onSuccess: empty result");
                                return;
                            }
                            updateMovieListOfMovieAdapter(result, serverAdapter);
//                                    loadMoviesRow(server, serverAdapter, result);
                            Movie sampleMovie = result.get(0);
                            if (sampleMovie != null && sampleMovie.getVideoUrl() != null) {
                                ServerConfig config = ServerConfigManager.getConfig(server.getServerId());
                                if (null != config) {
                                    updateDomain(sampleMovie.getVideoUrl(), config, dbHelper);
                                }
                            }
                        }

                        @Override
                        public void onInvalidCookie(ArrayList<Movie> result, String title) {
                            Log.d(TAG, "onInvalidCookie: " + result);
//                                    loadMoviesRow(server, serverAdapter, result);
                            updateMovieListOfMovieAdapter(result, serverAdapter);
                        }

                        @Override
                        public void onInvalidLink(ArrayList<Movie> result) {

                        }

                        @Override
                        public void onInvalidLink(String message) {

                        }
                    });
                } catch (Exception exception) {
                    Log.d(TAG, "loadHomepageRaws: error: " + server.getLabel() + ", " + exception.getMessage());
                }
            });
            executor2.shutdown();
        } catch (Exception exception) {
            Log.d(TAG, "loadHomepageRaws: error: " + exception.getMessage());
        }
    }

    protected void loadOmarServerResult(String query, AbstractServer server) {
        ExecutorService executor2 = Executors.newSingleThreadExecutor();
        executor2.submit(() -> {
            ArrayList<Movie> movies = server.getHomepageMovies(new SearchCallback());

            loadIptvServerHomepageResult();
            loadHomepageHistoryRows();
        });
        executor2.shutdown();
    }

    private void loadIptvServerHomepageResult() {
        AbstractServer server = ServerConfigManager.getServer(Movie.SERVER_IPTV);
        if (server == null){
            Log.d(TAG, "loadIptvServerHomepageResult: undefined iptv server");
            return;
        }
        ArrayList<Movie> movies = server.getHomepageMovies(new SearchCallback());
    }

    private void updateDomain(String movieLink, ServerConfig config, MovieDbHelper dbHelper) {
        String newDomain = Util.extractDomain(movieLink, true, false);
        boolean equal = config.getUrl().contains(newDomain);
        Log.d(TAG, "updateDomain: old: " + config.getUrl() + ", new: " + newDomain + ", = " + (equal));
        if (!equal) {
            config.setUrl(newDomain);
            config.setReferer(newDomain + "/");
//            ServerConfigManager.updateConfig(config);

//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//            Log.d(TAG, "addServerConfigsToDB: ");
//            Date date = null;
//            try {
//                date = format.parse("2024-02-22T12:30:00");
//            } catch (ParseException e) {
//                date = new Date();
//            }
//            dbHelper.saveServerConfigAsCookieDTO(config, date);
            ServerConfigManager.updateConfig(config, dbHelper);
        }
    }

    protected void loadHomepageHistoryRows() {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<Movie> historyMovies = dbHelper.getAllHistoryMovies(false);
                    generateCategory("المحفوظات", historyMovies, true);

                    ArrayList<Movie> iptvHistoryMovies = dbHelper.getAllHistoryMovies(true);
                    generateCategory("محفوظات القنوات", iptvHistoryMovies, true);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "loadHomepageRaws: error loading historyRows: " + e.getMessage());
        }
    }
}
