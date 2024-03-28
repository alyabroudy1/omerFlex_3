package com.omerflex.server;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * from SearchActivity or MainActivity -> item -> resolutions
 * Or -> GroupOfItem -> item -> resolutions
 * -> if security check -> web browser intent
 * -> else to video intent
 * group + item -> resolution
 */
public class ArabSeedServer extends AbstractServer {

    ServerConfig config;
    static String TAG = "arabseed";
    public static String WEBSITE_URL = "https://arabseed.show";
    Activity activity;
    static boolean START_BROWSER_CODE = false;
    private static ArabSeedServer instance;
    private String cookies;
    private String referer;
    private Map<String, String> headers;
    Fragment fragment;

    private ArabSeedServer(Fragment fragment, Activity activity) {
        // Private constructor to prevent instantiation
        this.activity = activity;
        this.fragment = fragment;
        headers = new HashMap<>();
    }

    public static synchronized ArabSeedServer getInstance(Fragment fragment, Activity activity) {
        if (instance == null) {
            instance = new ArabSeedServer(fragment, activity);
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
        String url = WEBSITE_URL + "/find/?find=" + query;
        ArrayList<Movie> movieList = new ArrayList<>();
        if (!query.contains("http")) {
            if (referer != null && !referer.isEmpty()) {
                if (referer.endsWith("/")) {
                    query = referer + "find/?find=" + query;
                } else {
                    query = referer + "/find/?find=" + query;
                }
            } else {
                query = WEBSITE_URL + "/find/?find=" + query;
            }
        }
        Log.d(TAG, "search: " + query);
        try {

            Document doc = Jsoup.connect(query)
                    .cookies(getMapCookies())
                    .headers(headers)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .get();
            Log.d(TAG, "result stop title: " + doc.title());

            if (!doc.title().contains("moment")) {

                //Elements links = doc.select("a[href]");
                Elements lis = doc.getElementsByClass("MovieBlock");
                if (lis.size() == 0) {
                    lis = doc.getElementsByClass("postDiv");
                }
                Log.i(TAG, "arab element found: " + lis.size());
                for (Element li : lis) {

                    Movie a = new Movie();
                    a.setStudio(Movie.SERVER_FASELHD);
                    Element videoUrlElem = li.getElementsByAttribute("href").first();
                    if (videoUrlElem != null) {
                        String videoUrl = videoUrlElem.attr("href");


                        Element imageElem = li.getElementsByAttribute("data-src").first();
                        String image = "";
                        String title = "";
                        if (imageElem == null) {
                            continue;
                        }
                            image = imageElem.attr("data-src");
                            title = imageElem.attr("alt");
                            if (title == null || title.contains("تحميل") || title.equals("")){
                                continue;
                            }


                        String category = "";
                        Elements cateElems = videoUrlElem.getElementsByClass("category");
                        for (Element catElem : cateElems) {
                            category = catElem.text();
                            break;
                        }
                        if (
                                category.contains("اغاني") ||
                                category.contains("كمبيوتر") ||
                                category.contains("موبايلات")
                        ){
                            continue;
                        }
//                        String rate = "";
//                        Elements spans = li.getElementsByTag("span");
//                        for (Element span : spans) {
//                            if (!span.hasAttr("class")) {
//                                rate = span.text();
//                                break;
//                            }
//                        }
//                        if (rate.equals("")) {
//                            Element rateElem = li.getElementsByClass("pImdb").first();
//                            if (rateElem != null){
//                                rate = rateElem.text();
//                            }
//                        }

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
                        m.setStudio(Movie.SERVER_ARAB_SEED);
                        m.setVideoUrl(videoUrl);
                        m.setCardImageUrl(image);
                        m.setBackgroundImageUrl(image);
                        m.setBgImageUrl(image);
                        m.setState(a.getState());
                        m.setRate("");
                        m.setSearchContext(searchContext);
                        m.setCreatedAt(Calendar.getInstance().getTime().toString());
                        m.setMainMovie(m);
                        movieList.add(m);
//                       Log.d(TAG, "search: movie: " + m);
                    }
                    //hhhhhhhhhhhhhhhhh
//                    break;
                }

                if (lis.size() > 0) {
                    //nextpage
                    Elements footerUls = doc.getElementsByClass("next page-numbers");
                    Log.d(TAG, "search: footerUls:" + footerUls.size());
                    for (Element ul : footerUls) {
                        String nextPageLink = config.url + ul.attr("href");

                        Movie nextPage = new Movie();
                        nextPage.setTitle("التالي");
                        nextPage.setDescription("0");
                        nextPage.setStudio(Movie.SERVER_ARAB_SEED);
                        nextPage.setVideoUrl(nextPageLink);
                        nextPage.setCardImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                        nextPage.setBackgroundImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                        nextPage.setState(Movie.NEXT_PAGE_STATE);
                        nextPage.setRate("");
                        nextPage.setSearchContext(searchContext);
                        nextPage.setMainMovie(nextPage);
                        nextPage.setMainMovieTitle(nextPageLink);
                        Log.d(TAG, "search: nextPage:" + nextPageLink);
                        movieList.add(nextPage);
                        break;
                    }
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
                m.setStudio(Movie.SERVER_ARAB_SEED);
                m.setVideoUrl(query);
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

        } catch (
                IOException e) {
            e.printStackTrace();
            Log.d(TAG, "search: error: " + e.getMessage());
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

//    @Override
//    public Movie fetch(Movie movie) {
//        Log.i(TAG, "fetch: " + movie.getVideoUrl());
//        switch (movie.getState()) {
//            case Movie.GROUP_STATE:
//                Log.i(TAG, "onItemClick. GROUP_STATE" + movie.getStudio() + ". url:" + movie.getVideoUrl());
//                return fetchGroup(movie);
//            case Movie.ITEM_STATE:
//                Log.i(TAG, "onItemClick. ITEM_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
//                return fetchItem(movie);
////                return startWebForResultActivity(movie);
//            case Movie.RESOLUTION_STATE:
//                Log.i(TAG, "onItemClick. RESOLUTION_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
//                return fetchResolutions(movie);
//            case Movie.BROWSER_STATE:
//                Log.i(TAG, "onItemClick. BROWSER_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
//                startBrowser(movie.getVideoUrl());
//                break;
//            case Movie.VIDEO_STATE:
//                Log.i(TAG, "onItemClick. VIDEO_STATE " + movie.getStudio() + ". url:" + movie.getVideoUrl());
//                startVideo(movie.getVideoUrl());
//                break;
//            default:
//                return fetchResolutions(movie);
//        }
//        return movie;
//    }

    @Override
    public Movie fetchBrowseItem(Movie movie) {
        return null;
    }

    @Override
    public int fetchNextAction(Movie movie) {
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY;
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
    }

    @Override
    public Movie fetchGroupOfGroup(Movie movie) {
        Log.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());
        return movie;
    }

    @Override
    public Movie fetchGroup(final Movie movie) {
        Log.i(TAG, "fetchGroup: " + movie.getVideoUrl());
        final String url = movie.getVideoUrl();
       /* Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

        */
        //     Movie series = dbHelper.findMovieByLink(movie.getVideoUrl());
        //   movie.setSubList(dbHelper.findSubListByMainMovieLink(movie.getStudio(), movie.getVideoUrl()));

        //  if (movie.getSubList().size() == 0) {
        //  if (series == null) {
        Log.d(TAG, "fetchGroup: source network");
        try {
            Log.i(TAG, "FetchSeriesLink url:" + url);
            //descriptionTextView = activity.findViewById(R.id.textViewDesc);

            Document doc = Jsoup.connect(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .ignoreContentType(true)
                    .get();

            //  Document doc = Jsoup.connect(url).header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8").header("User-Agent", "Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36").timeout(0).get();
            //description

            Elements decDivs = doc.select("h2");
            String description = "";
            for (Element div : decDivs) {
                String desc = div.getElementsByTag("p").html();
                description = desc;
                Log.i("description", "found:" + description);

                if (null != description && !description.equals("")) {
                    break;
                }
            }

            if (!description.equals("")) {
                Log.d(TAG, "fetchGroup: desooo" + description);
                movie.setDescription(description);
            }
            //hier             VideoDetailsFragment.MOVIE_DESCRIPTION = description;
            // VideoDetailsFragment.updateDescription(description);
            //      movie.setDescription(description);
                 /*   activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            descriptionPresenter.updateDescription(desc+" ");
                        }
                    });

                  */


            //backgroundImage and trailer
            Elements imageDivs = doc.getElementsByClass("row py-4");

            String bgImage = "";
            String ytLink = "";
            for (Element imageDiv : imageDivs) {
                Elements imageLinks = imageDiv.getElementsByAttribute("href");
                for (Element imagelink : imageLinks) {
                    Log.d(TAG, "run: bgimage divs : " + imagelink.attr("href"));
                    if (imagelink.attr("href").contains("/uploads/")) {
                        bgImage = imagelink.attr("href");
                        Log.d(TAG, "run: bgimage found : " + bgImage);
                        //break;
                    }
                    if (imagelink.attr("href").contains("youtube")) {
                        ytLink = imagelink.attr("href");
                        Log.d(TAG, "run: youtube found : " + ytLink);
                        break;
                    }
                }
                if (!bgImage.equals("")) {
                    break;
                }
            }
            //hier   VideoDetailsFragment.MOVIE_BACKGROUND_IMAGE = bgImage;
            //hier VideoDetailsFragment.MOVIE_TRAILER_URL = ytLink;
            movie.setBackgroundImageUrl(bgImage);
            movie.setTrailerUrl(ytLink);

            Elements links = doc.select("a");
            for (Element link : links) {
                // TODO: find better way to get the link
                if (
                        link.attr("href").contains("/episode") &&
                                link.getElementsByAttribute("src").hasAttr("alt")
                ) {
                    Movie episode = new Movie();
                    Log.i(TAG, "linkFound:" + link.attr("href") + "");


                    String title = link.getElementsByAttribute("src").attr("alt");
                    String cardImageUrl = link.getElementsByAttribute("src").attr("src");
                    String backgroundImageUrl = bgImage;
                    String rate = movie.getRate();

                    String studio = Movie.SERVER_AKWAM;
                    String videoUrl = link.attr("href");

                    episode.setTitle(title);
                    episode.setDescription(description);
                    episode.setStudio(studio);
                    episode.setVideoUrl(videoUrl);
                    episode.setCardImageUrl(cardImageUrl);
                    episode.setBackgroundImageUrl(backgroundImageUrl);
                    episode.setState(Movie.ITEM_STATE);
                    episode.setRate(rate);
                    episode.setMainMovieTitle(movie.getMainMovieTitle());
                    episode.setMainMovie(movie);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(episode);
                    //    episode.save(dbHelper);
                    // movie.addSeries(m);
                       /* Episode finalEpisode = episode;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listRowAdapter.add(finalEpisode);
                            }
                        });
                        */
                }
            }

        } catch (IOException e) {
            Log.i(TAG + "failed", e.getMessage() + "");
        }
    /*        movie.save(dbHelper);
        } else {
            Log.d(TAG, "fetchGroup: source db");
        }

     */
            /*}
        });

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

             */

        return movie;
    }

    @Override
    public Movie fetchItem(final Movie movie) {
        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());
        Movie m = null;
        Log.d(TAG, "fetchItem: source Network");

        try {

            String url = movie.getVideoUrl();
            Document doc = Jsoup.connect(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .ignoreContentType(true)
                    .get();

            Elements storyElems = doc.getElementsByClass("StoryLine");
            String dec = "";
            for (Element storyElem : storyElems) {
                Elements pElems = storyElem.getElementsByClass("descrip");
                for (Element pElem : pElems) {
                    if (pElem.hasAttr("style")) {
                        continue;
                    }
                    dec = pElem.text();
                    break;
                }
                break;
            }
            Elements watchElems = doc.getElementsByClass("WatchButtons");
            String link = "";
            Log.d(TAG, "fetchItem: watchElems:"+watchElems.size());
            for (Element watchElem : watchElems) {
                Elements linkElems = watchElem.getElementsByTag("a");
                for (Element linkElem : linkElems) {
                    link = linkElem.attr("href");
                    String domain = extractDomain(movie.getVideoUrl());
                    movie.setDescription(dec);
                    if (link != null) {
                        Movie resolution = Movie.clone(movie);
                        resolution.setVideoUrl(link);
                        Log.d(TAG, "fetchItem: link:" + link);
                        resolution.setState(Movie.BROWSER_STATE);

                        return fetchServers(resolution, domain);
                    }
                    break;
                }
                break;
            }
        } catch (IOException e) {
            Log.i(TAG, "error:" + e.getMessage());
        }

        return movie;
    }

    private Movie fetchServers(Movie movie, String referer) {
        Document doc = null;
        Log.d(TAG, "fetchServers run-1: " + movie.getVideoUrl());
        try {
            doc = Jsoup.connect(movie.getVideoUrl())
                    .header("referer", referer)
                    .userAgent("Android 8")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .timeout(0)
                    .ignoreContentType(true)
                    .get();

            Elements serverElems = doc.getElementsByClass("containerServers");

            if (serverElems.size() == 0){
                Movie clonedMovie = Movie.clone(movie);
                clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_LIST);
//                String userAgent = "Mozilla/5.0 (Linux; Android 9; SM-G960F Build/PPR1.180610.011) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Mobile Safari/537.36";
                String userAgent = "Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Mobile Safari/537.36";
                String newUrl = movie.getVideoUrl()+"||referer="+referer+"&User-Agent=" + userAgent;
                clonedMovie.setVideoUrl(newUrl);
                startWebForResultActivity(clonedMovie);
                return movie;//very important to return the original movie
            }
            Log.d(TAG, "fetchServers: serverElems:"+serverElems.size());
            for (Element serverElem : serverElems){
                Elements listElems = serverElem.getElementsByAttribute("data-link");
                int counter = 0;
                for (Element listElem : listElems){
                    Movie server = Movie.clone(movie);
                    server.setState(Movie.RESOLUTION_STATE);
//                    String link = movie.getVideoUrl() + "??"+(counter++)+"||referer="+ referer;
                    String link = listElem.attr("data-link");
                    if (link != null){
//                        if (link.contains(".wiki")){
//                            continue;
//                        }

                        link = link + "||Referer=" + extractDomain(movie.getVideoUrl());
//                        "&Accept-Encoding=\"gzip, deflate, br\""+
//                        "&Accept=\"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\""+
//                        "&Accept-Language=\"de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7\"";
//                        link = link + "||referer=https://arabseed.show/";
                        server.setVideoUrl(link);
                    }
                    Elements titleElems = listElem.getElementsByTag("span");
                    if (titleElems.size() > 0){
                        if (titleElems.first() != null){
                            String title = titleElems.first().text();

                            server.setTitle(title);
                        }
                    }else {
                        server.setTitle(listElem.text());
                    }
                    Log.d(TAG, "fetchServers: servers: "+server);
                    if (movie.getSubList() == null){
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(server);
                }

                break;
            }
        } catch (IOException e) {
            //builder.append("Error : ").append(e.getMessage()).append("\n");
            Log.i(TAG, "FetchVideoLink: " + e.getMessage() + "");
        }
        return movie;
    }

    private String extractDomain(String videoUrl) {
        String fullDomain = "";
        try {
            URL url = new URL(videoUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            fullDomain = protocol + "://" + host + "/";
        } catch (Exception e) {

        }
        Log.d(TAG, "extractDomain: " + fullDomain);
        return fullDomain;
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
    public Movie fetchResolutions(Movie movie) {
        Log.i(TAG, "fetchResolutions: " + movie.getVideoUrl());

        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
        startWebForResultActivity(clonedMovie);
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

         startWebForResultActivity(clonedMovie);
         return null;
    }

    @Override
    public void startVideo(final String link) {
        Log.i(TAG, "startVideo: " + link);
        ArabSeedServer.START_BROWSER_CODE = true;

        WebView simpleWebView = activity.findViewById(R.id.webView);
        simpleWebView.clearCache(true);
        simpleWebView.clearFormData();
        simpleWebView.clearHistory();

        simpleWebView.setWebViewClient(new Browser_Home() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("WEBCLIENT", "OnreDirect url:" + url);
                if (url.equals(link)) {
                    ArabSeedServer.START_BROWSER_CODE = true;
                }
                return !url.contains("akwam.");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WEBCLIENT", "onPageFinished");
                if (ArabSeedServer.START_BROWSER_CODE) {
                    view.evaluateJavascript("(function() { var x = document.getElementsByClassName(\"link btn btn-light\")[0]; return x.getAttribute(\"href\").toString();})();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            Log.d("LogName", s); // Prints the string 'null' NOT Java null
                            if (s.contains(".download")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                String type = "video/*"; // It works for all video application
                                String link = s.replace("\"", "");
                                Uri uri = Uri.parse(link);
                                intent.setDataAndType(uri, type);
                                try {
                                    activity.startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Log.d("errorr", e.getMessage());
                                }
                                ArabSeedServer.START_BROWSER_CODE = false;
                                activity.finish();
                            }
                        }
                    });

                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                Log.d("WEBCLIENT", "onLoadResource");


            }
        });
        simpleWebView.setWebChromeClient(new ChromeClient());
        WebSettings webSettings = simpleWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        //webSettings.setAppCacheEnabled(true);

        simpleWebView.loadUrl(link);
    }

    @Override
    public void startBrowser(String url) {
        Log.i(TAG, "startBrowser: " + url);
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

    @Override
    public void setCookies(String cookies) {

    }

    @Override
    public String getCookies() {
        return null;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {

    }

    @Override
    public Map<String, String> getHeaders() {
        return null;
    }

    public String fixTrailerUrl(String url) {
        Log.i(TAG, "browseTrailer: " + url);
        String newUrl = url;
        if (url.contains("v=")) {
            newUrl = "https://www.youtube.com/embed/" +
                    url.substring(url.indexOf("v=") + 2)
                    + "?autoplay=1&fs=1";
            Log.d(TAG, "browseTrailer: newUrl=" + newUrl);
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
//                Log.d("WEBCLIENT", "onPageFinished");
//            }
//
//            @Override
//            public void onLoadResource(final WebView view, String url) {
//                Log.d("WEBCLIENT", "onLoadResource :url" + url);
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

    @Override
    public boolean onLoadResource(Activity activity, WebView view, String url, Movie movie) {
        return false;
    }

    @Override
    public void fetchWebResult(Movie movie) {
    }

    @Override
    public int detectMovieState(Movie movie) {
        return Movie.ITEM_STATE;
    }

    private Movie startWebForResultActivity(Movie movie) {
        Intent browse = new Intent(activity, BrowserActivity.class);
        browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
        browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
        //   Log.d(TAG, "getResultFromWeb: activity:" + fragment.getClass().getName());
        //activity.startActivity(browse);
        fragment.startActivityForResult(browse, movie.getFetch());

        return movie;
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
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_STARTED) {
            if (movie.getState() == Movie.BROWSER_STATE) {
                Log.d(TAG, "getScript:cimaClub RESOLUTION_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                            "var serverElems = document.getElementsByClassName('containerServers');\n" +
                        "var postList = [];\n" +
                        "console.log(serverElems.length);" +
                        "if(serverElems.length > 0){\n" +
                        " var serverElem = serverElems[0];\n" +
                        "var listElems = serverElem.querySelectorAll('[data-link]');\n" +
                        "\n" +
                        "if(listElems.length > 0){\n" +
                        "for (let b = 0; b < listElems.length; b++) {\n" +
                        "    var listElem = listElems[b];\n" +
                        "        // Clone 'movie' object\n" +
                        "        var post = {};\n" +
                        "        post.state = 3;\n" +
                        "        post.studio = \""+movie.getStudio()+"\";\n" +
                        "        post.fetch = \""+movie.getFetch()+"\";\n" +
                        "        post.cardImageUrl = \""+movie.getStudio()+"\";\n" +
                        "        post.backgroundImageUrl = \""+movie.getBackgroundImageUrl()+"\";\n" +
                        "        post.cardImageUrl = \""+movie.getCardImageUrl()+"\";\n" +
                        "        post.getMainMovieTitle = \""+movie.getMainMovieTitle()+"\";\n" +
                        "        var link = listElem.getAttribute('data-link');\n" +
                        "\n" +
                        "        // Modify 'link' if it exists\n" +
                        "        if (link) {\n" +
                        "            post.videoUrl = link;\n" +
                        "        }\n" +
                        "\n" +
                        "var titleElems = listElem.getElementsByTagName(\"span\");\n" +
                        "                    if (titleElems.length > 0){\n" +
                        "                 \n" +
                        "                            var title = titleElems[0].textContent;\n" +
                        "if (title.includes(\"عرب سيد\")) {\n" +
                        "        post.videoUrl = \""+movie.getVideoUrl()+"\"; \n" +
                        "    }\n" +
                        "                            post.title = title;\n" +
                        "                        \n" +
                        "                    }else {\n" +
                        "                        post.title = listElem.textContent;\n" +
                        "                    }\n" +
                        "\n" +
                        "\n" +
                        "        // Log the server object\n" +
                        "        console.log(\"fetchServers: servers: \", post.videoUrl);\n" +
                        "\t\t\t\tpostList.push(post);\n" +
                        "    }\n" +
                        "\n" +
//                        "postList;\n" +
                        "MyJavaScriptInterface.myMethod(JSON.stringify(postList));\n" +
                        "}\n" +
                        "}"+
                        "});";



//                String serverId = "#";
//                String clickServer = "";
//                if (movie.getVideoUrl().contains("??")) {
//                    // newUrl = movie.getVideoUrl().substring(0, movie.getVideoUrl().indexOf("?"));
//                    int idIndex = movie.getVideoUrl().indexOf("??") + 2;
//                    serverId = movie.getVideoUrl().substring(idIndex, idIndex + 1);
//                    clickServer = "var intendedValue = '" + serverId + "';\n" +
//                            " var serversWatchSideElement = document.querySelector('.containerServers');\n" +
//                            "var elementList = serversWatchSideElement.querySelectorAll('[data-link]');" +
//                            "elementList['"+serverId+"'].click();";
//                    Log.d(TAG, "getScript: serverID:" + serverId);
//                }
//                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
//                        clickServer +
//                        "var iframe = document.querySelector('iframe[src]:not([src=\"about:blank\"])');" +
//                        "  // Check if the iframe element was found" +
//                        "  if (iframe) {" +
//                        "    iframe.scrollIntoView({behavior: 'smooth'});" +
//                        "  } " +
//                        "// Find the first iframe with src attribute not equal to \"about:blank\"\n" +
//                        "var originalIframe = document.querySelector('iframe[src]:not([src=\"about:blank\"])');\n" +
//                        "\n" +
////                        "if (originalIframe) {\n" +
////                        "  // Clone the iframe\n" +
////                        "  var clonedIframe = originalIframe.cloneNode(true);\n" +
////                        "\n" +
////                        "  // Remove all elements from the body\n" +
////                        "  document.body.innerHTML = '';\n" +
////                        "\n" +
////                        "  // Apply CSS to make the cloned iframe take up the entire screen\n" +
////                        "  clonedIframe.style.position = 'fixed';\n" +
////                        "  clonedIframe.style.top = '0';\n" +
////                        "  clonedIframe.style.left = '0';\n" +
////                        "  clonedIframe.style.width = '100%';\n" +
////                        "  clonedIframe.style.height = '100%';\n" +
////                        "\n" +
////                        "  // Add the cloned iframe back to the page\n" +
////                        "  document.body.appendChild(clonedIframe);\n" +
////                        "\n" +
//                        "  // Request fullscreen for the cloned iframe\n" +
//                        "  if (originalIframe.requestFullscreen) {\n" +
//                        "    originalIframe.requestFullscreen();\n" +
//                        "  } else if (originalIframe.mozRequestFullScreen) {\n" +
//                        "    originalIframe.mozRequestFullScreen();\n" +
//                        "  } else if (originalIframe.webkitRequestFullscreen) {\n" +
//                        "    originalIframe.webkitRequestFullscreen();\n" +
//                        "  } else if (originalIframe.msRequestFullscreen) {\n" +
//                        "    originalIframe.msRequestFullscreen();\n" +
//                        "  }\n" +
//                        "\n" +
//                        "  // Disable the ability to create new elements\n" +
//                        "  document.addEventListener('DOMNodeInserted', function (e) {\n" +
//                        "    e.preventDefault();\n" +
//                        "    e.stopPropagation();\n" +
//                        "    return false;\n" +
//                        "  });\n" +
//                        "} else {\n" +
//                        "  // Handle the case when no iframe with src other than \"about:blank\" is found\n" +
//                        "  console.error('No eligible iframe found.');\n" +
//                        "}\n" +
//                        "});";
            }
        }
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
        return search(
//                config.url +"/category/netfilx/");
                config.url +"/latest1/");
    }

    @Override
    public String getLabel() {
        return "عرب سيد";
    }
}
