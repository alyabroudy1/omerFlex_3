package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;

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
import java.util.ArrayList;
import java.util.List;

public class OldAkwamServer extends AbstractServer {
    ArrayObjectAdapter listRowAdapter;
    TextView descriptionTextView;
    static String TAG = "Old_Akwam";
    static String WEBSITE_NAME = "akwam";
    Activity activity;

    Fragment fragment;
    public static String WEBSITE_URL = "https://www.akwam.cc/old";
    static int GOO_FETCH_PAGE_REQUEST = 1;
    static int VIDEO_LINK_FETCH_PAGE_REQUEST = 2;
    static boolean STOP_BROWSER = false;
    static boolean CLOSE_BROWSER = false;
    private static OldAkwamServer instance;

    private OldAkwamServer(Activity activity, Fragment fragment) {
        // Private constructor to prevent instantiation
        this.activity = activity;
        this.fragment = fragment;
    }

    public static synchronized OldAkwamServer getInstance(Activity activity, Fragment fragment) {
        if (instance == null) {
            instance = new OldAkwamServer(activity, fragment);
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
    public boolean onLoadResource(Activity activity, WebView view, String url, Movie movie) {
        return false;
    }

    @Override
    public void fetchWebResult(Movie movie){}


    @Override
    public ArrayList<Movie> search(String query) {
        Log.i(TAG, "search: " + query);
        String searchContext = query;


        if (getConfig() != null && getConfig().getUrl() != null){
            query = getConfig().getUrl() + "/search/" + query;
        }else {
            query = WEBSITE_URL + "/search/" + query;
        }
        //final String url = query;
        ArrayList<Movie> movieList = new ArrayList<>();


                Document doc = getRequestDoc(query);
                if (doc == null) {
                    return movieList;
                }
                Elements divs = doc.select("div");
                for (Element div : divs) {
                    if (div.hasClass("tags_box")) {
                        String url = div.getElementsByTag("a").attr("href");
                        if (url.contains("لعبة") || url.contains("كورس") || url.contains("تحديث")) {
                            continue;
                        }

                        Movie a = new Movie();
                        a.setStudio(Movie.SERVER_OLD_AKWAM);
                        String image = div.getElementsByTag("div").attr("style");
                        image = image.substring(image.indexOf('(') + 1, image.indexOf(')'));


                        final String title = div.getElementsByTag("h1").text();
                        String rate = "";
                        final String studio = Movie.SERVER_OLD_AKWAM;
                        final String videoUrl = url;
                        final String description = "";
                        final String cardImageUrl = image;
                        final String backgroundImageUrl = image;

                        Movie m = new Movie();
                        m.setTitle(title);
                        m.setDescription(description);
                        m.setStudio(Movie.SERVER_OLD_AKWAM);
                        m.setVideoUrl(videoUrl);
                        m.setCardImageUrl(cardImageUrl);
                        m.setBackgroundImageUrl(backgroundImageUrl);
                        m.setState(Movie.GROUP_STATE);
                        m.setRate(rate);
                        m.setSearchContext(searchContext);
                        m.setMainMovieTitle(videoUrl);
                        m.setMainMovie(m);

                        movieList.add(m);
                    }
                }

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

    @Override
    public Movie fetch(Movie movie) {

        Intent intent = null;

        switch (movie.getState()) {
            case Movie.GROUP_STATE:
                Log.i(TAG, "onItemClick. GROUP_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return fetchGroup(movie);
            case Movie.ITEM_STATE:
                Log.i(TAG, "onItemClick. ITEM_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return fetchItem(movie);
            case Movie.RESOLUTION_STATE:
                Movie clonedMovie = Movie.clone(movie);
                clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
                return fetchResolutions(clonedMovie);
            case Movie.BROWSER_STATE:
                startBrowser(movie.getVideoUrl());
                break;
            case Movie.VIDEO_STATE:
                return movie;
            default:
                return fetchGroup(movie);
        }

        return movie;
    }

    @Override
    public Movie fetchBrowseItem(Movie movie) {
        return null;
    }

    @Override
    public int fetchNextAction(Movie movie) {
        if (movie.getTitle().contains("هذه المادة لا تحتوي علي رابط مباشر")){
            Toast.makeText(activity, "هذه المادة لا تحتوي علي رابط مباشر", Toast.LENGTH_SHORT).show();
            return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
        }
        switch (movie.getState()){
            case Movie.GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY; // to open VideoDetailsActivity
            case Movie.VIDEO_STATE:
                return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
//            case Movie.ITEM_STATE:
//                return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY; // not to open any activity
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
    }

    @Override
    public Movie fetchToWatchLocally(Movie movie) {
        Log.i(TAG, "fetchToWatchLocally: " + movie.getVideoUrl());
        if (movie.getTitle().contains("هذه المادة لا تحتوي علي رابط مباشر")){
            Toast.makeText(activity, "هذه المادة لا تحتوي علي رابط مباشر", Toast.LENGTH_SHORT).show();
            return null;
        }

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
        Log.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());

        return movie;
    }

    @Override
    public Movie fetchGroup(final Movie movie) {
        Log.i(TAG, "fetchGroup: " + movie.getVideoUrl());

                Log.d(TAG, "fetchGroup: source network");
                        Document doc = getRequestDoc(movie.getVideoUrl());
                if (doc == null) {
                    return movie;
                }
//                Document doc = Jsoup.connect(movie.getVideoUrl()).header(
//                        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8").header(
//                        "User-Agent", " Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36").header(
//                        "accept-encoding", "gzip, deflate").header(
//                        "accept-language", "en,en-US;q=0.9").header(
//                        "x-requested-with", "pc1"
//                ).timeout(6000).get();
                //Elements links = doc.select("a[href]");sw


                String bgImage = "";
                String ytLink = "";
                //  if (movie.getBackgroundImageUrl() == null || movie.getBackgroundImageUrl().equals("")) {
                //backgroundImage and trailer
                Elements imageTags = doc.getElementsByClass("main_img");

                for (Element imageTag : imageTags) {

                    Log.d(TAG, "run: bgimage divs : " + imageTag.attr("src"));

                    bgImage = imageTag.attr("src");
                    Log.d(TAG, "run: bgimage found : " + bgImage);
                    break;
                }

                //youtube
                Elements videoTags = doc.getElementsByClass("youtube-player");

                for (Element videoTag : videoTags) {

                    Log.d(TAG, "run: bgimage divs : " + videoTag.attr("data-id"));

                    ytLink = "https://www.youtube.com/embed/" + videoTag.attr("data-id") + "?autoplay=1";
                    // ytLink ="https://www.youtube.com/watch?v="+videoTag.attr("data-id") ;

                    // VideoDetailsFragment.MOVIE_BACKGROUND_IMAGE = bgImage;
                    Log.d(TAG, "run: videoTag found : " + ytLink);
                    break;
                }

                movie.setBackgroundImageUrl(bgImage);
                movie.setTrailerUrl(ytLink);
                //get profile image
                //    String imageLink = doc.getElementsByClass("fancybox-thumbs").attr("href");
                //get description
                String description = doc.getElementsByClass("sub_desc").text();
                movie.setDescription(description);


                //get episodes
                Elements divs = doc.select("div");
                for (Element div : divs) {
                    if (div.hasClass("sub_episode_links")) {
                        Movie a = new Movie();
                        //  a.setGenre("Old");


                        Log.i(TAG, "old found:" + div.getElementsByTag("h2").text());
                        int state = Movie.ITEM_STATE;


                        String title = div.getElementsByTag("h2").text();
                        String rate = "";
                        String studio = Movie.SERVER_OLD_AKWAM;
                        String videoUrl = div.getElementsByTag("a").attr("href");
                        //String description = "Empty Desc : rate "+ rate;
                        String cardImageUrl = movie.getCardImageUrl();
                        String backgroundImageUrl = bgImage;

                        Movie episode = new Movie();
                        episode.setTitle(title);
                        episode.setDescription(description);
                        episode.setStudio(studio);
                        episode.setVideoUrl(videoUrl);
                        episode.setCardImageUrl(cardImageUrl);
                        episode.setBackgroundImageUrl(backgroundImageUrl);
                        episode.setState(Movie.ITEM_STATE);
                        episode.setRate(rate);
                        episode.setCreatedAt(movie.getCreatedAt());
                        episode.setMainMovieTitle(movie.getMainMovieTitle());
                        if (movie.getSubList() == null ){
                            movie.setSubList(new ArrayList<>());
                        }
                        movie.addSubList(episode);
                    } else if (div.hasClass("sub_direct_links")) {
                        String name = div.getElementsByClass("sub_file_title").text();
                        String url = div.getElementsByTag("a").attr("href");
                        if (name.contains("للتصميم الجديد")) {
                            name = "هذه المادة لا تحتوي علي رابط مباشر، بسبب انتهاء مدة الملف";
                            url = "";
                            //break;
                        }
                        Movie a = new Movie();
                        //a.setGenre("Old");

                        Log.i(TAG, "old found:" + a.getTitle());


                        String title = name;
                        String rate = "";
                        String studio = Movie.SERVER_OLD_AKWAM;
                        String videoUrl = url;
                        //String description = "Empty Desc : rate "+ rate;
                        String cardImageUrl = movie.getCardImageUrl();
                        String backgroundImageUrl = movie.getCardImageUrl();

                        Movie episode = new Movie();
                        episode.setTitle(title);
                        episode.setDescription(description);
                        episode.setStudio(studio);
                        episode.setVideoUrl(videoUrl);
                        episode.setCardImageUrl(cardImageUrl);
                        episode.setBackgroundImageUrl(backgroundImageUrl);
                        episode.setState(Movie.ITEM_STATE);
                        episode.setRate(rate);
                        episode.setCreatedAt(movie.getCreatedAt());
                        episode.setMainMovieTitle(movie.getMainMovieTitle());
                        if(movie.getSubList() == null){
                            movie.setSubList(new ArrayList<>());
                        }
                        movie.addSubList(episode);
                    }
                }
        return movie;
    }

    private Movie startWebForResultActivity(Movie movie) {
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        Intent browse = new Intent(activity, BrowserActivity.class);
        browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
        browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
        //   Log.d(TAG, "getResultFromWeb: activity:" + fragment.getClass().getName());
        //activity.startActivity(browse);
        fragment.startActivityForResult(browse, movie.getFetch());
        //activity.startActivity(browse);
//            }
//        });

        return movie;
    }

    @Override
    public Movie fetchItem(Movie movie) {
        if (movie.getTitle().contains("هذه المادة لا تحتوي علي رابط مباشر")){
            Toast.makeText(activity, "هذه المادة لا تحتوي علي رابط مباشر", Toast.LENGTH_SHORT).show();
            return null;
        }

        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setState(Movie.RESOLUTION_STATE);

        if(movie.getSubList() == null){
            movie.setSubList(new ArrayList<>());
        }
        movie.addSubList(clonedMovie);
        return movie;
//
//        final WebView simpleWebView = activity.findViewById(R.id.webView);
//        if (movie.getVideoUrl().contains("download")) {
//            Log.d(TAG, "fetchItem: link contains download");
//           fetchServerListItem(movie, simpleWebView);
//        } else {
//            Log.d(TAG, "fetchItem: link contains noo download");
//            OldAkwamController.STOP_BROWSER = false; //important to run other links
//            OldAkwamController.CLOSE_BROWSER = false; //important to run other links
//
//            //final WebView simpleWebView = activity.findViewById(R.id.webView);
//            simpleWebView.clearCache(true);
//            simpleWebView.clearFormData();
//            simpleWebView.clearHistory();
//
//            simpleWebView.setWebViewClient(new Browser_Home() {
//                @Override
//                public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                    Log.d("WEBCLIENT", "OnreDirect url:" + url);
//                    return false;
//                }
//
//                @Override
//                public void onPageFinished(WebView view, String url) {
//                    super.onPageFinished(view, url);
//                    Log.d("WEBCLIENT", "onPageFinished");
//                }
//
//                @Override
//                public void onLoadResource(final WebView view, String url) {
//                    super.onLoadResource(view, url);
//                    Log.d("WEBCLIENT", "onLoadResource");
//                    if (!OldAkwamController.STOP_BROWSER) {
//                        view.evaluateJavascript("(function() { var x = document.getElementsByClassName(\"unauth_capsule clearfix\")[0].getElementsByTagName(\"a\")[0].getAttribute(\"ng-href\"); return x.toString();})();", new ValueCallback<String>() {
//                            @Override
//                            public void onReceiveValue(String s) {
//                                Log.d("LogName", s); // Prints the string 'null' NOT Java null
//                                if (s.contains("/download/")) {
//                                    simpleWebView.stopLoading();
//                                    view.stopLoading();
//                                    Log.d("isDownload", "OnreDirect url:" + s);
//                                    OldAkwamController.STOP_BROWSER = true;
//                                    OldAkwamController.CLOSE_BROWSER = true;
//                                    s = s.replace("\"", "");
//
//                                    Movie mm = Movie.clone(movie);
//                                    mm.setVideoUrl(s);
//                                    mm.setMainMovieTitle(movie.getVideoUrl());
//                                    if (movie.getMainMovie() != null){
//                                        mm.setMainMovie(movie.getMainMovie());
//                                    }else {
//                                        mm.setMainMovie(movie);
//                                    }
//                                    fetchServerListItem(mm, simpleWebView);
//                           //         activity.finish();
//
//                                    /*
//                                    activity.runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if (OldAkwamController.CLOSE_BROWSER) {
//                                                activity.finish();
//                                            }
//                                        }
//                                    });
//
//                                     */
//                                }
//                            }
//                        });
//                        //view.loadUrl("javascript:location.replace(document.getElementsByClassName('unauth_capsule clearfix')[0].getElementsByTagName('a')[0].getAttribute('ng-href'));");
//                    }
//                }
//            });
//            simpleWebView.setWebChromeClient(new ChromeClient());
//            WebSettings webSettings = simpleWebView.getSettings();
//
//            webSettings.setJavaScriptEnabled(true);
//            webSettings.setAllowFileAccess(true);
//            //webSettings.setAppCacheEnabled(false);
//            webSettings.setDomStorageEnabled(true);
//            webSettings.setLoadsImagesAutomatically(false);
//            webSettings.setBlockNetworkImage(true);
//
//            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
//            simpleWebView.loadUrl(movie.getVideoUrl());
//        }
//
//        return movie;
    }

    private Movie fetchItemLinkFromBrowser(Movie movie) {
        if (movie.getTitle().contains("هذه المادة لا تحتوي علي رابط مباشر")){return movie;}
            Intent intent = new Intent(activity, BrowserActivity.class); //start a browser to fetch item
            intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
            activity.startActivity(intent);
        return movie;
    }

    private Movie fetchServerListItem(Movie movie, WebView simpleWebView) {
        Log.d(TAG, "fetchServerListItem: "+movie.getVideoUrl());
        OldAkwamServer.STOP_BROWSER = false; //important to run other links
        OldAkwamServer.CLOSE_BROWSER = false; //important to run other links
        //      final WebView simpleWebView = activity.findViewById(R.id.webView);
        simpleWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        simpleWebView.clearCache(true);
        simpleWebView.clearFormData();
        simpleWebView.clearHistory();


        simpleWebView.setWebViewClient(new Browser_Home() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("WEBCLIENT", "OnreDirect url:" + url);
                String s = url;
                if ((s.contains("download") && s.endsWith(".***")) || s.contains(".link")) {
                    view.stopLoading();
                    OldAkwamServer.STOP_BROWSER = true;
                    simpleWebView.stopLoading();
                    s = s.replace("\"", "");
                    final String finalS = s;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Movie mm = Movie.clone(movie);
                            Log.d(TAG, "run: fetchServerListItem 1: " + mm.getState());
                            mm.setVideoUrl(finalS);
                            mm.setState(Movie.RESOLUTION_STATE);
                            if (movie.getMainMovie() != null){
                                mm.setMainMovie(movie.getMainMovie());
                            }else {
                                mm.setMainMovie(movie);
                            }

                            //Intent intent = new Intent(activity, PlaybackActivity.class);
                            Intent intent = new Intent(activity, DetailsActivity.class);
                            intent.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
                            //mm.save(dbHelper);
                            activity.startActivity(intent);
                            activity.finish();
                        }
                    });
                }

                boolean override = !url.contains(WEBSITE_NAME);
                return override;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WEBCLIENT", "onPageFinished");
            }

            @Override
            public void onLoadResource(final WebView view, String url) {
                Log.d("WEBCLIENT", "stop: "+STOP_BROWSER+", onLoadResource :url" + url);
                if (!OldAkwamServer.STOP_BROWSER) {
                    view.evaluateJavascript("(function() { var x = document.getElementsByClassName(\"download_button\")[0].getAttribute(\"href\").toString(); return x.toString();})();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            Log.d("LogName2", s); // Prints the string 'null' NOT Java null
                            if (s.contains("download") || s.contains(".link")) {
                                Log.d(TAG, "run: fetchServerListItem 2: close: " + OldAkwamServer.STOP_BROWSER);
                                if (!OldAkwamServer.STOP_BROWSER) {//important
                                    view.stopLoading();
                                    OldAkwamServer.STOP_BROWSER = true;
                                    simpleWebView.stopLoading();
                                    s = s.replace("\"", "");
                                    final String finalS = s;

                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Movie mm = Movie.clone(movie);
                                            Log.d(TAG, "run: fetchServerListItem 2: " + mm.getState() + "close: " + OldAkwamServer.STOP_BROWSER);
                                            mm.setVideoUrl(finalS);
                                          //  mm.setState(Movie.RESOLUTION_STATE);
                                            mm.setState(Movie.VIDEO_STATE);

                                            List<Movie> movieList = new ArrayList<>();
                                            movieList.add(mm);
                                            Gson gson = new Gson();
                                            String jsonMovie = gson.toJson(movieList);

                                            Intent returnIntent = new Intent(activity, DetailsActivity.class);
                                            mm.setFetch(0); //tell next activity not to fetch movie on start
                                            returnIntent.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
                                            returnIntent.putExtra(DetailsActivity.MOVIE_SUBLIST, jsonMovie);
                                            activity.startActivity(returnIntent);

//                                            //Intent intent = new Intent(activity, PlaybackActivity.class);
//                                            Intent intent = new Intent(activity, DetailsActivity.class);
//                                            intent.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//                                            Log.d(TAG, "run: start Details activity");
//                                            // mm.save(dbHelper);
//                                            activity.startActivity(intent);
                                            activity.finish();
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
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


        simpleWebView.loadUrl(movie.getVideoUrl());
        return movie;
    }

    @Override
    public void fetchServerList(Movie movie) {
        Log.i(TAG, "fetchServerList: " + movie.getVideoUrl());

    }

    /**
     * fetch a the download page link from the goo page in old_akwam and calls startVideo()
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    @Override
    public Movie fetchResolutions(Movie movie) {
        Log.i(TAG, "fetchResolutions: " + movie.getVideoUrl() + " studio: " + movie.getStudio() + movie.getState() + "desc: " + movie.getDescription());
        if (movie.getTitle().contains("هذه المادة لا تحتوي علي رابط مباشر")){return movie;}

        startWebForResultActivity(movie);

        return null;
//        Movie mm = Movie.clone(movie);
//        mm.setState(Movie.VIDEO_STATE);
//
//        if (movie.getMainMovie() != null){
//            movie.getMainMovie().save(dbHelper);
//        }
//        if (movie.getSubList() == null){
//            mm.setSubList(new ArrayList<>());
//        }
//
//        mm.addSubList(Movie.clone(mm));
//      /*  activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //Collections.reverse(MainActivity.listRowAdapter);
//                listRowAdapter.add(mm);
//            }
//        });
//
//       */
//        return movie;
    }

    /**
     * fetch a video link from the last page (the download page) in old_akwam and run the video
     *
     * @param url String url to run video intent
     */
    @Override
    public void startVideo(String url) {
        Log.i(TAG, "startingVideo: " + url);
        String type = "video/*";
        //Uri uri = Uri.parse(url + "");
        Uri uri = Uri.parse(url);
        Intent videoIntent = new Intent(Intent.ACTION_VIEW, uri);
        videoIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //  in1.setPackage("org.videolan.vlc");
        videoIntent.setDataAndType(uri, type);

        activity.startActivity(videoIntent);
    }

    @Override
    public void startBrowser(String url) {
        Log.i(TAG, "startBrowser: " + url);
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


        simpleWebView.loadUrl(url);

    }

    @Override
    public Movie fetchCookie(Movie movie) {
        return movie;
    }

    @Override
    public boolean isSeries(Movie movie) {
        return false;
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

    @Override
    public int detectMovieState(Movie movie) {return Movie.GROUP_STATE;}

    @Override
    public String getWebScript(int mode, Movie movie) {
        int state = movie.getState();
        String script = "";
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_FINISHED){
            if (state == Movie.RESOLUTION_STATE){
                if (movie.getVideoUrl().contains("download")) {
                    //var x = document.getElementsByClassName("download_button")[0].getAttribute("href"); x;
                    script ="var targetNode = document.getElementsByClassName('download_timer')[0];\n" +
                            "var config = { attributes: true, childList: true, subtree: true };\n" +
                            "\n" +
                            "var callback = function(mutationsList, observer) {\n" +
                            "  for(var mutation of mutationsList) {\n" +
                            "    if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {\n" +
                            "      var link = mutation.target.getElementsByClassName('download_button')[0];\n" +
                            "      if (link && link.href) {\n" +
                                    "         var postList = []; " +
                                    "    var post = {};" +
                                    "    post.videoUrl = link.href;" +
                                    "    post.rowIndex = '" + movie.getRowIndex()+"';"+ //very important
                                    "    post.title = '" + movie.getTitle()+"';"+
                                    "    post.fetch = '" + movie.getFetch()+"';"+
                                    "    post.cardImageUrl = '" + movie.getCardImageUrl()+"';"+
                                    "    post.bgImageUrl = '" + movie.getBgImageUrl()+"';"+
                                    "    post.description = '" + movie.getDescription()+"';"+
                                    "    post.state = '" + Movie.VIDEO_STATE+"';"+
                                    "    post.studio = '" + movie.getStudio()+"';"+
                                    "    postList.push(post);" +
                                    "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                            "        observer.disconnect();\n" +
                            "        break;\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "};\n" +
                            "\n" +
                            "var observer = new MutationObserver(callback);\n" +
                            "\n" +
                            "observer.observe(targetNode, config);"
                            ;
//                                    "var targetNode = document.getElementsByClassName('download_timer');\n" +
//                                    "var config = { attributes: true, childList: true, subtree: true };\n" +
//                                    "var callback = function(mutationsList, observer) {\n" +
//                                    "  for(var mutation of mutationsList) {\n" +
//                                    "    if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {\n" +
//                                    "      var link = mutation.target.getElementsByClassName('download_button');\n" +
//                                    "      if (link && link.href) {\n" +

//                                            "        observer.disconnect();\n" +
//                                            "        break;\n" +
//                                    "      }\n" +
//                                    "    }\n" +
//                                    "  }\n" +
//                                    "};\n" +
//                                    "\n" +
//                                    "var observer = new MutationObserver(callback);\n" +
//                                    "\n" +
//                                     "observer.observe(targetNode[0], config);"
//
//                                              ;
                }else{
                    //var x = document.getElementsByClassName("unauth_capsule clearfix")[0].getElementsByTagName("a")[0].getAttribute("ng-href");
                    script ="var targetNode = document.getElementsByClassName('download_timer')[0];\n" +
                            "var config = { attributes: true, childList: true, subtree: true };\n" +
                            "\n" +
                            "var callback = function(mutationsList, observer) {\n" +
                            "  for(var mutation of mutationsList) {\n" +
                            "    if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {\n" +
                            "      var link = mutation.target.document.getElementsByClassName(\"unauth_capsule clearfix\")[0].getElementsByTagName(\"a\")[0];\n" +
                            "      if (link && link.getAttribute(\"ng-href\")) {\n" +
                            "         var postList = []; " +
                            "    var post = {};" +
                            "    post.videoUrl = link.getAttribute(\"ng-href\");" +
                            "    post.rowIndex = '" + movie.getRowIndex()+"';"+ //very important
                            "    post.title = '" + movie.getTitle()+"';"+
                            "    post.fetch = '" + movie.getFetch()+"';"+
                            "    post.cardImageUrl = '" + movie.getCardImageUrl()+"';"+
                            "    post.bgImageUrl = '" + movie.getBgImageUrl()+"';"+
                            "    post.description = '" + movie.getDescription()+"';"+
                            "    post.state = '" + Movie.VIDEO_STATE+"';"+
                            "    post.studio = '" + movie.getStudio()+"';"+
                            "    postList.push(post);" +
                            "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                            "        observer.disconnect();\n" +
                            "        break;\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "};\n" +
                            "\n" +
                            "var observer = new MutationObserver(callback);\n" +
                            "\n" +
                            "observer.observe(targetNode, config);"
                    ;
                }
            }
        }

        Log.d(TAG, "getScript:Old_akoam RESOLUTION_STATE, mode: "+mode+", script: "+script);
        return script;
    }

    public ArrayList<Movie> getHomepageMovies() {
        return new ArrayList<>();
    }

    @Override
    public String getLabel() {
        return "old_akwam";
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_OLD_AKWAM;
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
