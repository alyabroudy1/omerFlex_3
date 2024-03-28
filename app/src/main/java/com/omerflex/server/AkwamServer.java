package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.omerflex.entity.dto.ServerConfig;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.DetailsActivity;
import com.omerflex.entity.Movie;
import com.omerflex.R;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * from SearchActivity or MainActivity -> item -> resolutions
 * Or -> GroupOfItem -> item -> resolutions
 * -> if security check -> web browser intent
 * -> else to video intent
 * group + item -> resolution
 */
public class AkwamServer extends AbstractServer {

    static String TAG = "Akwam";
    public static final int REQUEST_CODE = 1;
    static boolean START_BROWSER_CODE = false;
    public static String WEBSITE_URL = "https://www.akwam.cc";
    Activity activity;
    ServerConfig config;

    Fragment fragment;
    private String cookies;
    private String referer;
    private Map<String, String> headers;
    private static AkwamServer instance;

    private AkwamServer(Activity activity, Fragment fragment) {
        // Private constructor to prevent instantiation
        this.activity = activity;
        this.fragment = fragment;
        headers = new HashMap<>();
    }

    public static synchronized AkwamServer getInstance(Activity activity, Fragment fragment) {
        if (instance == null) {
            instance = new AkwamServer(activity, fragment);
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

    /**
     * produce movie from search result if isSeries than Group_State else Item_state
     *
     * @param query name to search for
     * @return
     */
    @Override
    public ArrayList<Movie> search(String query) {
        //Log.i(TAG, "search: " + query);
        String searchContext = query;
//        switch (query) {
//            case "https://akwam.co/movies":
//                break;
//            case "مسلسلات":
//                query = "https://akwam.co/series";
//                break;
//            case "كوميدي":
//                query = "https://akwam.co/movies?section=0&category=20&rating=0&year=0&language=0&format=0&quality=0";
//                break;
//            case "رعب":
//                query = "https://akwam.co/movies?section=0&category=22&rating=0&year=0&language=0&format=0&quality=0";
//                break;
//            default:
//                query = "https://akwam.co/search?q=" + query;
//        }

        if (!query.contains("http")) {
//            if (referer != null && !referer.isEmpty()) {
//                if (referer.endsWith("/")) {
//                    query = referer + "search?q=" + query;
//                } else {
//                    query = referer + "/search?q=" + query;
//                }
//            } else {
//                query = WEBSITE_URL + "/search?q=" + query;
//            }
            if (config != null && config.url != null){
                query = config.url + "/search?q=" + query;
            }else {
                query = WEBSITE_URL + "/search?q=" + query;
            }
        }


        Log.d(TAG, "search: " + query);


        final String url = query;

        ArrayList<Movie> movieList = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .cookies(getMapCookies())
                    .headers(headers)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .get();
            fetchSearchMovies(searchContext, movieList, doc);

        } catch (IOException e) {
            Log.i(TAG, "error" + e.getMessage());
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

    private void fetchSearchMovies(String searchContext, List<Movie> movieList, Document doc) {
        Elements links = doc.getElementsByClass("entry-box");

        for (Element link : links) {
            Elements linkUrlElements = link.getElementsByClass("box");
            if (linkUrlElements.size() == 0) {
                continue;
            }
            String linkUrl = linkUrlElements.attr("href");
            if (
                    linkUrl.contains("/movie") ||
                            linkUrl.contains("/series") ||
                            linkUrl.contains("/episode")
            ) {
                Movie a = new Movie();

                String rate = "";
                Elements rateElem = link.getElementsByClass("label rating");
                if (rateElem.size() > 0) {
                    rate = rateElem.text();
                }


                Elements titleElem = link.getElementsByAttribute("src");
                String title = "";
                String cardImageUrl = "";
                String backgroundImageUrl = "";
                if (titleElem.size() > 0) {
                    title = titleElem.attr("alt");
                    cardImageUrl = titleElem.attr("data-src");
                    backgroundImageUrl = titleElem.attr("data-src");
                }

                String description = "";

                String videoUrl = linkUrl;

                int state = Movie.ITEM_STATE;

                a.setTitle(title);
                a.setVideoUrl(videoUrl);
                Movie movie = new Movie();

                if (isSeries(a)) {
                    state = Movie.GROUP_STATE;
                }
                //Log.d(TAG, "search: isSeries:" + isSeries(a));

                movie.setTitle(title);
                movie.setDescription(description);
                movie.setStudio(Movie.SERVER_AKWAM);
                movie.setVideoUrl(videoUrl);
                movie.setCardImageUrl(cardImageUrl);
                movie.setBackgroundImageUrl(backgroundImageUrl);
                movie.setState(state);
                movie.setRate(rate);
                movie.setSearchContext(searchContext);
                movie.setMainMovie(movie);
                movie.setMainMovieTitle(videoUrl);
                movieList.add(movie);
            }
        }

        Elements nextLinkNaviElements = doc.getElementsByAttribute("rel");
        for (Element naviElem : nextLinkNaviElements) {
            if (naviElem.attr("rel").equals("next")) {
                String videoUrl = naviElem.attr("href");
                Log.d(TAG, "search: nextPage: " + videoUrl);
                Movie nextPage = new Movie();
                nextPage.setTitle("التالي");
                nextPage.setDescription("0");
                nextPage.setStudio(Movie.SERVER_AKWAM);
                nextPage.setVideoUrl(videoUrl);
                nextPage.setCardImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                nextPage.setBackgroundImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                nextPage.setState(Movie.NEXT_PAGE_STATE);
                nextPage.setRate("");
                nextPage.setSearchContext(searchContext);
                nextPage.setMainMovie(nextPage);
                nextPage.setMainMovieTitle(videoUrl);
                movieList.add(nextPage);

                break;
            }
        }
    }

    @Override
    public Movie fetch(Movie movie) {
        Log.i(TAG, "fetch: " + movie.getVideoUrl());
        switch (movie.getState()) {
            case Movie.GROUP_STATE:
                //Log.i(TAG, "onItemClick. GROUP_STATE" + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return fetchGroup(movie);
            case Movie.ITEM_STATE:
                //Log.i(TAG, "onItemClick. ITEM_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                //movie.setDescription("sssssss");
                // return movie;
                return fetchItem(movie);
            case Movie.RESOLUTION_STATE:
                Log.i(TAG, "onItemClick. RESOLUTION_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                Movie clonedMovie = Movie.clone(movie);
                clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
                return fetchResolutions(clonedMovie); // to do nothing and wait till result returned to activity only the first fetch
            case Movie.BROWSER_STATE:
                //Log.i(TAG, "onItemClick. BROWSER_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                startBrowser(movie.getVideoUrl());
                break;
            case Movie.VIDEO_STATE:
                Log.i(TAG, "onItemClick. VIDEO_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
                return movie;
                //startVideo(movie.getVideoUrl());
            default:
                return fetchResolutions(movie);
        }
        return movie;
    }

    @Override
    public Movie fetchBrowseItem(Movie movie) {
        return null;
    }

    @Override
    public int fetchNextAction(Movie movie) {
        Log.d(TAG, "fetchNextAction: "+movie);
        switch (movie.getState()) {
            case Movie.GROUP_STATE:
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
    public Movie fetchGroupOfGroup(Movie movie) {
        //Log.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());
        return movie;
    }

    @Override
    public Movie fetchGroup(final Movie movie) {
        Log.i(TAG, "fetchGroup: " + movie.getVideoUrl());
        final String url = movie.getVideoUrl();
        try {
            Document doc = Jsoup.connect(url)
                    .cookies(getMapCookies())
                    .headers(headers)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .get();

            Elements decDivs = doc.select("h2");
            String description = "";
            for (Element div : decDivs) {
                String desc = div.getElementsByTag("p").html();
                description = desc;
                if (null != description && !description.equals("")) {
                    break;
                }
            }

            if (!description.equals("")) {
                movie.setDescription(description);
            }

            //backgroundImage and trailer
            Elements imageDivs = doc.getElementsByClass("row py-4");

            String bgImage = "";
            String ytLink = "";
            for (Element imageDiv : imageDivs) {
                Elements imageLinks = imageDiv.getElementsByAttribute("href");
                for (Element imagelink : imageLinks) {
                    if (imagelink.attr("href").contains("/uploads/")) {
                        bgImage = imagelink.attr("href");
                    }
                    if (imagelink.attr("href").contains("youtube")) {
                        ytLink = imagelink.attr("href");
                        break;
                    }
                }
                if (!bgImage.equals("")) {
                    break;
                }
            }
            movie.setBackgroundImageUrl(bgImage);
            movie.setTrailerUrl(ytLink);

            Elements links = doc.select("a");
            for (Element link : links) {
                // TODO: find better way to get the link
                if (
                        link.attr("href").contains("/episode") &&
                                link.getElementsByAttribute("src").hasAttr("alt")
                ) {
                    Movie episode = Movie.clone(movie);
                    String title = link.getElementsByAttribute("src").attr("alt");
                    String cardImageUrl = link.getElementsByAttribute("src").attr("src");
                    String backgroundImageUrl = bgImage;

                    String videoUrl = link.attr("href");

                    episode.setTitle(title);
                    episode.setDescription(description);
                    episode.setVideoUrl(videoUrl);
                    episode.setCardImageUrl(cardImageUrl);
                    episode.setBackgroundImageUrl(backgroundImageUrl);
                    episode.setState(Movie.ITEM_STATE);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(episode);
                }
            }

        } catch (IOException e) {
            Log.i(TAG + "failed", e.getMessage() + "");
        }

        return movie;
    }

    @Override
    public Movie fetchItem(final Movie movie) {
        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());
        try {
            String url = movie.getVideoUrl();

            // page2 fetch goo- links
            String p2Caption = "/link/";
//                Document doc = Jsoup.connect(url)
//                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                        .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
//                        .followRedirects(true)
//                        .ignoreHttpErrors(true)
//                        .timeout(0)
//                        .ignoreContentType(true)
//                        .get();
            Document doc = Jsoup.connect(url)
                    .cookies(getMapCookies())
                    .headers(headers)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .get();

            String bgImage = "";
            String ytLink = "";
            //backgroundImage and trailer
            Elements imageDivs = doc.getElementsByClass("row py-4");

            for (Element imageDiv : imageDivs) {
                Elements imageLinks = imageDiv.getElementsByAttribute("href");
                for (Element imagelink : imageLinks) {
                    //Log.d(TAG, "run: bgimage divs : " + imagelink.attr("href"));
                    if (imagelink.attr("href").contains("/uploads/")) {
                        bgImage = imagelink.attr("href");
                        //Log.d(TAG, "run: bgimage found : " + bgImage);
                        //break;
                    }
                    if (imagelink.attr("href").contains("youtube")) {
                        ytLink = imagelink.attr("href");
                        //Log.d(TAG, "run: youtube found : " + bgImage);
                        break;
                    }
                }
                if (!bgImage.equals("")) {
                    break;
                }
            }
            movie.setBackgroundImageUrl(bgImage);
            movie.setTrailerUrl(ytLink);
            if (movie.getMainMovie() != null) {
                movie.getMainMovie().setTrailerUrl(ytLink);
                movie.getMainMovie().setBackgroundImageUrl(bgImage);
            }

            //description
            Elements decDivs = doc.select("h2");
            String description = "";
            if (movie.getDescription().length() < 2) {
                for (Element div : decDivs) {

                    String desc = div.getElementsByTag("p").html();
                    description = desc;
                    //Log.i("description", "found:" + description);
                    if (null != description && !description.equals("")) {
                        break;
                    }
                }
            } else {
                description = movie.getDescription();
            }
            if (movie.getDescription() == null || Objects.equals(movie.getDescription(), "")) {
                movie.setDescription(description);
            }
            if (description == null || Objects.equals(description, "")) {
                description = movie.getDescription();
            }

            //TODO: find better way to fetch links
            Elements divs = doc.getElementsByClass("tab-content quality");
            for (Element div : divs) {
                Elements links = div.getElementsByAttribute("href");
                String title = "";
                String videoUrl = "";

                for (Element link : links) {
                    if (link.attr("href").contains(p2Caption) || link.attr("href").contains("/download/")) {
                        videoUrl = link.attr("href");
                        title = link.text();
                    }
                }
                String backgroundImageUrl = bgImage;

                Movie resolution = Movie.clone(movie);
                resolution.setTitle(title);
                resolution.setDescription(description);
                resolution.setVideoUrl(videoUrl);
                resolution.setBackgroundImageUrl(backgroundImageUrl);
                resolution.setState(Movie.RESOLUTION_STATE);
                // resolution.setState(Movie.VIDEO_STATE);
                if (movie.getSubList() == null) {
                    movie.setSubList(new ArrayList<>());
                }
                movie.addSubList(resolution);
            }
        } catch (IOException e) {
            Log.i(TAG, "error:" + e.getMessage());
        }

        return movie;
    }

    @Override
    public void fetchServerList(Movie movie) {
        Log.i(TAG, "fetchServerList: " + movie.getVideoUrl());
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
        Movie resolution = Movie.clone(movie);// the new movie to be returned*/
        try {
            String url = movie.getVideoUrl();
            Document doc = null;
            Log.d(TAG, "fetchToWatchLocally 1-run: " + url);
            if (!url.contains("/link")) {
                Log.d(TAG, "fetchToWatchLocally: go page doesn't contain /link/ to akwam download page. url: " + url);
                //return;
            }

            //Log.d(TAG, "fetchToWatchLocally run-2: " + url);
            doc = Jsoup.connect(url)
                    .cookies(getMapCookies())
                    .headers(headers)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .get();

            String oldUrl = url;
            String regex = "(?:a[kwamoc])?.*/[download]{1,6}";

            Pattern pattern = Pattern.compile(regex);

            Elements links = doc.getElementsByClass("download-link");
            for (int i = 0; i < links.size(); i++) {
                Element link = links.get(i);
                String pLink = link.attr("href");
                Matcher matcher = pattern.matcher(pLink);
                if (matcher.find()) {
                    //Log.d(TAG, "fetchToWatchLocally 2-run: matching1 " + pLink);
                    url = pLink;
                    break;
                }
            }
            if (oldUrl.equals(url)) {
                links = doc.getElementsByTag("a");
                //Log.d(TAG, "fetchToWatchLocally run-3: old-url:" + oldUrl);
                for (int i = 0; i < links.size(); i++) {
                    Element link = links.get(i);
                    String pLink = link.attr("href");
                    Matcher matcher = pattern.matcher(pLink);
                    if (matcher.find()) {
                        Log.d(TAG, "fetchToWatchLocally 2-run: matching2 " + pLink);
                        url = pLink;
                        break;
                    }
                }
            }

            Log.d(TAG, "fetchToWatchLocally run-4: " + url);
            //####
            try {
                Document  doc2 = Jsoup.connect(url)
                        .cookies(getMapCookies())
                        .headers(headers)
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .timeout(0)
                        .get();

                //check if security caption
                Elements divs = doc2.getElementsByClass("btn-loader");
                Element form = doc2.getElementById("form");

                Elements hs = doc2.getElementsByTag("h1");

                boolean isCheck = divs.size() == 0;
                Log.d("isCheck", "size:" + isCheck);

                if (!isCheck) {
                    String videoCaption = "akwam.download";
                    String videoCaption2 = "akwam.link";
                    String videoCaption3 = "/download/";
                    for (Element div : divs) {
                        Elements links2 = div.getElementsByAttribute("href");
                        for (int i = 0; i < links2.size(); i++) {
                            Element link = links2.get(i);
                            String pLink = link.attr("href");
                            Matcher matcher = pattern.matcher(pLink);
                            if (matcher.find()) {
                                Log.i("akwam url3", pLink);
                                url = pLink;
                            }
                        }
                    }
                    Log.i(TAG, "FetchOneLink url3:" + url);

                    resolution.setVideoUrl(url);//####
                    resolution.setState(Movie.VIDEO_STATE);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(resolution);
                }
                else {
                    Movie newMovie = Movie.clone(movie);
                    newMovie.setVideoUrl(url);
                    Log.d(TAG, "fetchResolutions: ischeck + url:" + url);
                    // Log.d(TAG, "fetchResolutions: ischeck + url:"+url + "body:"+ doc2.body());
                    startWebForResultActivity(newMovie);
                    return null; //to do nothing till activity result returned to activity
                }
            } catch (IOException e) {
            //builder.append("Error : ").append(e.getMessage()).append("\n");
            Log.i(TAG, "error: FetchVideoLink: " + e.getMessage() + "");
        }

        } catch (IOException e) {
            //builder.append("Error : ").append(e.getMessage()).append("\n");
            Log.i(TAG, "FetchVideoLink: " + e.getMessage() + "");
        }
        return resolution;
    }

    /**
     * exactly same as fetchResolutions() fetch movie resolutions and start Exoplayer video intent
     *
     * @param movie
     * @return
     */
    @Override
    public Movie fetchToWatchLocally(final Movie movie) {
        Log.d(TAG, "fetchToWatchLocally: " + movie.getVideoUrl());
        if (movie.getState() == Movie.VIDEO_STATE) {
            return movie;
        }
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);

        return fetchResolutions(clonedMovie); // to do nothing till result returned to the fragment/activity
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
//        AkwamController.START_BROWSER_CODE = true;
//
//        WebView simpleWebView = activity.findViewById(R.id.webView);
//        simpleWebView.clearCache(true);
//        simpleWebView.clearFormData();
//        simpleWebView.clearHistory();
//
//        simpleWebView.setWebViewClient(new Browser_Home() {
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                //Log.d("WEBCLIENT", "OnreDirect url:" + url);
//                if (url.equals(link)) {
//                    AkwamController.START_BROWSER_CODE = true;
//                }
//                return !url.contains("akwam.");
//            }
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                super.onPageFinished(view, url);
//                //Log.d("WEBCLIENT", "onPageFinished");
//                if (AkwamController.START_BROWSER_CODE) {
//                    view.evaluateJavascript("(function() { var x = document.getElementsByClassName(\"link btn btn-light\")[0]; return x.getAttribute(\"href\").toString();})();", new ValueCallback<String>() {
//                        @Override
//                        public void onReceiveValue(String s) {
//                            //Log.d("LogName", s); // Prints the string 'null' NOT Java null
//                            if (s.contains(".download")) {
//                                Intent intent = new Intent(Intent.ACTION_VIEW);
//                                String type = "video/*"; // It works for all video application
//                                String link = s.replace("\"", "");
//                                Uri uri = Uri.parse(link);
//                                intent.setDataAndType(uri, type);
//                                try {
//                                    activity.startActivity(intent);
//                                } catch (ActivityNotFoundException e) {
//                                    //Log.d("errorr", e.getMessage());
//                                }
//                                AkwamController.START_BROWSER_CODE = false;
//                                activity.finish();
//                            }
//                        }
//                    });
//
//                }
//            }
//
//            @Override
//            public void onLoadResource(WebView view, String url) {
//                super.onLoadResource(view, url);
//                //Log.d("WEBCLIENT", "onLoadResource");
//
//
//            }
//        });
//        simpleWebView.setWebChromeClient(new ChromeClient());
//        WebSettings webSettings = simpleWebView.getSettings();
//
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setAllowFileAccess(true);
//        //webSettings.setAppCacheEnabled(true);
//
//        simpleWebView.loadUrl(link);
    }

    @Override
    public void startBrowser(String url) {
        ////Log.i(TAG, "startBrowser: " + url);
        if (url.contains("yout")) {
            url = fixTrailerUrl(url);
        }
        WebView webView = activity.findViewById(R.id.webView);

        webView.loadData("<html><body><iframe width=\"100%\" height=\"100%\" src=\"" + url + "\" frameborder=\"0\" allowfullscreen></iframe></body></html>", "text/html", "utf-8");
    }

    @Override
    public Movie fetchCookie(Movie movie) {
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
    public void setCookies(String cookies) {
        this.cookies = cookies;
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
    public String getCookies() {
        return this.cookies;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    public String fixTrailerUrl(String url) {
        ////Log.i(TAG, "browseTrailer: " + url);
        String newUrl = url;
        if (url.contains("v=")) {
            newUrl = "https://www.youtube.com/embed/" +
                    url.substring(url.indexOf("v=") + 2)
                    + "?autoplay=1&fs=1";
            //Log.d(TAG, "browseTrailer: newUrl=" + newUrl);
        }
//        Movie movie = new Movie();
//        movie.setVideoUrl(newUrl);
//        movie.setStudio(Movie.SERVER_AKWAM);
//        movie.setState(Movie.BROWSER_STATE);
//
//        Intent intent = new Intent(activity, BrowserActivity.class); //start a browser to fetch item
//        intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//        activity.startActivity(intent);

        //      WebView simpleWebView = activity.findViewById(R.id.webView);

//        simpleWebView.clearCache(true);
//        simpleWebView.clearFormData();
//        simpleWebView.clearHistory();
//
//
//        simpleWebView.setWebViewClient(new Browser_Home() {
//            // !url.contains("youtube") || !url.contains(WEBSITE_NAME);
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                super.onPageFinished(view, url);
//                //Log.d("WEBCLIENT", "onPageFinished");
//            }
//
//            @Override
//            public void onLoadResource(final WebView view, String url) {
//                //Log.d("WEBCLIENT", "onLoadResource :url" + url);
//                super.onLoadResource(view, url);
//            }
//        });
//        simpleWebView.setWebChromeClient(new ChromeClient());
//        WebSettings webSettings = simpleWebView.getSettings();
//
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setAllowFileAccess(true);
//        webSettings.setAppCacheEnabled(true);
//        webSettings.setDomStorageEnabled(true);
//        webSettings.setLoadsImagesAutomatically(true);
//        webSettings.setBlockNetworkImage(false);
//
//        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//        webSettings.setPluginState(WebSettings.PluginState.ON);
//        webSettings.setMediaPlaybackRequiresUserGesture(false);
//
//
//        simpleWebView.loadUrl(newUrl);
        return newUrl;
    }

    @Override
    public boolean isSeries(Movie movie) {
        String u = movie.getVideoUrl();
        return u.contains("/series") || u.contains("/movies");
    }

    private class Browser_Home extends WebViewClient {
        Browser_Home() {
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            super.shouldOverrideUrlLoading(view, url);
            ////Log.i("override", "url: " + url);
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

    @Override
    public boolean onLoadResource(Activity activity, WebView view, String url, Movie movie) {
        return false;
    }

    @Override
    public void fetchWebResult(Movie movie) {
    }

    @Override
    public int detectMovieState(Movie movie) {
        return Movie.GROUP_STATE;
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
        int state = movie.getState();
        String script = "";
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_STARTED){
            if (state == Movie.RESOLUTION_STATE){
                Log.d(TAG, "getScript:SERVER_AKWAM WEB_VIEW_MODE_ON_PAGE_STARTED RESOLUTION_STATE");
                script = "if(document != null){" +
                        "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        "var postList = [];" +
                        "var seasons = document.querySelectorAll('.btn-loader');" +
                        "if (seasons.length > 0){" +
                        "var aElem = seasons[0].getElementsByTagName('a');" +
                        "if(aElem.length > 0){" +
                        "    var post = {};" +
                        "    post.videoUrl = aElem[0].getAttribute('href');" +
                        "    post.rowIndex = '" + movie.getRowIndex()+"';"+ //very important
                        "    post.title = '" + movie.getTitle()+"';"+
                        "    post.fetch = '" + movie.getFetch()+"';"+
                        "    post.cardImageUrl = '" + movie.getCardImageUrl()+"';"+
                        "    post.bgImageUrl = '" + movie.getBgImageUrl()+"';"+
                        "    post.description = '" + movie.getDescription()+"';"+
                        "    post.state = '" + Movie.VIDEO_STATE+"';"+
                        "    post.studio = '" + movie.getStudio()+"';"+
                        "    postList.push(post);" +
                        "}" +
                        "MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                        "}" +
                        "}); }";
            }
        }
//        else if (mode == BrowserActivity.WEB_VIEW_MODE_ON_LOAD_RESOURCES) {
//            script = "var element = document.querySelector('.recaptcha-checkbox-border');\n" +
//                    "if (element) {\n" +
//                    "  element.scrollIntoView();\n" +
//                    "}\n";
//        }
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




    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies() {
        return search(config.url + "/recent");
    }

    @Override
    public String getLabel() {
        return "أكوام";
    }
}
