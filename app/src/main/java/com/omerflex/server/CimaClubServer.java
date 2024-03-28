package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;

import com.omerflex.view.BrowserActivity;
import com.omerflex.view.DetailsActivity;
import com.omerflex.entity.Movie;
import com.omerflex.R;
import com.omerflex.view.VideoDetailsFragment;
import com.omerflex.service.database.MovieDbHelper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.omerflex.entity.dto.ServerConfig;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * from SearchActivity or MainActivity -> if item -> resolutionsActivity
 * if series -> groupActivity -> resolutionsActivity
 * -> if security check -> web browser intent
 * -> else to video intent
 * group + item -> resolution
 */
public class CimaClubServer extends AbstractServer {

    ServerConfig config;
    ArrayObjectAdapter listRowAdapter;
    static String TAG = "CimaClub";
    static String WEB_NAME = "cima";
    public static String WEB_URL = "https://cimaclub.skin";
    Activity activity;
    static boolean START_BROWSER_CODE = false;

    MovieDbHelper dbHelper;
    private static CimaClubServer instance;
    public static int RESULT_COUNTER = 0;
    private String cookies;
    private String referer;
    Fragment fragment;
    private Map<String, String> headers;

    private CimaClubServer(Fragment fragment, Activity activity) {
        // Private constructor to prevent instantiation
        this.activity = activity;
        this.fragment = fragment;
        headers = new HashMap<>();
    }

    public static synchronized CimaClubServer getInstance(Fragment fragment, Activity activity) {
        if (instance == null) {
            instance = new CimaClubServer(fragment, activity);
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

    public CimaClubServer(ArrayObjectAdapter listRowAdapter, Activity activity) {
        this.listRowAdapter = listRowAdapter;
        this.activity = activity;
        dbHelper = MovieDbHelper.getInstance(this.activity.getApplicationContext());
    }

    /**
     * produce movie from search result if isSeries than Group_State else Item_state
     *
     * @param query name to search for
     * @return
     */
    @Override
    public ArrayList<Movie> search(String query) {
        Log.i(TAG, "search: " + query);
        String searchContext = query;
        if (!query.contains("http")) {
//            if (referer != null && !referer.isEmpty()){
//                if (referer.endsWith("/")){
//                    query = referer + "search?s=" + query;
//                }else {
//                    query = referer + "/search?s=" + query;
//                }
//            }else {
//                query = WEB_URL + "/search?s=" + query;
//            }
            if (config != null && config.url != null){
                query = config.url + "/search?s=" + query;
            }else {
                query = WEB_URL + "/search?s=" + query;
            }
        }
        final String url = query;

        // List<Movie> movieList = dbHelper.findMovieBySearchContext(Movie.SERVER_CIMA_CLUB, searchContext);
        ArrayList<Movie> movieList = new ArrayList<>();


        //     Log.d(TAG, "search: movieList: " + movieList.size());
        //   if (movieList.size() == 0) {
        Log.d(TAG, "search: source network: "+ query);
        Document doc = null;

        //#########
        try {
            Log.i(TAG, "Search url:" + query);
            doc = Jsoup.connect(query)
                    .cookies(getMapCookies())
                    .headers(headers)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .get();
            Log.d(TAG,"result stop title: "+ doc.title());

            if (!doc.title().contains("moment")) {
                Elements links = doc.getElementsByClass("content-box");
                for (Element link : links) {
                    Element imageElement = link.getElementsByClass("image").first();
                    if (imageElement != null){
                        String videoUrl = imageElement.attr("href");

                        if (!videoUrl.contains("http")){
                            videoUrl = config.url + videoUrl;
                        }

                        String cardImageUrl = imageElement.attr("data-src");

                        Element titleElem = link.getElementsByTag("h3").first();
                        String title = "";
                        if (titleElem != null){
                            title = titleElem.text();
                        }

                        Element rateElem = link.getElementsByClass("rate ti-star").first();
                        String rate = "";
                        if (rateElem != null){
                            rate = rateElem.text();
                        }

                        Movie m = new Movie();
                        m.setTitle(title);
                        m.setDescription("");
                        m.setStudio(Movie.SERVER_CIMA_CLUB);
                        m.setVideoUrl(videoUrl);
                        m.setCardImageUrl(cardImageUrl);
                        m.setBackgroundImageUrl(cardImageUrl);
                        m.setState(detectMovieState(m));
                        m.setRate(rate);
                        m.setSearchContext(searchContext);
                        m.setCreatedAt(Calendar.getInstance().getTime().toString());
                        m.setMainMovie(m); //important for saving the movie

                        Log.d(TAG, "search: "+ m);
                        movieList.add(m);
                    }
                    //m.save(dbHelper);
                }
            } else {
                //**** default
                // String title = "ابحث في موقع فاصل ..";
                String title = searchContext;
                //int imageResourceId = R.drawable.default_image;
                // String cardImageUrl = "android.resource://" + activity.getPackageName() + "/" + imageResourceId;
                String cardImageUrl = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
                String backgroundImageUrl = "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/bg.jpg";
                Movie m = new Movie();
                m.setTitle(title);
                m.setDescription("نتائج البحث في الاسفل...");
                m.setStudio(Movie.SERVER_CIMA_CLUB);
                m.setVideoUrl(query);
                //  m.setVideoUrl("https://www.google.com/");
                m.setState(Movie.COOKIE_STATE);
                // m.setState(Movie.RESULT_STATE);
                m.setCardImageUrl(cardImageUrl);
                m.setBackgroundImageUrl(backgroundImageUrl);
                m.setRate("");
                m.setSearchContext(searchContext);
                movieList.add(m);
            }


        } catch (IOException e) {
            Log.i(TAG, "search: error" + e.getMessage());
            if (e.getMessage().contains("resolve")) {
                try {
                    final String gUrl = "https://www.bing.com/search?q=cmaclb+%D8%B3%D9%8A%D9%85%D8%A7+%D9%83%D9%84%D9%88%D8%A8+release-year%2F2021";
                    Log.i(TAG, "Search: google: url:" + gUrl);
                    Document doc2 = Jsoup.connect(gUrl)
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                            .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36 Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:106.0) Gecko/20100101 Firefox/106.0")
                            .followRedirects(true)
                            .ignoreHttpErrors(true)
                            .get();
                    Elements links = doc2.getElementsByTag("a");
                    String linkUrl = "";
                    Log.i(TAG, "Search: google: size:" + links.size());
                    boolean hostFound = false;
                    for (Element link : links) {
                        linkUrl = link.attr("href");
                        String title = link.text();
                        Log.d(TAG, "Search: google:" + title + ", " + linkUrl);
                        if (linkUrl.contains("release-year/2021")) {
                            String regex = "((?:https://)?[a-zA-Z0-9.-]+).*";
                            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                            Matcher matcher = pattern.matcher(linkUrl);
                            while (matcher.find()) {
                                String referer = getValidReferer(matcher.group(1));
                                setReferer(referer);
                                query = referer + "/search?s=" + searchContext;
                                Log.d(TAG, "search: host found:" + referer);
                                hostFound = true;
                                break;
                            }
                            if (hostFound) {
                                break;
                            }
                        }
                    }
//            for (Element link : links) {
//                String title = link.getElementsByAttribute("span").text();
//                linkUrl = link.parent().attr("href");
//                Log.d(TAG, "Search: google: "+title+", url:"+linkUrl);
//                if (linkUrl.contains("yallashoote")){
//                    break;
//                }
//            }

                } catch (IOException e2) {
                    Log.i(TAG, "error" + e2.getMessage());
                }
            }
        }
     /*   } else {
            Log.d(TAG, "search: source db");
        }

      */
//        if (movieList.size() == 0) {
//            Movie m = new Movie();
//            // m.setTitle("ابحث في موقع سيماكلوب ..");
//            m.setTitle(searchContext);
//            m.setDescription("نتائج البحث في الاسفل...");
//            m.setStudio(Movie.SERVER_CIMA_CLUB);
//            m.setVideoUrl(url);
//            m.setState(Movie.COOKIE_STATE);
//            //m.setState(Movie.ITEM_STATE);
//            //int imageResourceId = R.drawable.default_image;
//            // String cardImageUrl = "android.resource://" + activity.getPackageName() + "/" + imageResourceId;
//            String cardImageUrl = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
//            String backgroundImageUrl = "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/bg.jpg";
//            m.setCardImageUrl(cardImageUrl);
//            m.setBackgroundImageUrl(backgroundImageUrl);
//            m.setRate("");
//            m.setSearchContext(searchContext);
//            m.setCreatedAt(Calendar.getInstance().getTime().toString());
//
//            movieList.add(m);
//        }
//        Log.d(TAG, "search: result: "+movieList.size());
        return movieList;
    }

    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return null;
    }

    @Override
    protected String getSearchUrl(String query) {
        return null;
    }

    public Map<String, String> getMapCookies() {
        Map<String, String> cookiesHash = new HashMap<>();
        if (cookies != null && !cookies.equals("")) {
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

    @Override
    public int detectMovieState(Movie movie) {
        String url = movie.getVideoUrl();
        if (url.contains("serie/")) {
            return Movie.GROUP_OF_GROUP_STATE;
        } else if (url.contains("season/")) {
            return Movie.GROUP_STATE;
        } else {
            return Movie.ITEM_STATE;
        }
    }

    @Override
    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    @Override
    public String getCookies() {
        return cookies;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    private String getValidReferer(String referer) {
            Pattern pattern = Pattern.compile("(https?://[^/]+)");
            Matcher matcher = pattern.matcher(referer);
            if (matcher.find()){
                referer = matcher.group(1);
            }
        return referer;
    }

    @Override
    public boolean onLoadResource(Activity activity, WebView view, String url, Movie movie) {
        String newUrl = movie.getVideoUrl();
        String serverId = "#";
        if (movie.getVideoUrl().contains("?")) {
            newUrl = movie.getVideoUrl().substring(0, movie.getVideoUrl().indexOf("?"));
            serverId = movie.getVideoUrl().substring(movie.getVideoUrl().indexOf("?") + 1);
            Log.d(TAG, "startBrowser: serverID:" + serverId);
        }
        String jsClickServerLink = "var intendedValue = \"" + serverId + "\";\n" +
                "var serversTab = document.getElementsByClassName('servers-tabs')[0];" +
                "var links = serversTab.getElementsByTagName('li');" +
                "  var result = 0;\n" +
                "for (var i = 0; i < links.length; i++) {\n" +
                "  var link = links[i];\n" +
                "  if (link.getAttribute(\"data-embedd\") == intendedValue) {\n" +
                "    link.click();\n" +
                "    result= 1;\n" +
                "  }\n" +
                "}\n" +
                "result;";
        if (CimaClubServer.RESULT_COUNTER < 20) {
            view.evaluateJavascript(jsClickServerLink, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    // Log.d(TAG, "onReceiveValue:tempValue1: " + value.length() + ", " + value);
                    // Log.d(TAG, "onReceiveValue:counter: " + Cima4uController.RESULT_COUNTER);
                    if (value.equals("1")) { // means server Clicked
                        CimaClubServer.RESULT_COUNTER++;
                    }
                }
            });
        }

        return true;
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public Movie fetch(Movie movie) {
        Log.i(TAG, "fetch: " + movie.getVideoUrl());
        Intent intent = null;
        switch (movie.getState()) {
            case Movie.GROUP_STATE:
                Log.i(TAG, "onItemClick. GROUP_STATE" + movie.getStudio() + ". url:" + movie.getVideoUrl());
                //movie.setVideoUrl(movie.getVideoUrl()+"/episodes");
                return fetchGroup(movie);
            case Movie.ITEM_STATE:
//                movie.setVideoUrl(movie.getVideoUrl().replace("episode", "watch")
//                                .replace("film", "watch")
//                        // +"#video-player"
//                );
                Log.i(TAG, "onItemClick. ITEM_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return fetchItem(movie);
            case Movie.RESOLUTION_STATE:
                Log.i(TAG, "onItemClick. RESOLUTION_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                Movie clonedMovie = Movie.clone(movie);
                clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
                return fetchResolutions(clonedMovie); // to do nothing and wait till result returned to activity only the first fetch
            case Movie.VIDEO_STATE:
                Log.i(TAG, "onItemClick. video " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                startVideo(movie.getVideoUrl());
                break;
            case Movie.BROWSER_STATE:
                Log.i(TAG, "onItemClick. BROWSER_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return startWebForResultActivity(movie);
            case Movie.GROUP_OF_GROUP_STATE:
                //movie.setVideoUrl(movie.getVideoUrl() + "/seasons");
                Log.i(TAG, "onItemClick. GROUP_OF_GROUP_STATE" + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return fetchGroupOfGroup(movie);
            case Movie.RESULT_STATE:
                Log.i(TAG, "onItemClick. RESULT_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                startWebForResultActivity(movie);
                activity.finish();
                return movie;
            default:
                fetchResolutions(movie);
        }
        return movie;
    }

    @Override
    public Movie fetchBrowseItem(Movie movie) {
        return null;
    }

    private Movie startWebForResultActivity(Movie movie) {
//        fragment.getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
                Intent browse = new Intent(fragment.getActivity(), BrowserActivity.class);
                browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
                browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
                //   Log.d(TAG, "getResultFromWeb: activity:" + fragment.getClass().getName());
                //activity.startActivity(browse);
                fragment.startActivityForResult(browse, movie.getFetch());
//            }
//        });
        return movie;
    }

    @Override
    public int fetchNextAction(Movie movie) {
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY; // to open VideoDetailsActivityreturn true;
            case Movie.VIDEO_STATE:
                return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY; // not to open any activity;
    }

    @Override
    public Movie fetchGroupOfGroup(Movie movie) {
        //movie.setVideoUrl(movie.getVideoUrl() + "/seasons");
        String url = movie.getVideoUrl() + "/seasons" ;
        Log.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());
        // movie.setSubList(dbHelper.findSubListByMainMovieLink(movie.getStudio(), movie.getVideoUrl()));
//        CimaClubController.RESULT_COUNTER = 0;
//        WebView webView = activity.findViewById(R.id.webView);
//
//        CookieManager cookieManager = CookieManager.getInstance();
//        Callback callBack = new Callback() {
//            @Override
//            public void onCallback(String value, int counter) {
//                if (counter != 0) {
//                    return;
//                }
//                Log.d(TAG, "onCallback: " + counter);
//                setCookies(cookieManager.getCookie(movie.getVideoUrl()));
//                Intent returnIntent = new Intent(activity, DetailsActivity.class);
//                movie.setFetch(0); //tell next activity not to fetch movie on start
//                Gson gson = new Gson();
//                Type movieListType = new TypeToken<List<Movie>>() {
//                }.getType();
//                List<Movie> movies = gson.fromJson(value, movieListType);
//
//                String jsonMovies = gson.toJson(movies);
//                returnIntent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//                returnIntent.putExtra(DetailsActivity.MOVIE_SUBLIST, jsonMovies);
//                activity.startActivity(returnIntent);
////TODO: document.getElementsByTagName("input")[0].click();
//                activity.finish();
//
//            }
//        };
//
//        //when url not fixed
//        String fetchGroupOfGroup =
//                "var postList = [];\n" +
//                        "var seasonSection = document.getElementsByClassName(\"holder-block\");\n" +
//                        "var postDivs = seasonSection[0].getElementsByTagName(\"a\");" +
//                        "for(var i = 0; i < postDivs.length; i++) {\n" +
//                        "    var post = {};\n" +
//                        "    var postDiv = postDivs[i];\n" +
//                        "    post.title = postDiv.getElementsByTagName(\"h3\")[0].textContent;" +
//                        "    post.videoUrl = postDiv.getAttribute(\"href\");" +
//                        "    post.cardImageUrl = \"" + movie.getCardImageUrl() + "\";" +
//                        "    post.description = \"" + movie.getDescription() + "\";" +
//                        "    post.bgImageUrl = post.cardImageUrl;" +
//                        "    post.studio = \"" + Movie.SERVER_CIMA_CLUB + "\";" +
//                        "    post.state = 1;" +
//                        "    postList.push(post);\n" +
//                        "}\n" +
//                        "postList;\n";

//        String fetchGroupOfGroup2 =
//                "var postList = [];\n" +
//                        "var postDivs = document.getElementsByClassName(\"content-box\");\n" +
//                        "for (var i = 0; i < postDivs.length; i++) {\n" +
//                        "    var post = {};\n" +
//                        "    var postDiv = postDivs[i];\n" +
//                        "    var imageDiv = postDiv.getElementsByClassName(\"image\")[0];\n" +
//                        "    var rateEle = postDiv.getElementsByClassName(\"rate ti-star\");" +
//                        "    if(rateEle.length > 0){" +
//                        "    post.rate = rateEle[0].textContent;" +
//                        "    }" +
//                        "    post.title = postDiv.getElementsByTagName(\"h3\")[0].textContent; console.log('post-title'+post.title);" +
//                        "    post.videoUrl = imageDiv.href; console.log('post-url'+post.videoUrl);" +
//                        "    post.cardImageUrl = imageDiv.getAttribute('data-src'); console.log('post-image'+post.cardImageUrl);" +
//                        "    post.bgImageUrl = post.cardImageUrl;" +
//                        "    post.studio = \"" + Movie.SERVER_CIMA_CLUB + "\";" +
//                        "    post.state = 1;" +
//                        "    postList.push(post);\n" +
//                        "}\n" +
//                        "postList;\n";
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    .cookies(getMapCookies())
                    .headers(headers)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .get();

            Elements links = doc.getElementsByClass("content-box");
            for (Element link : links) {
                Element imageElement = link.getElementsByClass("image").first();
                if (imageElement != null){
                    String  videoUrl = imageElement.attr("href");
                    if (!videoUrl.contains("http")){
                        videoUrl = config.url + videoUrl;
                    }
                    String  cardImageUrl = imageElement.attr("data-src");


                    Element titleElem = link.getElementsByTag("h3").first();
                    String title = movie.getTitle();
                    if (titleElem != null){
                        title = titleElem.text();
                    }

                    Element rateElem = link.getElementsByClass("rate ti-star").first();
                    String rate = movie.getRate();
                    if (rateElem != null){
                        rate = rateElem.text();
                    }

                    Movie m = Movie.clone(movie);
                    m.setTitle(title);
                    m.setVideoUrl(videoUrl);
                    m.setCardImageUrl(cardImageUrl);
                    m.setBackgroundImageUrl(cardImageUrl);
                    m.setState(detectMovieState(m));
                    m.setRate(rate);
                    if (movie.getSubList() == null){
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(m);
                }
            }
        } catch (IOException e) {
                Log.i(TAG, "search: error" + e.getMessage());
            }
        return movie;
    }

    @Override
    public Movie fetchGroup(final Movie movie) {
        Log.i(TAG, "fetchGroup: " + movie.getVideoUrl());

//        movie.setVideoUrl(movie.getVideoUrl() + "/episodes");

        String url = movie.getVideoUrl() + "/episodes";

//        // movie.setSubList(dbHelper.findSubListByMainMovieLink(movie.getStudio(), movie.getVideoUrl()));
//        CimaClubController.RESULT_COUNTER = 0;
//        WebView webView =  activity.findViewById(R.id.webView);
//
//        CookieManager cookieManager = CookieManager.getInstance();
//        Callback callBack = new Callback() {
//            @Override
//            public void onCallback(String value, int counter) {
//                if (counter != 0){
//                    return;
//                }
//                Log.d(TAG, "onCallback: "+counter);
//                setCookies(cookieManager.getCookie(movie.getVideoUrl()));
//                Intent returnIntent = new Intent(activity, DetailsActivity.class);
//                movie.setFetch(0); //tell next activity not to fetch movie on start
//                Gson gson = new Gson();
//                Type movieListType = new TypeToken<List<Movie>>() {
//                }.getType();
//                List<Movie> movies = gson.fromJson(value, movieListType);
//
//                String jsonMovies = gson.toJson(movies);
//                returnIntent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//                returnIntent.putExtra(DetailsActivity.MOVIE_SUBLIST, jsonMovies);
//                activity.startActivity(returnIntent);
////TODO: document.getElementsByTagName("input")[0].click();
//                activity.finish();
//
//            }};
//
//        //when ur not fixed
//        String fetchGroup =
//                "var postList = [];\n" +
//                        "var seasonSection = document.getElementsByClassName(\"holder-block\");\n" +
//                        "var postDivs = seasonSection[0].getElementsByTagName(\"a\");"+
//                        "for(var i = 0; i < postDivs.length; i++) {\n" +
//                        "    var post = {};\n" +
//                        "    var postDiv = postDivs[i];\n" +
//                        "    post.title = postDiv.getElementsByTagName(\"h3\")[0].textContent;" +
//                        "    post.videoUrl = postDiv.getAttribute(\"href\");" +
//                        "    post.cardImageUrl = \""+movie.getCardImageUrl()+"\";" +
//                        "    post.description = \""+movie.getDescription()+"\";" +
//                        "    post.bgImageUrl = post.cardImageUrl;" +
//                        "    post.studio = \""+Movie.SERVER_CIMA_CLUB+"\";" +
//                        "    post.state = 2;" +
//                        "    postList.push(post);\n" +
//                        "}\n" +
//                        "postList;\n";
//
//        String fetchGroup2 =
//                "var postList = [];\n" +
//                        "var postDivs = document.getElementsByClassName(\"content-box\");\n" +
//                        "for (var i = 0; i < postDivs.length; i++) {\n" +
//                        "    var post = {};\n" +
//                        "    var postDiv = postDivs[i];\n" +
//                        "    var imageDiv = postDiv.getElementsByClassName(\"image\")[0];\n" +
//                        "    var rateEle = postDiv.getElementsByClassName(\"rate ti-star\");" +
//                        "    if(rateEle.length > 0){" +
//                        "    post.rate = rateEle[0].textContent;" +
//                        "    }"+
//                        "    post.title = postDiv.getElementsByClassName(\"episode-number\")[0].textContent;" +
//                        "    post.videoUrl = imageDiv.href;" +
//                        "    post.cardImageUrl = imageDiv.getAttribute('data-src');" +
//                        "    post.bgImageUrl = post.cardImageUrl;" +
//                        "    post.studio = \""+Movie.SERVER_CIMA_CLUB+"\";" +
//                        "    post.state = 2;" +
//                        "    postList.push(post);\n" +
//                        "}\n" +
//                        "postList;\n";
//
//
//        webView.setWebViewClient(new CustomWebViewClient(movie) {
//            @Override
//            public void onLoadResource(WebView view, String url) {
//                super.onLoadResource(view, url);
//                Log.d(TAG, "onLoadResource: fetchGroupOfGroup ");
//                if (CimaClubController.RESULT_COUNTER == 0) {
//                    webView.evaluateJavascript(fetchGroup2, new ValueCallback<String>() {
//                                @Override
//                                public void onReceiveValue(String value) {
//                                    Log.d(TAG, "onReceiveValue:tempValue1: c: "+ CimaClubController.RESULT_COUNTER+", "+ value.length() + ", " + value);
//                                    if (value.length() > 4) {
//                                        view.stopLoading();
//                                        webView.stopLoading();
//                                        callBack.onCallback(value, CimaClubController.RESULT_COUNTER++);
//                                    }
//                                }
//                            }
//                    );
//                }
//            }
//        });
//
//        webView.loadUrl(movie.getVideoUrl());

//        final String url = movie.getVideoUrl();
//        movie.setSubList(dbHelper.findSubListByMainMovieLink(movie.getStudio(), movie.getVideoUrl()));
//
//        if (movie.getSubList().size() == 0) {
//            //  if (series == null) {
//            Log.d(TAG, "fetchGroup: source network");
        try {
            Log.i(TAG, "Search url:" + url);
            Document doc = Jsoup.connect(url)
                    .headers(headers)
                    .cookies(getMapCookies())
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .ignoreContentType(true)
                    .get();

            Elements episodesList = doc.getElementsByClass("content-box");
            for (Element episodeElement : episodesList) {
                Elements titleElems = episodeElement.getElementsByClass("episode-number");
                String title = "";
                if (!titleElems.isEmpty()) {
                    title = titleElems.first().text();
                }

                Element imageElem = episodeElement.getElementsByClass("image").first();
                String image = "www.google.come";
                String videoUrl = "";
                if (imageElem != null) {
                    image = imageElem.attr("data-src");
                    videoUrl = imageElem.attr("href");
                    if (!videoUrl.contains("http")){
                        videoUrl = config.url + videoUrl;
                    }
                    Element rateElem = episodeElement.getElementsByClass("rate ti-star").first();
                    String rate = "";
                    if (rateElem != null) {
                        rate = rateElem.text();
                    }
                    if (image.isEmpty()) {
                        image = movie.getCardImageUrl();
                    }

                    Movie season = Movie.clone(movie);
                    season.setTitle(title);
                    season.setVideoUrl(videoUrl);
                    season.setCardImageUrl(image);
                    season.setBgImageUrl(image);
                    season.setBackgroundImageUrl(image);
                    season.setState(Movie.ITEM_STATE);
                    season.setRate(rate);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                   // Log.d(TAG, "fetchGroup: result:" + season);
                    movie.addSubList(season);
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "error" + e.getMessage());
        }
        return movie;
    }

    @Override
    public Movie fetchItem(final Movie movie) {
        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());

//        //Movie mm = Movie.clone(movie);
//        movie.setVideoUrl(movie.getVideoUrl().replace("episode", "watch")
//                        .replace("film", "watch")
//                // +"#video-player"
//        );
        String url = movie.getVideoUrl()
                .replace("episode", "watch")
                .replace("movie", "watch")
                .replace("film", "watch");
        try {
            Log.i(TAG, "Search url:" + url);
            Document doc = Jsoup.connect(url)
                    .headers(headers)
                    .cookies(getMapCookies())
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .ignoreContentType(true)
                    .get();

//            Elements scripts = doc.getElementsByTag("script");
//            String getServerLink = "";
//            for (Element script : scripts) {
//                Pattern pattern = Pattern.compile("(https?://[^\"]+)");
//                Matcher matcher = pattern.matcher(script.html());
//
//                // Find and print all URLs in the JavaScript code
//
//                while (matcher.find()) {
//                    String url = matcher.group();
//                    if (url != null && url.contains("getserver")){
//                        getServerLink = url;
//                      //  Log.d(TAG, "fetchItem: wwww getserver link found:"+ url);
//                        break;
//                    }
//                   // Log.d(TAG, "fetchItem: xxxx: script: " + url);
//                }
//
//            }

            Elements serverTab = doc.getElementsByClass("servers-tabs");
            if (!serverTab.isEmpty()){
                Elements serverList = serverTab.first().getElementsByAttribute("data-embedd");
                for (int i = 0; i < serverList.size(); i++) {
                    Element serverElement = serverList.get(i);
                    String title = serverElement.text();
                    String serverId = serverElement.attr("data-embedd");
                    Log.d(TAG, "fetchItem: wwww:"+title + ", "+serverId);
                    Movie server = Movie.clone(movie);
                    server.setTitle(title);
//                    if (i == 0){
//                        //TODO get iframe src
//                        Element iframe = doc.selectFirst("iframe[src]:not([src=\"about:blank\"])");
//                      if (iframe != null){
//                          server.setVideoUrl(movie.getVideoUrl());
//                      }else {
//                          server.setVideoUrl(getServerLink+"|"+serverId);
//                      }
//                    }else {
//                        server.setVideoUrl(getServerLink+"|"+serverId);
//                    }
                    if (i == 0){
                        server.setVideoUrl(url);
                    }else {
                        server.setVideoUrl(url + "?"+serverId);
                    }
                    //server.setState(Movie.BROWSER_STATE);
                    server.setState(Movie.RESOLUTION_STATE);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(server);
                }
            }


        } catch (IOException e) {
            Log.i(TAG, "error" + e.getMessage());
        }

        return movie;
    }

    @Override
    public void fetchServerList(Movie movie) {
        //Log.i(TAG, "fetchServerList: " + movie.getVideoUrl());

    }

    /**
     * fetch movie resolutions and start an external video intent
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    @Override
    public Movie fetchResolutions(final Movie movie) {
        Log.i(TAG, "fetchResolutions: " + movie.getVideoUrl());
        startWebForResultActivity(movie);
        return null;
    }

    /**
     * exactly same as fetchResolutions() fetch movie resolutions and start Exoplayer video intent
     *
     * @param movie
     * @return
     */
    @Override
    public Movie fetchToWatchLocally(final Movie movie) {
        Log.i(TAG, "fetchToWatchLocally: " + movie.getVideoUrl());
        if (movie.getState() == Movie.VIDEO_STATE) {
            return movie;
        }
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);

        return fetchResolutions(clonedMovie); // to do nothing till result returned to the fragment/activity
    }

    @Override
    public void startVideo(String url) {
        Log.i(TAG, "startVideo: " + url);
        String type = "video/*"; // It works for all video application
        Uri uri = Uri.parse(url);
        Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
        in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //  in1.setPackage("org.videolan.vlc");
        in1.setDataAndType(uri, type);
        // view.stopLoading();
        activity.startActivity(in1);
        //startBrowser(url);
    }


    @Override
    public void startBrowser(String url) {
        Log.i(TAG, "startBrowser: " + url);
        CimaClubServer.RESULT_COUNTER = 0; //important
        WebView simpleWebView = activity.findViewById(R.id.webView);
        simpleWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
/*        simpleWebView.setWebViewClient(new Browser_Home() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("WEBCLIENT", "OnreDirect url:" + url);
                String newUrl = url;
                if (newUrl.length() > 25) {
                    newUrl = url.substring(0, 25);
                }
                Log.d(TAG, "shouldOverrideUrlLoading: "+!newUrl.contains(WEB_NAME));
                return !newUrl.contains(WEB_NAME);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WEBCLIENT", "onPageFinished");
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.d("WEBCLIENT", "onLoadResource: " + url);
                if (url.contains(".mp4") || url.contains("file_code=")) {
                    Log.d("yessss", url + "");
                    String type = "video/*"; // It works for all video application
                    Uri uri = Uri.parse(url);
                    Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
                    in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //  in1.setPackage("org.videolan.vlc");
                    in1.setDataAndType(uri, type);
                    // view.stopLoading();
                    activity.startActivity(in1);
                } else {
                    super.onLoadResource(view, url);
                }
            }
        });
        simpleWebView.setWebChromeClient(new ChromeClient());
        WebSettings webSettings = simpleWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);*/

        simpleWebView.loadUrl(url);
    }

    @Override
    public Movie fetchCookie(Movie movie) {
        return movie;
    }

    public void browseTrailer(String url) {
        Log.i(TAG, "browseTrailer: " + url);
        String newUrl = url;
        if (url.contains("v=")) {
            newUrl = "https://www.youtube.com/embed/" +
                    url.substring(url.indexOf("v=") + 2)
                    + "?autoplay=1";
            Log.d(TAG, "browseTrailer: newUrl=" + newUrl);
        }

        WebView simpleWebView = activity.findViewById(R.id.webView);

        simpleWebView.clearCache(true);
        simpleWebView.clearFormData();
        simpleWebView.clearHistory();


        simpleWebView.setWebViewClient(new Browser_Home() {
            // !url.contains("youtube") || !url.contains(WEBSITE_NAME);

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WEBCLIENT", "onPageFinished");
            }

            @Override
            public void onLoadResource(final WebView view, String url) {
                Log.d("WEBCLIENT", "onLoadResource :url" + url);
                super.onLoadResource(view, url);
            }
        });
        simpleWebView.setWebChromeClient(new ChromeClient());
        WebSettings webSettings = simpleWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        //webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);

        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setMediaPlaybackRequiresUserGesture(false);


        simpleWebView.loadUrl(newUrl);
    }

    @Override
    public boolean isSeries(Movie movie) {
        String u = movie.getVideoUrl();
        return u.contains("/series") || u.contains("/movies");
    }

    @Override
    public void fetchWebResult(Movie movie) {
        WebView webView = activity.findViewById(R.id.webView);

        CimaClubServer.RESULT_COUNTER = 0;

        String extractMovies =
                "var postList = [];\n" +
                        "var postDivs = document.getElementsByClassName(\"content-box\");\n" +
                        "for (var i = 0; i < postDivs.length; i++) {\n" +
                        "    var post = {};\n" +
                        "    var postDiv = postDivs[i];\n" +
                        "    var imageDiv = postDiv.getElementsByClassName(\"image\")[0];\n" +
                        "    var rateEle = postDiv.getElementsByClassName(\"rate ti-star\");" +
                        "    if(rateEle.length > 0){" +
                        "    post.rate = rateEle[0].textContent;" +
                        "    }" +
                        "    post.title = postDiv.getElementsByTagName(\"h3\")[0].textContent; console.log('post-title'+post.title);" +
                        "    post.videoUrl = imageDiv.href; console.log('post-url'+post.videoUrl);" +
                        "    post.cardImageUrl = imageDiv.getAttribute('data-src'); console.log('post-image'+post.cardImageUrl);" +
                        "    post.bgImageUrl = post.cardImageUrl;" +
                        "    post.studio = \"" + Movie.SERVER_CIMA_CLUB + "\";" +
                        "    post.state = 0;" +
                        "    postList.push(post);\n" +
                        "} console.log('input:'+document.getElementsByTagName(\"input\")[0]);" +
                        "postList;\n";

        webView.setWebViewClient(new CustomWebViewClient(movie) {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                CookieManager cookieManager = CookieManager.getInstance();
                Log.d(TAG, "onLoadResource: Club:" + url + ", movie:" + movie.getVideoUrl());

                Callback callBack = new Callback() {
                    @Override
                    public void onCallback(String value, int counter) {
                        if (counter != 0) {
                            return;
                        }
                        Log.d(TAG, "onCallback: " + counter);
                        setCookies(cookieManager.getCookie(movie.getVideoUrl()));
                        Intent returnIntent = new Intent(activity, DetailsActivity.class);
                        movie.setFetch(0); //tell next activity not to fetch movie on start
                        Gson gson = new Gson();
                        Type movieListType = new TypeToken<List<Movie>>() {
                        }.getType();
                        List<Movie> movies = gson.fromJson(value, movieListType);

                        for (Movie mov : movies) {
                            movies.get(movies.indexOf(mov)).setState(detectMovieState(mov));
                        }

                        String jsonMovies = gson.toJson(movies);
                        returnIntent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
                        returnIntent.putExtra(DetailsActivity.MOVIE_SUBLIST, jsonMovies);
                        activity.startActivity(returnIntent);

                        activity.finish();

                    }
                };

                if (CimaClubServer.RESULT_COUNTER == 0 && !(url.contains("cdn-cgi") || url.contains("challenge"))) {
                    webView.evaluateJavascript(extractMovies, new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.d(TAG, "onReceiveValue:tempValue1: c: " + CimaClubServer.RESULT_COUNTER + ", " + value.length() + ", " + value);
                                    if (value.length() > 4) {
                                        view.stopLoading();
                                        webView.stopLoading();
                                        callBack.onCallback(value, CimaClubServer.RESULT_COUNTER++);
                                    }
                                }
                            }
                    );
                }
            }
        });


        webView.loadUrl(movie.getVideoUrl());
    }


    private class Browser_Home extends WebViewClient {
        Browser_Home() {
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            super.shouldOverrideUrlLoading(view, url);
            Log.i("override", "url: " + url);
            //return !url.contains(getStudioText(movie.getStudio()));
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
        }
    }

    private class ChromeClient extends WebChromeClient {
        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        protected FrameLayout mFullscreenContainer;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        ChromeClient() {
        }


        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(activity.getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) activity.getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            activity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            activity.setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = activity.getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) activity.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            activity.getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    public String getStudioText(String serverName) {

        switch (serverName) {
            case Movie.SERVER_SHAHID4U:
                return "https://shahid4u";
            case Movie.SERVER_FASELHD:
                return "www.faselhd";
            case Movie.SERVER_CIMA4U:
                return "cima4u.io/";
            case Movie.SERVER_AKWAM:
                return "akwam.";

        }

        return "akwam.";
    }


    private class CustomWebViewClient extends WebViewClient {

        private String currentVideoUrl = "";
        boolean scrolled = false;
        List<Movie> movieList = new ArrayList<>();
        Map<String, String> headers = new HashMap<>();
        CookieManager cookieManager = CookieManager.getInstance();
        boolean stopCode = false;

        int counter = 0;
        Movie movie;

        CustomWebViewClient(Movie movie) {
            //cookieManager.setAcceptCookie(true);
            this.movie = movie;
//            cookieManager.setAcceptThirdPartyCookies(webView, true);
//            CookieSyncManager.createInstance(activity);
            CookieSyncManager.getInstance().startSync();

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //return super.shouldOverrideUrlLoading(view, request);
            String newUrl = request.getUrl().toString().length() > 25 ? request.getUrl().toString().substring(0, 25) : request.getUrl().toString();
            Log.d(TAG, "shouldOverrideUrlLoading: " + newUrl);
            if (!BrowserActivity.shouldOverride(newUrl)) {
                Log.d(TAG, "shouldOverrideUrlLoading: no");
                view.loadUrl(request.getUrl().toString());
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            currentVideoUrl = "";
            movieList = new ArrayList<>();
            scrolled = false;
            stopCode = false;
            counter = 0;
            super.onPageStarted(view, url, favicon);
        }


        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            headers = request.getRequestHeaders();
            Log.d(TAG, "shouldInterceptRequest: headers:" + headers.toString());
            return super.shouldInterceptRequest(view, request);
        }

        public void onLoadResource_2(WebView view, String url) {
            Log.d(TAG, "onLoadResource fasel ori: " + url);
            if (!url.contains("challenge") && !url.contains("cdn-cgi")) {
                Log.d(TAG, "onLoadResource: inject");
                String searchContext = "";
                if (movie.getSearchContext() != null) {
                    searchContext = movie.getSearchContext();
                }
                setHeaders(headers);
                //inject js code
                // server.onLoadResource(activity, webView, url, movie);
                //clean webpage from ads
                BrowserActivity.cleanWebPage(view);

                if (!(url.contains("/?s=") || url.contains("/search"))) {
                    //scrolling to video
                    if (!scrolled) {
                        String jsScroll = "function scrollToIframe() {\n" +
                                "  // Find the iframe element\n" +
                                "  var iframe = document.querySelector('iframe[src]:not([src=\"about:blank\"])');\n" +
                                "  \n" +
                                "  // Check if the iframe element was found\n" +
                                "  if (iframe) {\n" +
                                "    iframe.scrollIntoView({behavior: 'smooth'});" +
                                "    // Call the callback function with 1\n" +
                                "    return 1;\n" +
                                "  } else {\n" +
                                "    // Call the callback function with 0\n" +
                                "    return 0;\n" +
                                "  }\n" +
                                "}\n" +
                                "\n" +
                                "scrollToIframe();\n";

                        view.evaluateJavascript(jsScroll, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                // value is a JSON array of src attributes
                                // String[] srcs = new Gson().fromJson(value, String[].class);
                                Log.d(TAG, "onReceiveValue: scrolled: " + value);
                                if (value.equals("1")) {
                                    scrolled = true;
                                }
                                // Do something with the src attributes
                            }
                        });

                    }

                    //fetch video
                    if (BrowserActivity.isVideo(url) && !url.equals(currentVideoUrl)) {
                        String newUrl = url;
                        // server.setHeaders(headers);
                        //server.setCookies(cookieManager.getCookie(movie.getVideoUrl()));
                        //   if (url.endsWith("m3u8")){
                        //convert headers to string
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
                        String headerString = sb.toString();
                        newUrl = url + "|" + headerString;
                        // newUrl =url+"|Referer=https://vidshar.org/";
                        //   }
                        Log.d("yessss1", newUrl + "");

                        Uri uri = Uri.parse(newUrl);
//                        //to play master.m3u8 not other versions
//                        if (currentVideoUrl.endsWith(".m3u8") && currentVideoUrl.contains("master")){
//                            uri = Uri.parse(currentVideoUrl);
//                        }else {
                        currentVideoUrl = url;
                        //  }

                        //String newUrl=  "https://s49.vidsharcdn.com/hls/,pdomb2axiwm4f4kmlfccf2dfhe5pjdnzs2my25t67wekxuae4atqnrryu7kq,.urlset/master.m3u8|Referer=https://vidshar.org/";
                        String type = "video/*"; // It works for all video application
                        //  webView.stopLoading();
                        Log.d("yessss2", uri + "");
                        Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
                        in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //  in1.setPackage("org.videolan.vlc");
                        in1.setDataAndType(uri, type);
                        activity.startActivity(in1);
                    }
                }
            } else {
                super.onLoadResource(view, url);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            //super.onReceivedSslError(view, handler, error);
            Log.d(TAG, "onReceivedSslError: " + error.toString());
            // Ignore SSL certificate errors
            handler.proceed();
        }


        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String cookies = cookieManager.getCookie(url);
            Log.d(TAG, "onPageFinished: " + url + ", Cookies:" + cookies);
            if (cookies == null) {
                cookieManager.setCookie(url, getCookies());
            } else {
                if (getCookies() == null) {
                    setCookies(cookies);
                } else {
                    if (!getCookies().contains(cookies)) {
                        // Add the cookie to the string
                        setCookies(getCookies() + "; " + cookies);
                    }
                }
            }
            Log.d(TAG, "onPageFinished: cookie:" + cookies);
        }

    }

    interface Callback {
        void onCallback(String value, int counter);
    }


    @Override
    public void setReferer(String referer) {
        this.referer = referer;
    }

    @Override
    public String getReferer() {
        return referer;
    }
    @Override
    public String getWebScript(int mode, Movie movie) {
        String script = null;
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_STARTED){
                if (movie.getState() == Movie.COOKIE_STATE) {
                    Log.d(TAG, "getScript: SERVER_CIMA_CLUB COOKIE_STATE");
                    script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                            "var postDivs = document.getElementsByClassName(\"content-box\");" +
                            "if (postDivs.length > 0){" +
                            "var postList = [];" +
                            "for (var i = 0; i < postDivs.length; i++) {" +
                            "    var post = {};" +
                            "    var postDiv = postDivs[i];" +
                            "    var imageDiv = postDiv.getElementsByClassName(\"image\")[0];" +
                            "    var rateEle = postDiv.getElementsByClassName(\"rate ti-star\");" +
                            "    if(rateEle.length > 0){" +
                            "    post.rate = rateEle[0].textContent;" +
                            "    }" +
                            "    post.title = postDiv.getElementsByTagName(\"h3\")[0].textContent;" +
                            "    post.videoUrl = imageDiv.href;" +
                            "    post.cardImageUrl = imageDiv.getAttribute('data-src');" +
                            "    post.bgImageUrl = post.cardImageUrl;" +
                            "    post.studio = \"" + Movie.SERVER_CIMA_CLUB + "\";" +
                            "    post.state = 0;" +
                            "    postList.push(post);" +
                            "}" +
                            "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                            "}" +
                            "});";

                }
                else if (movie.getState() == Movie.GROUP_OF_GROUP_STATE) {
                    Log.d(TAG, "getScript:cimaClub GROUP_OF_GROUP_STATE");
                    script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                            "var postDivs = document.getElementsByClassName(\"content-box\");" +
                            "if (postDivs.length > 0){" +
                            "var postList = [];" +
                            "for (var i = 0; i < postDivs.length; i++) {" +
                            "    var post = {};" +
                            "    var postDiv = postDivs[i];" +
                            "    var imageDiv = postDiv.getElementsByClassName(\"image\")[0];" +
                            "    var rateEle = postDiv.getElementsByClassName(\"rate ti-star\");" +
                            "    if(rateEle.length > 0){" +
                            "    post.rate = rateEle[0].textContent;" +
                            "    }" +
                            "    post.title = postDiv.getElementsByTagName(\"h3\")[0].textContent;" +
                            "    post.videoUrl = imageDiv.href;" +
                            "    post.cardImageUrl = imageDiv.getAttribute('data-src');" +
                            "    post.bgImageUrl = post.cardImageUrl;" +
                            "    post.studio = \"" + Movie.SERVER_CIMA_CLUB + "\";" +
                            "    post.state = 1;" +
                            "    postList.push(post);" +
                            "}" +
                            "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                            "}" +
                            "});";
                }
                else if (movie.getState() == Movie.GROUP_STATE) {
                    Log.d(TAG, "getScript:cimaClub GROUP_OF_GROUP_STATE");
                    script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                            "var postDivs = document.getElementsByClassName(\"content-box\");" +
                            "if (postDivs.length > 0){" +
                            "var postList = [];" +
                            "for (var i = 0; i < postDivs.length; i++) {" +
                            "    var post = {};" +
                            "    var postDiv = postDivs[i];" +
                            "    var imageDiv = postDiv.getElementsByClassName(\"image\")[0];" +
                            "    var rateEle = postDiv.getElementsByClassName(\"rate ti-star\");" +
                            "    if(rateEle.length > 0){" +
                            "    post.rate = rateEle[0].textContent;" +
                            "    }" +
                            "    post.title = postDiv.getElementsByClassName(\"episode-number\")[0].textContent;" +
                            "    post.videoUrl = imageDiv.href;" +
                            "    post.cardImageUrl = imageDiv.getAttribute('data-src');" +
                            "    post.bgImageUrl = post.cardImageUrl;" +
                            "    post.studio = \"" + Movie.SERVER_CIMA_CLUB + "\";" +
                            "    post.state = 2;" +
                            "    postList.push(post);" +
                            "}" +
                            "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                            "}" +
                            "});";
                }
                else if (movie.getState() == Movie.ITEM_STATE) {
                    //fetch servers
//                    script ="document.addEventListener(\"DOMContentLoaded\", () => {" +
//                                    "var serversTab = document.getElementsByClassName('servers-tabs');" +
//                            "if (serversTab.length > 0){" +
//                            "var postList = [];" +
//                            "var postDivs = serversTab[0].getElementsByTagName('li');" +
//                                "if (postDivs.length > 0){" +
//                                    "for (var i = 0; i < postDivs.length; i++) {" +
//                                    "    var post = {};" +
//                                    "    var postDiv = postDivs[i];" +
//                                    "    post.title = postDiv.textContent.replace(/\\n/g, \"\");" +
//                                   "    post.videoUrl =\""+ movie.getVideoUrl()+"\"+'?'+postDiv.getAttribute('data-embedd');" +
//                                   // "    post.videoUrl = postDiv.getAttribute('data-embedd');" +
//                                    "    post.cardImageUrl = \""+movie.getCardImageUrl()+"\";" +
//                                    "    post.bgImageUrl = post.cardImageUrl;" +
//                                    "    post.studio = \""+Movie.SERVER_CIMA_CLUB+"\";" +
//                                    "    post.state = 5;" +
//                                    "    postList.push(post);" +
//                                    "}" +
//                                "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
//                                "}" +
//                            "}" +
//                            "});";
                    Log.d(TAG, "getScript: SERVER_CIMA_CLUB ITEM_STATE");
                    script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                            "// Get all iframes on the page\n" +
                            "var iframes = document.querySelectorAll('iframe');\n" +
                            "\n" +
                            "// Loop over each iframe\n" +
                            "iframes.forEach(iframe => {\n" +
                            "  // Check if the iframe's src is 'about:blank'\n" +
                            "  if (iframe.src === 'about:blank') {\n" +
                            "    // If it is, delete the iframe from the DOM\n" +
                            "    iframe.parentNode.removeChild(iframe);\n" +
                            "  } else {\n" +
                            "    // If it's not, scroll smoothly to the iframe\n" +
                            "    iframe.scrollIntoView({\n" +
                            "      behavior: 'smooth'\n" +
                            "    });\n" +
                            "  }\n" +
                            "});" +
                            "});";
                }
                else if (movie.getState() == Movie.RESOLUTION_STATE) {
                    Log.d(TAG, "getScript:cimaClub RESOLUTION_STATE");
                    String serverId = "#";
                    String clickServer = "";
                    if (movie.getVideoUrl().contains("?")) {
                        // newUrl = movie.getVideoUrl().substring(0, movie.getVideoUrl().indexOf("?"));
                        serverId = movie.getVideoUrl().substring(movie.getVideoUrl().indexOf("?") + 1);
                        clickServer = "var intendedValue = '" + serverId + "';\n" +
                                " var serversWatchSideElement = document.querySelector('.servers-tabs');\n" +
                                "var elementToClick = serversWatchSideElement.querySelector(\"[data-embedd='" + serverId + "']\");\n" +
                                "elementToClick.click();";
                        Log.d(TAG, "getScript: serverID:" + serverId);
                    }
                    script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                            clickServer +
                            "var iframe = document.querySelector('iframe[src]:not([src=\"about:blank\"])');" +
                            "  // Check if the iframe element was found" +
                            "  if (iframe) {" +
                            "    iframe.scrollIntoView({behavior: 'smooth'});" +
                            "  } " +
                            "// Find the first iframe with src attribute not equal to \"about:blank\"\n" +
                            "var originalIframe = document.querySelector('iframe[src]:not([src=\"about:blank\"])');\n" +
                            "\n" +
                            "if (originalIframe) {\n" +
                            "  // Clone the iframe\n" +
                            "  var clonedIframe = originalIframe.cloneNode(true);\n" +
                            "\n" +
                            "  // Remove all elements from the body\n" +
                            "  document.body.innerHTML = '';\n" +
                            "\n" +
                            "  // Apply CSS to make the cloned iframe take up the entire screen\n" +
                            "  clonedIframe.style.position = 'fixed';\n" +
                            "  clonedIframe.style.top = '0';\n" +
                            "  clonedIframe.style.left = '0';\n" +
                            "  clonedIframe.style.width = '100%';\n" +
                            "  clonedIframe.style.height = '100%';\n" +
                            "\n" +
                            "  // Add the cloned iframe back to the page\n" +
                            "  document.body.appendChild(clonedIframe);\n" +
                            "\n" +
                            "  // Request fullscreen for the cloned iframe\n" +
                            "  if (clonedIframe.requestFullscreen) {\n" +
                            "    clonedIframe.requestFullscreen();\n" +
                            "  } else if (clonedIframe.mozRequestFullScreen) {\n" +
                            "    clonedIframe.mozRequestFullScreen();\n" +
                            "  } else if (clonedIframe.webkitRequestFullscreen) {\n" +
                            "    clonedIframe.webkitRequestFullscreen();\n" +
                            "  } else if (clonedIframe.msRequestFullscreen) {\n" +
                            "    clonedIframe.msRequestFullscreen();\n" +
                            "  }\n" +
                            "\n" +
                            "  // Disable the ability to create new elements\n" +
                            "  document.addEventListener('DOMNodeInserted', function (e) {\n" +
                            "    e.preventDefault();\n" +
                            "    e.stopPropagation();\n" +
                            "    return false;\n" +
                            "  });\n" +
                            "} else {\n" +
                            "  // Handle the case when no iframe with src other than \"about:blank\" is found\n" +
                            "  console.error('No eligible iframe found.');\n" +
                            "}\n" +
                            "});";
                }
        }
        Log.d(TAG, "getWebScript: script:"+script);
        return script;
    }
    @Override
    public void setConfig(ServerConfig serverConfig) {
        this.config = serverConfig;
    }

    @Override
    public ServerConfig getConfig() {
        return this.config;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies() {
        return search(config.url + "/");
    }

    @Override
    public String getLabel() {
        return "سيماكلوب";
    }
}
