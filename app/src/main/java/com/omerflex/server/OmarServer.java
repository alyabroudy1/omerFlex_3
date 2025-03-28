package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.omerflex.entity.Movie;
import com.omerflex.entity.dto.CategoryDTO;
import com.omerflex.entity.dto.LinkDTO;
import com.omerflex.entity.dto.MovieDTO;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.DetailsActivity;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OmarServer extends AbstractServer {

    static String TAG = "Omar";
    public static final  String TYPE_SERIES = "Series";
    public static final  String TYPE_SEASON = "Season";
    public static final  String TYPE_EPISODE = "Episode";
    public static final  String TYPE_FILM = "Film";

    public static final  String LINK_STATE_FETCH = "Fetch";
    public static final  String LINK_STATE_BROWSE = "Browse";
    public static final  String LINK_STATE_VIDEO = "Video";

    public static final  String FETCH_URL = "/fetch/";
    Activity activity;

    Fragment fragment;
    private static OmarServer instance;

    private OmarServer(Activity activity, Fragment fragment) {
        // Private constructor to prevent instantiation
        this.activity = activity;
        this.fragment = fragment;
    }

    public static synchronized OmarServer getInstance(Activity activity, Fragment fragment) {
        if (instance == null) {
            instance = new OmarServer(activity, fragment);
        } else {
            if (activity != null) {
                instance.activity = activity;
            }
            if (fragment != null) {
                instance.fragment = fragment;
            }
        }
        return instance;
    }

    @Override
    public ArrayList<Movie> search(String query) {
        Log.d(TAG, "search: " + query);
        String url = query;
        if (!query.contains("http")){
            url = getSearchUrl(query);
        }
        Log.d(TAG, "search: " + url);

        ArrayList<Movie> movieList = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try {
                    Log.d(TAG, "search: isSuccessful");
                    String body = response.body().string();

                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<MovieDTO>>() {
                    }.getType();
                    ArrayList<MovieDTO> movies = gson.fromJson(body, listType);
                    Log.d(TAG, "search: movies: "+movies);
                    for (MovieDTO movieDTO : movies) {
//                        if (movieDTO.sources == null || movieDTO.sources.size() == 0) {
//                            continue;
//                        }
                        Movie movie = generateMovieObject(movieDTO);

                        movie.setMainMovie(movie);
                        movieList.add(movie);
                    }
                } catch (JsonSyntaxException e) {
//                    e.printStackTrace();
                    Log.d(TAG, "search: error parsing json:" + e.getMessage());
                }

            }
        } catch (IOException e) {
//            e.printStackTrace();
            Log.d(TAG, "search: error:" + e.getMessage());
        }
        Log.d(TAG, "search: movieList: "+movieList.toString());
        return movieList;
    }

    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return null;
    }

    @Override
    protected String getSearchUrl(String query) {
        return getConfig().getUrl() + "/search/";
    }

    @NonNull
    private Movie generateMovieObject(MovieDTO movieDTO) {
        Movie movie = new Movie();
        movie.setTitle(movieDTO.title);
        movie.setDescription(movieDTO.description);

        String image = movieDTO.cardImage;
        String bgImage = movieDTO.backgroundImage;
        LinkDTO linkDTO = movieDTO.link;
        String serverAddress = linkDTO.authority;
//        String serverAddress = movieDTO.serverUrl;
        if (!image.startsWith("http")){
            image = serverAddress + image;
            bgImage = serverAddress + bgImage;
        }

        String type = movieDTO.type;
        int movieState = Movie.GROUP_OF_GROUP_STATE;
        if (type.equals(TYPE_SEASON)) {
            movieState = Movie.GROUP_STATE;
        } else {
            movieState = Movie.ITEM_STATE;
        }

        String stateString = linkDTO.state;
        int linkState = Movie.VIDEO_STATE;
        if (stateString.equals(LINK_STATE_FETCH)) {
            linkState = Movie.RESOLUTION_STATE;
        } else {
            linkState = Movie.BROWSER_STATE;
        }



//        if (linkDTO.url.startsWith("http")){
//            movie.setVideoUrl(linkDTO.url);
//        }else {
//            movie.setVideoUrl(linkDTO.server.authority + linkDTO.url);
//        }
            movie.setVideoUrl(getConfig().getUrl() + FETCH_URL +movieDTO.id);


        //state
            movie.setState(movieState);

        movie.setCardImageUrl(image);
        movie.setBackgroundImageUrl(bgImage);
        movie.setStudio(Movie.SERVER_OMAR);
//        movie.setState(movieDTO.state);
        movie.setRate(movieDTO.rate);

        movie.setId(movieDTO.id);
        for (CategoryDTO categorydto: movieDTO.categories) {
            movie.addCategory(categorydto.name);
        }
        if (movie.getCategories().isEmpty()){
            movie.addCategory(getLabel());
        }
        return movie;
    }

    @Override
    public Movie fetch(Movie movie) {
        Log.i(TAG, "fetch: " + movie.getVideoUrl());
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.GROUP_STATE:
                return fetchGroup(movie);
            case Movie.ITEM_STATE:
                Log.d(TAG, "fetch: ITEM_STATE " + movie.getVideoUrl());
                return fetchItem(movie);
            case Movie.VIDEO_STATE:
                Log.d(TAG, "fetch: VIDEO_STATE " + movie.getVideoUrl());
                return movie;
            case Movie.BROWSER_STATE:
                Log.d(TAG, "fetch: BROWSER_STATE " + movie.getVideoUrl());
                Movie clonedMovie = Movie.clone(movie);
                clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
                return startWebForResultActivity(clonedMovie);
        }

        return fetchItem(movie);
    }

    @Override
    public Movie fetchBrowseItem(Movie movie) {
        return null;
    }

    public Map<String, String> parseParamsToMap(String params) {
        params = params.substring(params.indexOf("||") + 2);
        Map<String, String> map = new HashMap<>();
        String[] pairs;
        if (params.contains("&")) {
            pairs = params.split("&");
        } else {
            pairs = new String[]{params};
        }
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            map.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return map;
    }


    private Movie startWebForResultActivity(Movie movie) {
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        String url = movie.getVideoUrl();

        Log.d(TAG, "startWebForResultActivity: url:" + url);

        Intent browse = new Intent(activity, BrowserActivity.class);
        browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
        browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
        //   Log.d(TAG, "getResultFromWeb: activity:" + fragment.getClass().getName());
        //activity.startActivity(browse);
        fragment.startActivityForResult(browse, movie.getFetch());
        //activity.startActivity(browse);
//            }
//        });

        return null;
    }


    @Override
    public int fetchNextAction(Movie movie) {
        Log.d(TAG, "fetchNextAction: " + movie);
        switch (movie.getState()) {
            case Movie.GROUP_STATE:
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY;
            case Movie.VIDEO_STATE:
                return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
//           case Movie.RESOLUTION_STATE:
//               if (movie.getFetch() == 1 || movie.getFetch() == 0){
//                   return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
//               }
//                break;
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
    }

    @Override
    public Movie fetchToWatchLocally(Movie movie) {
        Log.d(TAG, "fetchToWatchLocally: " + movie);
        if (movie.getState() == Movie.VIDEO_STATE) {
            return movie;
        }
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);
        startWebForResultActivity(clonedMovie);
        return null;
    }

    @Override
    public Movie fetchGroupOfGroup(Movie movie) {

        return movie;
    }

    @Override
    public Movie fetchGroup(Movie movie) {
        String url = getConfig().getUrl() + "/fetch/" + movie.getId();
        // String url = movie.getVideoUrl();
        Log.d(TAG, "fetchGroup: " + url);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try {
                    String body = response.body().string();

                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<MovieDTO>>() {
                    }.getType();
                    ArrayList<MovieDTO> movies = gson.fromJson(body, listType);

                    for (MovieDTO movieDTO : movies) {
//                       if (movieDTO.subMovies == null || movieDTO.subMovies.size() == 0) {
//                            continue;
//                        }
                        Log.d(TAG, "fetchGroup: end 1:");
                        ArrayList<Movie> sublist = generateSubMovieList(movieDTO, movie);
                        Log.d(TAG, "fetchGroup: end 2:" + sublist);
                        movie.setSubList(sublist);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "search: error parsing json:" + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "fetchItem: error:" + e.getMessage());
        }
        Log.d(TAG, "fetchGroup: " + movie.getSubList());
        return movie;
    }

    @NonNull
    private ArrayList<Movie> generateSubMovieList(MovieDTO movieDTO, Movie mainMovie) {
        ArrayList<Movie> sublist = new ArrayList<>();
        Log.d(TAG, "generateSubMovieList: 1");
//                try {
//                    Movie subMovie = generateMovieObject(subMovieDTO);
//                    Log.d(TAG, "generateSubMovieList: 3: " + subMovie);
//                    subMovie.setMainMovie(mainMovie);
//                    sublist.add(subMovie);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.d(TAG, "search: error parsing json:" + e.getMessage());
//                }

        return sublist;
    }

    @NonNull
    private static Movie generateResolutionlinks(LinkDTO linkDTO, Movie mainMovie) {
        Log.d(TAG, "generateSubSourceList: 1");
        Movie sourceMovie = Movie.clone(mainMovie);
        try {
                    sourceMovie.setId(linkDTO.id);
                    sourceMovie.setTitle(linkDTO.title);


                    String stateString = linkDTO.state;
                    int linkState = Movie.VIDEO_STATE;
                    if (stateString.equals(LINK_STATE_FETCH)) {
                        linkState = Movie.RESOLUTION_STATE;
                    } else {
                        linkState = Movie.BROWSER_STATE;
                    }
                    sourceMovie.setState(linkState);

                    if (linkDTO.url.startsWith("http")) {
                        sourceMovie.setVideoUrl(linkDTO.url);
                    } else {
                        sourceMovie.setVideoUrl(linkDTO.server.authority + linkDTO.url);
                    }
                    sourceMovie.setMainMovie(mainMovie);
                } catch (Exception e) {
                    Log.d(TAG, "search: error parsing json:" + e.getMessage());
                }
        Log.d(TAG, "generateResolutionlinks: Link: "+ sourceMovie);
        return sourceMovie;
    }

    @Override
    public Movie fetchItem(Movie movie) {
        String url = getConfig().getUrl() + FETCH_URL+ movie.getId();
        // String url = movie.getVideoUrl();
        Log.d(TAG, "fetchItem: " + url);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try {
                    String body = response.body().string();

                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<LinkDTO>>() {
                    }.getType();
                    ArrayList<LinkDTO> movies = gson.fromJson(body, listType);
//                    Log.d(TAG, "fetchItem: movies: "+ movies);
                    if (movie.getSubList() == null){
                        movie.setSubList(new ArrayList<>());
                    }

                    for (LinkDTO linkDTO : movies) {
                        Log.d(TAG, "fetchGroup: end 1:");
                        Movie resoLink = generateResolutionlinks(linkDTO, movie);

                        movie.addSubList(resoLink);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "search: error parsing json:" + e.getMessage());
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "fetchItem: error:" + e.getMessage());
        }
        return movie;
    }

    @Override
    public void fetchWebResult(Movie movie) {

    }

    @Override
    public void fetchServerList(Movie movie) {

    }

    @Override
    public Movie fetchResolutions(Movie movie) {
        return movie;
    }

    @Override
    public void startVideo(String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String type = "video/*"; // It works for all video application
        link = link.replace("\"", "");
        Uri uri = Uri.parse(link);
        intent.setDataAndType(uri, type);
        activity.startActivity(intent);
        Log.i(TAG, "startVideo: " + link);
    }

    @Override
    public void startBrowser(String url) {

    }

    @Override
    public Movie fetchCookie(Movie movie) {
        return movie;
    }

    @Override
    public boolean isSeries(Movie movie) {
        return false;
    }


    @Override
    public boolean onLoadResource(Activity activity, WebView view, String url, Movie movie) {
        return false;
    }

    @Override
    public int detectMovieState(Movie movie) {
        return 0;
    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        return null;
    }

    public ArrayList<Movie> getHomepageMovies() {

        return search(getConfig().getUrl()+"/homepage");
//        return search("sonic");
    }

    @Override
    public String getLabel() {
        return "omar";
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_OMAR;
    }

    @Override
    protected Fragment getFragment() {
        return fragment;
    }

    @Override
    protected Activity getActivity() {
        return activity;
    }
}
