package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.omerflex.entity.Movie;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.DetailsActivity;
import com.omerflex.view.ExoplayerMediaPlayer;
import com.omerflex.view.mobile.MobileMovieDetailActivity;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private static final String TAG = "Util";
    private static final Gson gson = new Gson();

    public static String extractDomain(String videoUrl, boolean withSchema, boolean endSlash) {
        String fullDomain = "";
        try {
            URL url = new URL(videoUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            if (!withSchema) {
                return host;
            }
            String endPart = "";
            if (endSlash) {
                endPart = "/";
            }
            fullDomain = protocol + "://" + host + endPart;

        } catch (Exception e) {
            Log.d(TAG, "error: extractDomain: " + e.getMessage());
        }
        Log.d(TAG, "extractDomain: " + fullDomain);
        return fullDomain;
    }

    public static String getUrlPathOnly(String url) {
        try {
            URI uri = new URI(url);
            // Get the path part of the URL without the domain
            String path = uri.getPath();
            return path;
        } catch (URISyntaxException e) {
            // Handle invalid URI syntax
            e.printStackTrace();
            return null;
        }
    }

    public static String getValidReferer(String referer) {
        String result = referer;
        if (referer != null) {
            Pattern pattern = Pattern.compile("(https?://[^/]+)");
            Matcher matcher = pattern.matcher(referer);
            if (matcher.find()) {
                result = matcher.group(1);
            }
        }
        Log.d(TAG, "getValidReferer: " + result + ", " + referer);
        return result;
    }

    public static Map<String, String> getMapCookies(String cookies) {
        Map<String, String> cookiesHash = new HashMap<>();
        if (cookies != null) {
            //split the String by a comma
            String parts[] = cookies.split(";");

            //iterate the parts and add them to a map
            for (String part : parts) {

                //split the employee data by : to get id and name
                String empdata[] = part.split("=");

                String strId = empdata[0].trim();
                String strName = empdata[1].trim();

                //add to map
                cookiesHash.put(strId, strName);
            }

        }
        return cookiesHash;
    }

    public static String generateHeadersForVideoUrl(Map<String, String> headers) {
        String headerString = "";
        if (headers != null && !headers.isEmpty()) {
            try {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
//                                if (entry.getKey().equals("User-Agent")){
//                                    continue;
//                                }
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    sb.append("&");
                }
                // Remove the last "&" character
                sb.deleteCharAt(sb.length() - 1);
                headerString = sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "onLoadResource: error building headers for the video: " + e.getMessage());
                return "";
            }

            if (!headerString.isEmpty()) {
                return "|" + headerString;
            }
        }
        return "";
    }

    public static void openDetailsIntent(Movie movie, Activity activity) {
        Intent exoIntent = generateIntent(movie, new Intent(activity, MobileMovieDetailActivity.class));
        Objects.requireNonNull(activity).startActivity(exoIntent);
    }

    public static void openBrowserIntent(Movie movie, Activity activity) {
        Intent exoIntent = generateIntent(movie, new Intent(activity, BrowserActivity.class));
        Objects.requireNonNull(activity).startActivity(exoIntent);
    }

    @NonNull
    private static Intent generateIntent(Movie movie, Intent intent) {
        intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
        intent.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
        if (movie.getSubList() != null) {
            intent.putExtra(DetailsActivity.MOVIE_SUBLIST, (Serializable) movie.getSubList());
        }
        return intent;
    }

    public static void openExoPlayer(Movie movie, Activity activity) {
        Intent exoIntent = generateIntent(movie, new Intent(activity, ExoplayerMediaPlayer.class));
        Objects.requireNonNull(activity).startActivity(exoIntent);
    }


    public static Movie recieveSelectedMovie(Activity activity) {
        Intent intent = activity.getIntent();
        Movie movie = (Movie) intent.getSerializableExtra(DetailsActivity.MOVIE);

        if (movie == null) {
            movie = new Movie();
            movie.setTitle("حدث خطأ...");
            return movie;
        }

        Movie mSelectedMovieMainMovie = (Movie) intent.getSerializableExtra(DetailsActivity.MAIN_MOVIE);

        String movieJson = intent.getStringExtra(DetailsActivity.MOVIE_SUBLIST);
//        Gson gson = new Gson();
        Type type = new TypeToken<List<Movie>>() {
        }.getType();
        List<Movie> movieSublist = gson.fromJson(movieJson, type);
//        Log.d(TAG, "onCreate: subList:" + movieSublist);


        if (movieSublist != null) {
            movie.setSubList(movieSublist);
        }
        if (movie.getSubList() == null) {
            movie.setSubList(new ArrayList<>());
        }
        movie.setMainMovie(mSelectedMovieMainMovie);
        return movie;
    }

    private static Movie fetchMovieAtStart(Movie movie, Activity activity) {
        AbstractServer server = determineServer(movie, null, activity, null);
        if (server != null) {
            return server.fetch(movie);
        }
        return movie;
    }

    public static void openExternalVideoPlayer(Movie movie, Activity activity) {
        if (movie != null && movie.getVideoUrl() != null) {
            // Uri uri = Uri.parse(res.getSubList().get(0).getVideoUrl());
//            Log.d(TAG, "onActionClicked: Resolutions " + res);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String type = "video/*"; // It works for all video application
                    Uri uri = Uri.parse(movie.getVideoUrl());
                    Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
                    in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //  in1.setPackage("org.videolan.vlc");
                    in1.setDataAndType(uri, type);

                    activity.startActivity(in1);
//                    updateItemFromActivityResult(movie);
//                    dbHelper.addMainMovieToHistory(mSelectedMovie);
                }
            });

        }
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
