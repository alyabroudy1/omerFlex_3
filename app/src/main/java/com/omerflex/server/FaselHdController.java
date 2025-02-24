package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.DetailsActivity;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaselHdController extends AbstractServer {

    static String TAG = "FaselHd";
    static String WEBSITE_NAME = ".faselhd.";
    public static String WEBSITE_URL = "https://www.faselhd.center";
    Fragment fragment;
    Activity activity;
    static boolean START_BROWSER_CODE = false;
    static boolean STOP_BROWSER_CODE = false;
    static int RESULT_COUNTER = 0;

    private static FaselHdController instance;

    private FaselHdController(Fragment fragment, Activity activity) {
        // Private constructor to prevent instantiation
        this.fragment = fragment;
        this.activity = activity;
    }

    public static synchronized FaselHdController getInstance(Fragment fragment, Activity activity) {
        if (instance == null) {
            instance = new FaselHdController(fragment, activity);
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
        Log.i(TAG, "search: " + query);
        String searchContext = query;
        String queryName = query;
        ArrayList<Movie> movieList = new ArrayList<>();
        // if (!query.contains("faselhd")) {
//        if (headers.containsKey("Referer")){
//            if (headers.get("Referer").contains("?s=")){
//                query = headers.get("Referer");
//            }else {
//                query = headers.get("Referer")+ "/?s=" + query;
//            }
//        }else {
//            query = WEBSITE_URL + "/?s=" + query;
//        }
//        if (referer != null && !referer.isEmpty()){
//            if (referer.endsWith("/")){
//                query = referer + "?s=" + query;
//            }else {
//                query = referer + "/?s=" + query;
//            }
//        }else {
//            query = WEBSITE_URL + "/?s=" + query;
//        }
        String url = query;
        if (!query.contains("http")) {
            url = this.getSearchUrl(query);
        }
        Log.i(getLabel(), "search: " + url);

        Document doc = this.getRequestDoc(url);
        if (doc == null) {
            return null;
        }

        Log.d(TAG, "result stop title: " + doc.title());

        if (!doc.title().contains("moment")) {

            //Elements links = doc.select("a[href]");
            Elements lis = doc.getElementsByClass("postDiv");
            for (Element li : lis) {
                Log.i(TAG, "Fasel element found: ");

                Movie a = new Movie();
                a.setStudio(Movie.SERVER_FASELHD);
                Element videoUrlElem = li.getElementsByAttribute("href").first();
                if (videoUrlElem != null) {
                    String videoUrl = videoUrlElem.attr("href");

                    Element titleElem = li.getElementsByAttribute("alt").first();
                    String title = "";
                    if (titleElem != null) {
                        title = titleElem.attr("alt");
                    }

                    Element imageElem = li.getElementsByAttribute("data-src").first();
                    String image = "";
                    if (imageElem != null) {
                        image = imageElem.attr("data-src");
                    }
                    String rate = "";
                    Elements spans = li.getElementsByTag("span");
                    for (Element span : spans) {
                        if (!span.hasAttr("class")) {
                            rate = span.text();
                            break;
                        }
                    }
                    if (rate.equals("")) {
                        Element rateElem = li.getElementsByClass("pImdb").first();
                        if (rateElem != null) {
                            rate = rateElem.text();
                        }
                    }

                    a.setTitle(title);
                    a.setVideoUrl(videoUrl);
                    a.setCardImageUrl(image);

                    if (isSeries(a)) {
                        a.setState(Movie.GROUP_OF_GROUP_STATE);
                    } else {
                        a.setState(Movie.ITEM_STATE);
                    }
                    final Movie m = new Movie();
                    m.setTitle(title);
                    m.setDescription("");
                    m.setStudio(Movie.SERVER_FASELHD);
                    m.setVideoUrl(videoUrl);
                    m.setCardImageUrl(image);
                    m.setBackgroundImageUrl(image);
                    m.setBgImageUrl(image);
                    m.setState(a.getState());
                    m.setRate(rate);
                    m.setSearchContext(searchContext);
                    m.setCreatedAt(Calendar.getInstance().getTime().toString());
                    m.setMainMovie(m);
                    movieList.add(m);
                }

            }
        } else {

            setCookieRefreshed(false);
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
            m.setStudio(Movie.SERVER_FASELHD);
            m.setVideoUrl(url);
            //  m.setVideoUrl("https://www.google.com/");
            m.setState(Movie.COOKIE_STATE);
            // m.setState(Movie.RESULT_STATE);
            m.setCardImageUrl(cardImageUrl);
            m.setBackgroundImageUrl(backgroundImageUrl);
            m.setRate("");
            m.setSearchContext(searchContext);
            m.setCreatedAt(Calendar.getInstance().getTime().toString());
            movieList.add(m);
        }

        return movieList;
    }

    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return null;
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_FASELHD;
    }

    @Override
    protected Fragment getFragment() {
        return fragment;
    }

    @Override
    protected Activity getActivity() {
        return activity;
    }

    @Override
    protected String getSearchUrl(String query) {
        String searchUrl = query;
        if (query.contains("http")) {
            return query;
        }
        String webLink = getConfig().getUrl();
        if (webLink == null || webLink.isEmpty()) {
            webLink = getConfig().getReferer();
            if (webLink == null || webLink.isEmpty()) {
                webLink = WEBSITE_URL;
            }
        }
        if (webLink.endsWith("/")) {
            searchUrl = webLink + "?s=" + query;
        } else {
            searchUrl = webLink + "/?s=" + query;
        }
        Log.d(TAG, "getSearchUrl: " + searchUrl);
        return searchUrl;
    }

    @Override
    public String getLabel() {
        return "فاصل";
    }

    @Override
    public Movie fetch(Movie movie) {
        Log.d(TAG, "fetch: " + movie.getVideoUrl());
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
                Log.i(TAG, "onItemClick. GROUP_OF_GROUP_STATE" + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return fetchGroupOfGroup(movie);
            //return startWebForResultActivity(movie);
            //return movie;
            case Movie.GROUP_STATE:
                Log.i(TAG, "onItemClick. GROUP_STATE" + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return fetchGroup(movie);
            //return startWebForResultActivity(movie);
            //return movie;
            case Movie.ITEM_STATE:
                Log.i(TAG, "onItemClick. ITEM_STATE" + movie.getStudio() + ". url:" + movie.getVideoUrl());
                //fetchItem(movie);
                // movie.setVideoUrl(movie.getVideoUrl() + "#vihtml");
                //return startWebForResultActivity(movie);
                return fetchItem(movie);
            case Movie.RESOLUTION_STATE:
                Log.i(TAG, "onItemClick. RESOLUTION_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                Movie clonedMovie = Movie.clone(movie);
                clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
                return fetchResolutions(clonedMovie);
            case Movie.BROWSER_STATE:
                Log.i(TAG, "onItemClick. BROWSER_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                //startBrowser(movie.getVideoUrl());
                break;
            case Movie.RESULT_STATE:
                startWebForResultActivity(movie);
                activity.finish();
                return movie;
            case Movie.VIDEO_STATE:
                return movie;
            default:
                return fetchResolutions(movie);
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
//                Intent browse = new Intent(fragment.getActivity(), BrowserActivity.class);
//                browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//                //   Log.d(TAG, "getResultFromWeb: activity:" + fragment.getClass().getName());
//                //activity.startActivity(browse);
//                fragment.startActivityForResult(browse, movie.getFetch());
//            }
//        });
//
//        return movie;
        Intent browse = new Intent(activity, BrowserActivity.class);
        browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
        browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());

        fragment.startActivityForResult(browse, movie.getFetch());
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
    public Movie fetchToWatchLocally(Movie movie) {
        Log.d(TAG, "fetchToWatchLocally: " + movie.getVideoUrl());
        if (movie.getState() == Movie.VIDEO_STATE) {
            return movie;
        }
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);
        fetchResolutions(clonedMovie);
        return null; // to do nothing till result returned to the fragment/activity
    }

    @Override
    public Movie fetchGroupOfGroup(final Movie movie) {
        Log.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());


//            Document doc = Jsoup.connect(movie.getVideoUrl())
//                    .cookies(getMapCookies())
//                    .headers(headers)
//                    .followRedirects(true)
//                    .ignoreHttpErrors(true)
//                    .timeout(0)
//                    .get();

        Document doc = this.getRequestDoc(movie.getVideoUrl());
        if (doc == null) {
            return movie;
        }
        Log.i(TAG, "result stop title: " + doc.title());
        if (doc.title().contains("moment")) {
            setCookieRefreshed(false);
            Movie clonedMovie = Movie.clone(movie);
            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
            return startWebForResultActivity(clonedMovie);
        }

        Element posterImg = doc.selectFirst(".posterImg");
        String backgroundImage = "";
        String trailer = "";
        String description = "";
        if (posterImg != null) {
            Element bgImage = posterImg.selectFirst("img");
            if (bgImage != null) {
                backgroundImage = bgImage.attr("src");
            }

            Element trailerElement = posterImg.selectFirst("a");
            if (trailerElement != null) {
                trailer = trailerElement.attr("href");
            }
        }

        Element singleDesc = doc.selectFirst(".singleDesc");
        if (singleDesc != null) {
            Element desElement = singleDesc.selectFirst("p");
            if (desElement != null) {
                description = desElement.text();
            } else {
                description = singleDesc.text();
            }
        }

        movie.setDescription(description);
        movie.setTrailerUrl(trailer);
        movie.setBackgroundImageUrl(backgroundImage);

        Elements lis = doc.getElementsByClass("seasonDiv");
        for (Element seasonDiv : lis) {
            Log.i(TAG, "Fasel element found: " + description);

            Movie a = Movie.clone(movie);
//                if (headers.containsKey("Referer") && !headers.get("Referer").contains("/s")){
//                    WEBSITE_URL =  headers.get("Referer");
//                }

            String title = "";
            String rate = "";
            String image = "";
            String link = "";
            if (seasonDiv.selectFirst(".title") != null) {
                title = seasonDiv.selectFirst(".title").text();
            }
            if (seasonDiv.selectFirst(".fa-star") != null) {
                rate = seasonDiv.selectFirst(".fa-star").parent().ownText();
            }
            if (seasonDiv.selectFirst("img") != null) {
                image = seasonDiv.selectFirst("img").attr("data-src");
                if (image == null || image.equals("")) {
                    image = seasonDiv.selectFirst("img").attr("src");
                }
            }
            if (seasonDiv.attr("onclick") != null) {
                link = Util.extractDomain(movie.getVideoUrl(), true, false) + seasonDiv.attr("onclick").replace("window.location.href = '", "").replace("'", "");
            }

            Log.d(TAG, "fetchGroupOfGroup: image:" + image);
            Log.d(TAG, "fetchGroupOfGroup: link:" + link);
            a.setTitle(title);
            a.setVideoUrl(link);
            a.setCardImageUrl(image);
            a.setRate(rate);
            a.setState(Movie.GROUP_STATE);
            a.setTrailerUrl(trailer);
            a.setDescription(description);
            a.setStudio(Movie.SERVER_FASELHD);
            a.setBackgroundImageUrl(backgroundImage);
            if (movie.getSubList() == null) {
                movie.setSubList(new ArrayList<>());
            }
            movie.addSubList(a);
        }


        return movie;
    }

    @Override
    public Movie fetchGroup(Movie movie) {
        Log.i(TAG, "fetchGroup: " + movie.getVideoUrl());

        Document doc = this.getRequestDoc(movie.getVideoUrl());
        if (doc == null) {
            return movie;
        }

//            Document doc = Jsoup.connect(movie.getVideoUrl())
//                    .cookies(getMapCookies())
//                    .headers(headers)
//                    .followRedirects(true)
//                    .ignoreHttpErrors(true)
//                    .timeout(0)
//                    .get();
        Log.i(TAG, doc.title());

        if (doc.title().contains("moment")) {
            Movie clonedMovie = Movie.clone(movie);
            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
            return startWebForResultActivity(clonedMovie);
        }

            Element posterImg = doc.selectFirst(".posterImg");
            String backgroundImage = "";

            String description = "";
            if (posterImg != null) {
                Element bgImage = posterImg.selectFirst("img");
                if (bgImage != null) {
                    backgroundImage = bgImage.attr("src");
                }

            }

            Element singleDesc = doc.selectFirst(".singleDesc");
            if (singleDesc != null) {
                Element desElement = singleDesc.selectFirst("p");
                if (desElement != null) {
                    description = desElement.text();
                } else {
                    description = singleDesc.text();
                }
            }

            movie.setDescription(description);
            movie.setBackgroundImageUrl(backgroundImage);

            Element episodeContainer = doc.selectFirst(".epAll");
            Log.d(TAG, "fetchGroup: xxx epAll ");
//                Log.d(TAG+"xxx", "fetchGroup: xxx epAll2 " + episodeContainer);
            if (episodeContainer != null) {
                Elements episodeList = episodeContainer.getElementsByTag("a");
//                    Log.d(TAG+"xxx", "fetchGroup: xxx epAll3 " + episodeList.size());
//                    Log.d(TAG, "fetchGroup: xxx episode " + episodeList.size());
                for (Element episodeDiv : episodeList) {
                    Log.i(TAG, "Fasel episode element found: ");

                    Movie a = Movie.clone(movie);
                    String title = episodeDiv.text();
                    String link = episodeDiv.attr("href");

                    a.setTitle(title);
                    a.setVideoUrl(link);
                    a.setCardImageUrl(movie.getCardImageUrl());
                    a.setRate(movie.getRate());
                    a.setState(Movie.ITEM_STATE);
                    a.setTrailerUrl(movie.getTrailerUrl());
                    a.setDescription(description);
                    a.setStudio(Movie.SERVER_FASELHD);
                    a.setBackgroundImageUrl(backgroundImage);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(a);
                }
            }

        Log.d(TAG, "fetchGroup: result movie: " + movie.getSubList());
        return movie;
    }

    @NonNull
    private WebView getWebView() {
        WebView webView = activity.findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        //webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAllowFileAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        // webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setDomStorageEnabled(true);

        webView.setInitialScale(1);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // Enable hardware acceleration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setBlockNetworkImage(false);
        webSettings.setPluginState(WebSettings.PluginState.OFF);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new ChromeClient());
        return webView;
    }

    @Override
    public Movie fetchItem(Movie movie) {
        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());

        Document doc = this.getRequestDoc(movie.getVideoUrl());
        if (doc == null) {
            return movie;
        }
        Log.i(TAG, "result stop title: " + doc.title());

        if (doc.title().contains("moment")) {
            Movie clonedMovie = Movie.clone(movie);
            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
            return startWebForResultActivity(clonedMovie);
        }
            Element posterImg = doc.selectFirst(".posterImg");
            String backgroundImage = "";

            String description = "";
            if (posterImg != null) {
                Element bgImage = posterImg.selectFirst("img");
                if (bgImage != null) {
                    backgroundImage = bgImage.attr("src");
                }

            }

            Element singleDesc = doc.selectFirst(".singleDesc");
            if (singleDesc != null) {
                Element desElement = singleDesc.selectFirst("p");
                if (desElement != null) {
                    description = desElement.text();
                } else {
                    description = singleDesc.text();
                }
            }

            movie.setDescription(description);
            movie.setBackgroundImageUrl(backgroundImage);

            Element resolutionsTab = doc.selectFirst(".signleWatch");
            if (resolutionsTab != null) {
                Elements episodeList = resolutionsTab.getElementsByTag("li");
                for (Element episodeDiv : episodeList) {

                    Movie a = Movie.clone(movie);

                    String title = "";
                    Element titleElem = episodeDiv.selectFirst("a");
                    if (titleElem != null) {
                        title = titleElem.text();
                    }

                    String link = "";
                    String linkElem = episodeDiv.attr("onclick");
                    if (linkElem != null) {
                        link = linkElem.replace("player_iframe.location.href = ", "").replace("'", "");
                        link = link + Util.generateHeadersForVideoUrl(getConfig().getHeaders());
                    }

                    Log.i(TAG, "Fasel server element found: " + link);

                    a.setTitle(title);
                    a.setVideoUrl(link);
                    a.setCardImageUrl(movie.getCardImageUrl());
                    a.setRate(movie.getRate());
                    a.setState(Movie.RESOLUTION_STATE);
                    a.setTrailerUrl(movie.getTrailerUrl());
                    a.setDescription(description);
                    a.setStudio(Movie.SERVER_FASELHD);
                    a.setBackgroundImageUrl(backgroundImage);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(a);
                }
            }
        return movie;
    }

    @Override
    public void fetchServerList(Movie movie) {
        Log.i(TAG, "fetchServerList: " + movie.getVideoUrl());
    }

    /**
     * should fetch video link from the video page but doesnot work coz of security check
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

    @Override
    public void startVideo(String url) {
        Log.i(TAG, "startVideo: " + url);
        //for now as the web site require security check.
        String type = "video/*"; // It works for all video application
        Uri uri = Uri.parse(url);
        Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
        in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //  in1.setPackage("org.videolan.vlc");
        in1.setDataAndType(uri, type);
        fragment.startActivity(in1);
    }

    @Override
    public void startBrowser(String url) {
        Log.i(TAG, "startBrowser: " + url);
        //   FaselHdController.CURRENT_VIDEO_URL = "";
        FaselHdController.START_BROWSER_CODE = false;
        WebView simpleWebView = fragment.getActivity().findViewById(R.id.webView);
        simpleWebView.loadUrl(url);
    }

    @Override
    public Movie fetchCookie(Movie movie) {
        return null;
    }

    @Override
    public boolean isSeries(Movie movie) {
        String u = movie.getVideoUrl();
        String n = movie.getTitle();
        return u.contains("/seasons") || n.contains("مسلسل");
    }

    //load it from browser activity but doesnt work now cuz of security check
    public void loadFaselVideo(Movie movie) {

        Log.d(TAG, "loadFaselVideo: " + movie.getVideoUrl());

   /*     final Movie m1 = Movie.buildMovieInfo("Auto", "", Movie.SERVER_FASELHD, movie.getVideoUrl(), movie.getCardImageUrl(), movie.getBackgroundImageUrl(), Movie.VIDEO_STATE, "", movie.getHistoryUrl(), movie.getHistoryTitle(), movie.getHistoryState(),
              //  movie.getHistoryCardImageUrl(),
                movie.getCreatedAt(),"");

        String url2 = movie.getVideoUrl().replaceAll("/master.m3u8", "/index-f1-v1-a1.m3u8");
        final Movie m2 = Movie.buildMovieInfo("1080", "", Movie.SERVER_FASELHD, url2, movie.getCardImageUrl(), movie.getBackgroundImageUrl(), Movie.VIDEO_STATE, "", movie.getHistoryUrl(), movie.getHistoryTitle(),movie.getHistoryState(),
             //   movie.getHistoryCardImageUrl(),
                movie.getCreatedAt(),"");

        String url3 = movie.getVideoUrl().replaceAll("/master.m3u8", "/index-f2-v1-a1.m3u8");
        final Movie m3 = Movie.buildMovieInfo("720", "", Movie.SERVER_FASELHD, url3, movie.getCardImageUrl(), movie.getBackgroundImageUrl(), Movie.VIDEO_STATE, "", movie.getHistoryUrl(), movie.getHistoryTitle(),movie.getHistoryState(),
             //   movie.getHistoryCardImageUrl(),
                movie.getCreatedAt(),"");

        String url4 = movie.getVideoUrl().replaceAll("/master.m3u8", "/index-f3-v1-a1.m3u8");
        final Movie m4 = Movie.buildMovieInfo("480", "", Movie.SERVER_FASELHD, url4, movie.getCardImageUrl(), movie.getBackgroundImageUrl(), Movie.VIDEO_STATE, "", movie.getHistoryUrl(), movie.getHistoryTitle(),movie.getHistoryState(),
             //   movie.getHistoryCardImageUrl(),
            //    movie.getCreatedAt(),"");

        String url5 = movie.getVideoUrl().replaceAll("/master.m3u8", "/index-f4-v1-a1.m3u8");
        //final Movie m5 = Movie.buildMovieInfo("320", "", Movie.SERVER_FASELHD, url5, movie.getCardImageUrl(), movie.getBackgroundImageUrl(), Movie.VIDEO_STATE, "", movie.getHistoryUrl(), movie.getHistoryTitle(),movie.getHistoryState(),
        final Movie m5 = new Movie();
                //  movie.getHistoryCardImageUrl(),
              //  movie.getCreatedAt(),"");

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Collections.reverse(LinkSeriesActivity.seriesMovieList);
                listRowAdapter.add(m1);
                listRowAdapter.add(m2);
                listRowAdapter.add(m3);
                listRowAdapter.add(m4);
                listRowAdapter.add(m5);
            }
        });


    */
        //addToHistory(movie);

    }

    @Override
    public boolean onLoadResource(Activity activity, WebView webView, String url, Movie movie) {
    /*    CookieManager cookieManager = CookieManager.getInstance();
        Log.d(TAG, "onLoadResource: Fasel:" + url + ", movie:" + movie.getVideoUrl());
        String extractMovies =
                "var postList = [];\n" +
                        "var postDivs = document.getElementsByClassName(\"postDiv\");\n" +
                        "for (var i = 0; i < postDivs.length; i++) {\n" +
                        "    var post = {};\n" +
                        "    var postDiv = postDivs[i];\n" +
                        "    post.title = postDiv.getElementsByTagName(\"img\")[0].alt;\n" +
                        "    post.videoUrl = postDiv.getElementsByTagName(\"a\")[0].href;\n" +
                        "    post.cardImageUrl = postDiv.getElementsByTagName(\"img\")[0].getAttribute('data-src');\n" +
                        "    post.bgImageUrl = post.cardImageUrl;" +
                        "    post.studio = 'FaselHd';" +
                        "    post.state = 0;" +
                        "    postList.push(post);\n" +
                        "}\n" +
                        "postList;\n";

        // String web = FaselHdController.WEBSITE_URL + "/?p=";
        String fetchGroupOfGroup =
                "var postList = [];\n" +
                        "var seasons = document.querySelectorAll('.seasonDiv');\n" +
                        "var description = document.getElementsByClassName(\"singleDesc\")[0].innerHTML.replace(/<.*?>/g, \"\").replace(/(\\r\\n|\\n|\\r)/gm,\"\");" +
                        "for (var i = 0; i < seasons.length; i++) {\n" +
                        "    var post = {};\n" +
                        "    var season = seasons[i];\n" +
                        "    post.videoUrl = 'https://www.faselhd.club/?p='+ season.getAttribute('data-href');\n" +
                        "    post.title = season.querySelector('.title').textContent;\n" +
                        "    post.cardImageUrl = season.querySelector('[data-src]').getAttribute('data-src');\n" +
                        "    post.bgImageUrl = post.cardImageUrl;\n" +
                        "    post.description = description;\n" +
                        "    var spans = season.querySelectorAll('.seasonMeta');\n" +
                        "    for (var j = 0; j < spans.length; j++) {\n" +
                        "        post.rate = spans[j].querySelector('*').textContent;\n" +
                        "        break;\n" +
                        "    }\n" +
                        "    post.state = 1;\n" +
                        "    post.studio = 'FaselHd';\n" +
                        "    postList.push(post);\n" +
                        "}\n" +
                        "postList;\n";

        String fetchGroup =
                "//fetch description\n" +
                        "        var description = document.getElementsByClassName(\"singleDesc\")[0].innerHTML.replace(/<.*?>/g, \"\").replace(/(\\r\\n|\\n|\\r)/gm,\"\");" +
                        "        //fetch session\n" +
                        "        var boxs = document.getElementsByClassName(\"seasonDiv active\");\n" +
                        "        var postList = [];\n" +
                        "        if (boxs.length == 0){\n" +
                        "            var title = document.getElementsByClassName(\"h1 title\")[0].text;\n" +
                        "            var cardImageUrl = document.getElementsByClassName(\"img-fluid\")[0].getAttribute(\"src\");\n" +
                        "            var divs = document.getElementById(\"epAll\").querySelectorAll(\"[href]\");\n" +
                        "            for (const div of divs) {\n" +
                        "                var post = {};\n" +
                        "                post.cardImageUrl = cardImageUrl;\n" +
                        "                post.bgImageUrl = cardImageUrl;\n" +
                        "                post.backgroundImageUrl = cardImageUrl;\n" +
                        "                post.videoUrl = div.getAttribute(\"href\");\n" +
                        "                post.title = div.innerHTML.replace(/<.*?>/g, \"\").replace(/(\\r\\n|\\n|\\r)/gm,\"\").trim();\n" +
                        "                post.description = description;\n" +
                        "                post.state = 2;\n" +
                        "                post.rate = '';\n" +
                        "                post.studio = 'FaselHd';\n" +
                        "                postList.push(post);\n" +
                        "               console.log('box0: '+post.toString());" +
                        "            }\n" +
                        "        }else{" +
                        "        for (var i = 0; i < boxs.length; i++) {\n" +
                        "            var title = boxs[i].getElementsByClassName(\"title\")[0].textContent;\n" +
                        "            var cardImageUrl = boxs[i].querySelectorAll(\"[data-src]\")[0].getAttribute(\"data-src\");\n" +
                        "                var divs = document.getElementById(\"epAll\").getElementsByTagName(\"a\");\n" +
                        "                for (const div of divs) {\n" +
                        "                var post = {};\n" +
                        "                post.cardImageUrl = cardImageUrl;\n" +
                        "                post.bgImageUrl = cardImageUrl;\n" +
                        "                post.backgroundImageUrl = cardImageUrl;\n" +
                        "                post.videoUrl = div.getAttribute(\"href\");\n" +
                        "                post.title = div.innerHTML.replace(/<.*?>/g, \"\").replace(/(\\r\\n|\\n|\\r)/gm,\"\").trim();\n" +
                        "                post.description = description;\n" +
                        "                post.state = 2;\n" +
                        "                post.rate = '';\n" +
                        "                post.studio = 'FaselHd';\n" +
                        "                postList.push(post);\n" +
                        "            console.log('box2: '+post.title+', '+post.videoUrl);" +
                        "               }\n" +
                        "         }\n" +
                        "      }" +
                        "postList;";
        Callback callBack = new Callback() {
            @Override
            public void onCallback(String value, int counter) {
                Log.d(TAG, "onCallback: " +counter + ", "+ url + ", " + value);
                webView.stopLoading();
                // webView.destroy();
                // Remove the WebView from its parent view
//                ViewGroup parent = (ViewGroup) webView.getParent();
//                if (parent != null) {
//                    parent.removeView(webView);
//                }
                // Remove any child views from the WebView
                // webView.removeAllViews();
                // Destroy the WebView
                //   webView.destroy();
                setCookies(cookieManager.getCookie(movie.getVideoUrl()));
                setHeaders(headers);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", value);
                activity.setResult(Activity.RESULT_OK, returnIntent);

                activity.finish();
                return; // to stop loading resources
            }
        };

        String faselJsCode = "";
        if (movie.getState() == Movie.RESULT_STATE) {
            if (url.equals(movie.getVideoUrl())) {
                faselJsCode = extractMovies;
            }
        } else if (movie.getState() == Movie.GROUP_OF_GROUP_STATE) {
            if (url.equals(movie.getVideoUrl())) {
                faselJsCode = fetchGroupOfGroup;
            }
        } else if (movie.getState() == Movie.GROUP_STATE) {
            faselJsCode = fetchGroup;
        } else if (movie.getState() == Movie.ITEM_STATE) {
            // view.loadUrl("javascript:window.confirm = function() { return false; }");
            // view.loadUrl("javascript:window.addEventListener('beforeunload', function (e) { e.preventDefault(); });");
            //  view.loadUrl("javascript:window.frames['player_iframe'].click()");
            String jsClickScript = "(function() {" +
                    "var playerIframe = document.getElementById('player_iframe');" +
                    "if (playerIframe) {" +
                    "playerIframe.click();" +
                    "}" +
                    "})();";
            webView.evaluateJavascript(jsClickScript, null);
        }

        webView.evaluateJavascript(faselJsCode, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (value.length() > 4) {
                            Log.d(TAG, "onReceiveValue:tempValue1: " + value.length() + ", " + value);
                            webView.stopLoading();
                           // callBack.onCallback(value);
                            setCookies(cookieManager.getCookie(movie.getVideoUrl()));
                            setHeaders(headers);
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("result", value);
                            activity.setResult(Activity.RESULT_OK, returnIntent);

                            activity.finish();
                        }
                    }
                }
        );
*/
        return true;
    }

    @Override
    public void fetchWebResult(Movie movie) {
//        WebView webView = activity.findViewById(R.id.webView);
//        webView.loadUrl(movie.getVideoUrl());
        WebView webView = getWebView();
        FaselHdController.RESULT_COUNTER = 0;
        //WebView webView = MyApplication.getWebView();
        int counter = 0;

        webView.setWebViewClient(new CustomWebViewClient(movie) {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                CookieManager cookieManager = CookieManager.getInstance();
                Log.d(TAG, "onLoadResource: Fasel:" + url + ", movie:" + movie.getVideoUrl());
                String extractMovies =
                        "var postList = [];\n" +
                                "var postDivs = document.getElementsByClassName(\"postDiv\");\n" +
                                "for (var i = 0; i < postDivs.length; i++) {\n" +
                                "    var post = {};\n" +
                                "    var postDiv = postDivs[i];\n" +
                                "    post.title = postDiv.getElementsByTagName(\"img\")[0].alt;\n" +
                                "    post.videoUrl = postDiv.getElementsByTagName(\"a\")[0].href;\n" +
                                "    post.cardImageUrl = postDiv.getElementsByTagName(\"img\")[0].getAttribute('data-src');\n" +
                                "    post.bgImageUrl = post.cardImageUrl;" +
                                "    post.studio = 'FaselHd';" +
                                "    post.state = 0;" +
                                "    postList.push(post);\n" +
                                "}\n" +
                                "postList;\n";
                Callback callBack = new Callback() {
                    @Override
                    public void onCallback(String value, int counter) {
                        Log.d(TAG, "onCallback: " + counter + ", " + url + ", " + value);
                        if (counter != 0) {
                            return;
                        }
                        webView.stopLoading();
                        // webView.destroy();
                        // Remove the WebView from its parent view
//                ViewGroup parent = (ViewGroup) webView.getParent();
//                if (parent != null) {
//                    parent.removeView(webView);
//                }
                        // Remove any child views from the WebView
                        // webView.removeAllViews();
                        // Destroy the WebView
                        //   webView.destroy();
                        setCookies(cookieManager.getCookie(movie.getVideoUrl()));
                        setHeaders(headers);
                        Intent returnIntent = new Intent(activity, DetailsActivity.class);
                        movie.setFetch(0); //tell next activity not to fetch movie on start
                        Gson gson = new Gson();
                        Type movieListType = new TypeToken<List<Movie>>() {
                        }.getType();
                        List<Movie> movies = gson.fromJson(value, movieListType);

                        for (Movie mov : movies) {
                            if (isSeries(mov)) {
                                movies.get(movies.indexOf(mov)).setState(Movie.GROUP_OF_GROUP_STATE);
                            } else {
                                movies.get(movies.indexOf(mov)).setState(Movie.ITEM_STATE);
                            }
                        }

                        String jsonMovies = gson.toJson(movies);
                        returnIntent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
                        returnIntent.putExtra(DetailsActivity.MOVIE_SUBLIST, jsonMovies);
                        activity.startActivity(returnIntent);


                        //returnIntent.putExtra("result", value);
                        // activity.setResult(Activity.RESULT_OK, returnIntent);

                        activity.finish();
                        return; // to stop loading resources
                    }
                };

                //     if (url.equals(movie.getVideoUrl())) {
                webView.evaluateJavascript(extractMovies, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                if (value.length() > 4) {
                                    Log.d(TAG, "onReceiveValue:tempValue1: " + value.length() + ", " + value);
                                    view.stopLoading();
                                    webView.stopLoading();
                                    callBack.onCallback(value, FaselHdController.RESULT_COUNTER++);
//                                        setCookies(cookieManager.getCookie(movie.getVideoUrl()));
//                                        setHeaders(headers);
//                                        Intent returnIntent = new Intent();
//                                        returnIntent.putExtra("result", value);
//                                        activity.setResult(Activity.RESULT_OK, returnIntent);
//
//                                        activity.finish();
                                }
                            }
                        }
                );
                // }

            }
        });


        webView.loadUrl(movie.getVideoUrl());
    }

    interface Callback {
        void onCallback(String value, int counter);
    }

    private class ChromeClient extends WebChromeClient {
        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        protected FrameLayout mFullscreenContainer;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;
        private float mOriginalMouseElevation;

        ChromeClient() {
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, "onJsAlert: " + message);
            result.cancel();
            //super.onJsAlert(view, url, null, result);
            return true;
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, "onJsBeforeUnload: " + url + ", m:" + message + ", re:" + result);
            result.cancel();
            return true;
        }


        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            Log.d(TAG, "onJsPrompt: " + message);
            result.cancel();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            Log.d(TAG, "onJsConfirm: " + message);
            result.cancel();
            return true;
        }


        public Bitmap getDefaultVideoPoster() {
            Log.d(TAG, "getDefaultVideoPoster: " + mCustomView);
            if (mCustomView == null) {
                return null;
            }
            Log.d(TAG, "getDefaultVideoPoster: " + BitmapFactory.decodeResource(activity.getApplicationContext().getResources(), 2130837573));
            return BitmapFactory.decodeResource(activity.getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            Log.d(TAG, "onHideCustomView: ");
            ((FrameLayout) activity.getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            activity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            activity.setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
            //mouseArrow.setElevation(this.mOriginalMouseElevation);
            //allowArrowEdgeScrolling = true;
        }

        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
            Log.d(TAG, "onShowCustomView: ");
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = activity.getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            //this.mOriginalMouseElevation = mouseArrow.getElevation();

            FrameLayout frameLayout = (FrameLayout) paramView;
            View focusedChild = frameLayout.getFocusedChild();

            //   Log.d(TAG, "onShowCustomView: elevm:" + mouseArrow.getElevation()
            //         + ", elevV:" + paramView.getElevation() + ", elevFu:" + focusedChild.getElevation());
            // Set the mouse arrow's elevation to a higher value than the custom view's elevation
            //mouseArrow.setElevation(focusedChild.getElevation() + 1);


            Log.d(TAG, "onShowCustomView: custom:" + mCustomView);


            FrameLayout.LayoutParams newFrame = new FrameLayout.LayoutParams(-1, -1);
            ((FrameLayout) activity.getWindow().getDecorView()).addView(this.mCustomView, newFrame);
            activity.getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            // mouseArrow.bringToFront();
            //allowArrowEdgeScrolling = false;
            paramCustomViewCallback.onCustomViewHidden();
        }
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
            setCookies(cookieManager.getCookie(FaselHdController.WEBSITE_URL));
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //return super.shouldOverrideUrlLoading(view, request);
            String newUrl = request.getUrl().toString().length() > 25 ? request.getUrl().toString().substring(0, 25) : request.getUrl().toString();
            Log.d(TAG, "shouldOverrideUrlLoading: " + newUrl);
            if (!BrowserActivity.shouldOverride(newUrl)) {
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
                    if (BrowserActivity.isVideo(url, movie) && !url.equals(currentVideoUrl)) {
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

    @Override
    public int detectMovieState(Movie movie) {
        String u = movie.getVideoUrl();
        String n = movie.getTitle();
        boolean series = u.contains("/seasons") || n.contains("مسلسل");
        if (series) {
            return Movie.GROUP_OF_GROUP_STATE;
        } else {
            return Movie.ITEM_STATE;
        }
    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        String script = "";
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_STARTED) {
            if (movie.getState() == Movie.COOKIE_STATE) {
                Log.d(TAG, "getScript:WEB_VIEW_MODE_ON_PAGE_STARTED COOKIE_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        // "alert(\"DOM ready!\");" +
                        "var postDivs = document.getElementsByClassName(\"postDiv\");" +
                        "if (postDivs.length > 0){" +
                        "var postList = [];" +
                        "for (var i = 0; i < postDivs.length; i++) {" +
                        "    var post = {};" +
                        "    var postDiv = postDivs[i];" +
                        "    post.title = postDiv.getElementsByTagName(\"img\")[0].alt;" +
                        "    post.videoUrl = postDiv.getElementsByTagName(\"a\")[0].href;" +
                        "    post.cardImageUrl = postDiv.getElementsByTagName(\"img\")[0].getAttribute('data-src');" +
                        "    post.bgImageUrl = post.cardImageUrl;" +
                        "    post.backgroundImageUrl = post.cardImageUrl;" +
                        "    post.studio = '" + Movie.SERVER_FASELHD + "';" +
                        "    post.state = 0;" +
                        "    postList.push(post);" +
                        "}" +
                        //"postList;"+
                        "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                        "}" +
                        "});";
            } else if (movie.getState() == Movie.GROUP_OF_GROUP_STATE) {
                Log.d(TAG, "getScript:Fasel WEB_VIEW_MODE_ON_PAGE_STARTED GROUP_OF_GROUP_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        "var seasons = document.querySelectorAll('.seasonDiv');" +
                        "if (seasons.length > 0){" +
                        "var postList = [];" +
                        "var description = document.getElementsByClassName(\"singleDesc\")[0].innerHTML.replace(/<.*?>/g, \"\").replace(/(\\r\\n|\\n|\\r)/gm,\"\");" +
                        "for (var i = 0; i < seasons.length; i++) {" +
                        "    var post = {};" +
                        "    var season = seasons[i];" +
                        "    post.videoUrl = 'https://www.faselhd.ac/'+ season.getAttribute('onclick').match(/\\?p=.[^']*/);;" +
                        //   "    post.videoUrl = 'https://www.faselhd.club/?p='+ season.getAttribute('data-href');" +
                        "    post.title = season.querySelector('.title').textContent;" +
                        "    post.cardImageUrl = season.querySelector('[data-src]').getAttribute('data-src');" +
                        "    post.bgImageUrl = post.cardImageUrl;" +
                        "    post.description = description;" +
                        "    var spans = season.querySelectorAll('.seasonMeta');" +
                        "    for (var j = 0; j < spans.length; j++) {" +
                        "        post.rate = spans[j].querySelector('*').textContent;" +
                        "        break;" +
                        "    }" +
                        "    post.state = 1;" +
                        "    post.studio = '" + Movie.SERVER_FASELHD + "';" +
                        "    postList.push(post);" +
                        "}" +
                        "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                        "}" +
                        "});";
            } else if (movie.getState() == Movie.GROUP_STATE) {
                Log.d(TAG, "getScript:Fasel WEB_VIEW_MODE_ON_PAGE_STARTED GROUP_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        "const description = document.getElementsByClassName('singleDesc')[0].innerHTML.replace(/<.*?>/g, '').replace(/(\\\\\\\\r\\\\\\\\n|\\\\\\\\n|\\\\\\\\r)/gm,'');\n" +
                        "                        const boxs = document.getElementsByClassName('seasonDiv active');\n" +
                        "                        const postList = [];\n" +
                        "                        if (boxs.length === 0){\n" +
                        "                        var title = document.getElementsByClassName('h1 title')[0].text;\n" +
                        "                        var cardImageUrl = document.getElementsByClassName('img-fluid')[0].getAttribute('src');\n" +
                        "                        var divs = document.getElementById('epAll').querySelectorAll('[href]');\n" +
                        "                        for (const div of divs) {\n" +
                        "                        var post = {};\n" +
                        "                        post.cardImageUrl = cardImageUrl;\n" +
                        "                        post.bgImageUrl = cardImageUrl;\n" +
                        "                        post.backgroundImageUrl = cardImageUrl;\n" +
                        "                        post.videoUrl = div.getAttribute('href');\n" +
                        "                        post.title = div.innerHTML.replace(/<.*?>/g, '').replace(/(\\\\\\\\r\\\\\\\\n|\\\\\\\\n|\\\\\\\\r)/gm,'').trim();\n" +
                        "                        post.description = description;\n" +
                        "                        post.state = 2;\n" +
                        "                        post.rate = '';\n" +
                        "                        post.studio = 'FaselHd';\n" +
                        "                        postList.push(post);\n" +
                        "                        }\n" +
                        "                        }else{\n" +
                        "                        for (var i = 0; i < boxs.length; i++) {\n" +
                        "                        var title = boxs[i].getElementsByClassName('title')[0].textContent;\n" +
                        "                        var cardImageUrl = boxs[i].querySelectorAll('[data-src]')[0].getAttribute('data-src');\n" +
                        "                        var divs = document.getElementById('epAll').getElementsByTagName('a');\n" +
                        "                        for (const div of divs) {\n" +
                        "                        var post = {};\n" +
                        "                        post.cardImageUrl = cardImageUrl;\n" +
                        "                        post.bgImageUrl = cardImageUrl;\n" +
                        "                        post.backgroundImageUrl = cardImageUrl;\n" +
                        "                        post.videoUrl = div.getAttribute('href');\n" +
                        "                        post.title = div.innerHTML.replace(/<.*?>/g, '').replace(/(\\\\\\\\r\\\\\\\\n|\\\\\\\\n|\\\\\\\\r)/gm,'').trim();\n" +
                        "                        post.description = description;\n" +
                        "                        post.state = 2;\n" +
                        "                        post.rate = '';\n" +
                        "    post.studio = '" + Movie.SERVER_FASELHD + "';" +
                        "                        postList.push(post);\n" +
                        "                        }\n" +
                        "                        }\n" +
                        "                        }" +
                        "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                        // "MyJavaScriptInterface.myMethod(postList.length);" +
                        "});";
            } else if (movie.getState() == Movie.ITEM_STATE) {
                Log.d(TAG, "getScript:Fasel WEB_VIEW_MODE_ON_PAGE_STARTED ITEM_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        "var iframe = document.querySelector('iframe[src]:not([src=\"about:blank\"])');" +
                        "  // Check if the iframe element was found" +
                        "  if (iframe) {" +
                        "    iframe.scrollIntoView({behavior: 'smooth'});" +
                        "  } " +
                        "});";
            } else if (movie.getState() == Movie.RESOLUTION_STATE) {
                Log.d(TAG, "getScript:Fasel WEB_VIEW_MODE_ON_PAGE_STARTED RESOLUTION_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        "var buttons = document.getElementsByClassName('hd_btn');" +
                        " if (buttons.length > 0){" +
                        "     buttons[0].click();" +
                        "}" +
                        "});";
            }
        } else if (mode == BrowserActivity.WEB_VIEW_MODE_ON_LOAD_RESOURCES) {
            if (movie.getState() == Movie.ITEM_STATE) {
                Log.d(TAG, "getScript: fasel WEB_VIEW_MODE_ON_LOAD_RESOURCES ITEM_STATE");
                script =
//                            "var singeWatch = document.getElementsByClassName(\"signleWatch\");" +
//                                    "if(singeWatch.length > 0){" +
//                                    "singeWatch[0].getElementsByTagName('li')[1].click();" +
//                                    "singeWatch[0].getElementsByTagName('li')[0].click();" +
//                                    "}" +
                        "var singeWatch = document.getElementsByClassName('signleWatch');\n" +
                                "            if (singeWatch.length > 0) {\n" +
                                "                singeWatch[0].getElementsByTagName('li')[1].click();\n" +
                                "                //singeWatch[0].getElementsByTagName('li')[0].click();\n" +
                                "            }\n" +
                                "            var video = document.getElementsByTagName('video');\n" +
                                "            if (video.length > 0){\n" +
                                "                video[0].click();\n" +
                                "            }\n" +
                                "            var button = document.getElementsByClassName('hd_btn');\n" +
                                "            if (button.length > 0){\n" +
                                "                button[0].click();\n" +
                                "                window.frames['player_iframe'].document.querySelectorAll('.hd_btn selected')[0].click();" +
                                "MyJavaScriptInterface.myMethod('clicked')" +
                                "            }";
            }
        }

        Log.d(TAG, "getWebScript: " + script);
        return script;
    }


    @Override
    public ArrayList<Movie> getHomepageMovies() {
        return search(getConfig().getUrl() + "/most_recent");
    }

}
