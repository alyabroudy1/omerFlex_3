package com.omerflex.server;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * from SearchActivity or MainActivity -> item -> resolutions
 * Or -> GroupOfItem -> item -> resolutions
 * -> if security check -> web browser intent
 * -> else to video intent
 * group + item -> resolution
 */
public class ArabSeedServer extends AbstractServer {

    static String TAG = "arabseed";
    public static String WEBSITE_URL = "https://arabseed.show";

    static boolean START_BROWSER_CODE = false;

    public ArabSeedServer() {
    }

    /**
     * produce movie from search result if isSeries than Group_State else Item_state
     *
     * @param query name to search for
     * @return
     */
    @Override
    public ArrayList<Movie> search(String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Log.i(TAG, "search: " + query);
        String searchContext = query;
//        String url = WEBSITE_URL + "/find/?find=" + query;
        ArrayList<Movie> movieList = new ArrayList<>();


        String url = query;
        if (!query.contains("http")) {
            url = this.getSearchUrl(query);
        }
        Log.i(getLabel(), "search: " + url);

        Document doc = this.getSearchRequestDoc(url);
        if (doc == null) {
            activityCallback.onInvalidLink("Invalid link");
            return null;
        }

        Log.d(TAG, "result stop title: " + doc.title());
        if (doc.title().contains("moment")) {
//        if (true) {
//            setCookieRefreshed(false);

            //we need to update the url as domain might be updated in getSearchRequestDoc
//            Log.d(TAG, "search: new Url: "+ doc.baseUri() + ", location:"+doc.location());

            //**** default
            // String title = "ابحث في موقع فاصل ..";
//            String title = query;
            //int imageResourceId = R.drawable.default_image;
            // String cardImageUrl = "android.resource://" + activity.getPackageName() + "/" + imageResourceId;
            String cardImageUrl = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
            String backgroundImageUrl = "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/bg.jpg";
            Movie m = new Movie();
            m.setTitle(query);
            m.setDescription("نتائج البحث في الاسفل...");
            m.setStudio(Movie.SERVER_ARAB_SEED);
            m.setVideoUrl(doc.location());
            //  m.setVideoUrl("https://www.google.com/");
            m.setState(Movie.COOKIE_STATE);
            // m.setState(Movie.RESULT_STATE);
            m.setCardImageUrl(cardImageUrl);
            m.setBackgroundImageUrl(backgroundImageUrl);
            m.setRate("");
            m.setSearchContext(searchContext);
            m.setCreatedAt(Calendar.getInstance().getTime().toString());
            movieList.add(m);

            activityCallback.onInvalidCookie(movieList, getLabel());
            return movieList;
        }


        //Elements links = doc.select("a[href]");
        Elements lis = doc.getElementsByClass("MovieBlock");
        if (lis.size() == 0) {
            lis = doc.getElementsByClass("postDiv");
        }
        Log.i(TAG, "arab element found: " + lis.size());
        for (Element li : lis) {
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
                if (title == null || title.contains("تحميل") || title.equals("")) {
                    continue;
                }

                    title = title.replace("مسلسل", "");

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
                ) {
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
                Movie m = new Movie();
                m.setTitle(title);
                m.setDescription("");
                m.setStudio(Movie.SERVER_ARAB_SEED);
                m.setVideoUrl(videoUrl);
                m.setCardImageUrl(image);
                m.setBackgroundImageUrl(image);
                m.setBgImageUrl(image);
                m.setState(detectMovieState(m));
                m.setRate("");
                m.setSearchContext(searchContext);
                m.setMainMovie(m);
                m.setMainMovieTitle(m.getVideoUrl());
                movieList.add(m);
//                       Log.d(TAG, "search: movie: " + m);
            }
            //hhhhhhhhhhhhhhhhh
//                    break;
        }

        //nextpage
        Movie nextPage = generateNextPage(doc);

        if (nextPage != null) {
            movieList.add(nextPage);
        }
        activityCallback.onSuccess(movieList, getLabel());
        return movieList;
    }

    private Movie generateNextPage(Document doc) {

        Elements footerUls = doc.getElementsByClass("next page-numbers");
        Log.d(TAG, "search: footerUls:" + footerUls.size());
        if (footerUls.isEmpty()) {
            return null;
        }
        String nextPageLink = "";
        for (Element ul : footerUls) {
            nextPageLink = ul.attr("href");
            if (nextPageLink == null) {
                return null;
            }
            break;
        }
        nextPageLink = getConfig().getUrl() + nextPageLink;
        Movie nextPage = new Movie();
        nextPage.setTitle("التالي");
        nextPage.setDescription("0");
        nextPage.setStudio(Movie.SERVER_ARAB_SEED);
        nextPage.setVideoUrl(nextPageLink);
        nextPage.setCardImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
        nextPage.setBackgroundImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
        nextPage.setState(Movie.NEXT_PAGE_STATE);
        nextPage.setRate("");
        nextPage.setMainMovie(nextPage);
        nextPage.setMainMovieTitle(nextPageLink);
        Log.d(TAG, "search: nextPage:" + nextPageLink);
        return nextPage;
    }

    @Override
    public boolean shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return false;
    }

    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return null;
    }

    @Override
    protected String getSearchUrl(String query) {
        if (query.contains("http")) {
            return query;
        }

        String searchPart = "/find/?find=";
        ServerConfig config = getConfig();
        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            return WEBSITE_URL + searchPart + query;
        }


        if (query.startsWith("/")) {
            return config.getUrl() + query;
        }
        return config.getUrl() + searchPart + query;
    }

    public MovieFetchProcess fetchBrowseItem(Movie movie, ActivityCallback<Movie> activityCallback) {
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
        // to do nothing and wait till result returned to activity only the first fetch
//        return startWebForResultActivity(clonedMovie);
        activityCallback.onInvalidCookie(clonedMovie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, clonedMovie);
    }

    protected MovieFetchProcess fetchSeriesAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        if (action == Movie.GROUP_OF_GROUP_STATE) {
            return fetchGroupOfGroup(movie, activityCallback);
        }
        return fetchGroup(movie, activityCallback);
    }

    protected MovieFetchProcess fetchItemAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
//        Log.d(TAG, "fetchItemAction: 55");
        switch (action) {
            case Movie.BROWSER_STATE:
                return fetchBrowseItem(movie, activityCallback);
            case Movie.COOKIE_STATE:
                return fetchCookie(movie);
            case Movie.ACTION_WATCH_LOCALLY:
                return fetchWatchLocally(movie, activityCallback);
            case Movie.RESOLUTION_STATE:
                return fetchResolutions(movie, activityCallback);
//            case Movie.RESOLUTION_STATE:
//                return fetchResolutions(movie);
            case Movie.VIDEO_STATE:
                activityCallback.onSuccess(movie, getLabel());
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
            default:
                return fetchItem(movie, activityCallback);
        }
    }

    private MovieFetchProcess fetchWatchLocally(Movie movie, ActivityCallback<Movie> activityCallback) {
        if (movie.getState() == Movie.BROWSER_STATE) {
//            Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);
//            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, clonedMovie);
            activityCallback.onInvalidCookie(movie, getLabel());
        }
        if (movie.getState() == Movie.RESOLUTION_STATE) {
            return fetchResolutions(movie, activityCallback);
        }
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_EXOPLAYER, movie);
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
    public int fetchNextAction(Movie movie) {
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY;
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
    }

    private MovieFetchProcess fetchGroupOfGroup(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    private MovieFetchProcess fetchGroup(final Movie movie, ActivityCallback<Movie> activityCallback) {
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

        Log.i(TAG, "FetchSeriesLink url:" + url);
        //descriptionTextView = activity.findViewById(R.id.textViewDesc);

        Document doc = getRequestDoc(url);

        if (doc == null) {
            Log.d(TAG, "fetchGroup: error doc is null ");
            activityCallback.onInvalidLink(movie);
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
        }

        if (doc.title().contains("Just a moment")) {
//            Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
//            return startWebForResultActivity(clonedMovie);
            activityCallback.onInvalidCookie(movie, getLabel());
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
        }


//            Document doc = Jsoup.connect(url)
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
//                    .followRedirects(true)
//                    .ignoreHttpErrors(true)
//                    .timeout(0)
//                    .ignoreContentType(true)
//                    .get();

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
                Movie episode = Movie.clone(movie);
                Log.i(TAG, "linkFound:" + link.attr("href") + "");


                String title = link.getElementsByAttribute("src").attr("alt");
                String cardImageUrl = link.getElementsByAttribute("src").attr("src");

                String videoUrl = link.attr("href");

                episode.setTitle(title);
                episode.setDescription(description);
                episode.setVideoUrl(videoUrl);
                episode.setCardImageUrl(cardImageUrl);
                episode.setState(Movie.ITEM_STATE);
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
        activityCallback.onSuccess(movie, getLabel());
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

        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    private MovieFetchProcess fetchItem(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());
        Movie m = null;
        Log.d(TAG, "fetchItem: source Network");

        String url = movie.getVideoUrl();
        Document doc = getRequestDoc(url);
//            Document doc = Jsoup.connect(url)
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
//                    .followRedirects(true)
//                    .ignoreHttpErrors(true)
//                    .timeout(0)
//                    .ignoreContentType(true)
//                    .get();

        if (doc == null) {
            activityCallback.onInvalidLink(movie);
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
        }

        if (doc.title().contains("Just a moment")) {
//            Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
//            return startWebForResultActivity(clonedMovie);
            activityCallback.onInvalidCookie(movie, getLabel());
        }

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
        Log.d(TAG, "fetchItem: watchElems:" + watchElems.size());
        for (Element watchElem : watchElems) {
            Elements linkElems = watchElem.getElementsByTag("a");
            Log.d(TAG, "fetchItem: watchElems-link: " + linkElems.size());
            String domain = Util.extractDomain(movie.getVideoUrl(), true, true);
            Log.d(TAG, "fetchItem: domain: "+ domain);
            movie.setDescription(dec);
            if (!linkElems.isEmpty()) {
                for (Element linkElem : linkElems) {
                    link = linkElem.attr("href");
                    if (link != null) {
                        Movie resolution = Movie.clone(movie);
                        resolution.setVideoUrl(link);
                        Log.d(TAG, "fetchItem: link:" + link);
                        resolution.setState(Movie.BROWSER_STATE);

                        return fetchServers(null, resolution, domain, activityCallback);
                    }
                    break;
                }
                break;
            }
            //new style
            try {
            // Find the form dynamically (by checking if it contains a hidden input and a button)
            Element targetForm = null;
            Elements forms = watchElem.select("form");
            for (Element form : forms) {
                if (form.selectFirst("input[type='hidden']") != null && form.selectFirst("button") != null) {
                    targetForm = form;
                    break;
                }
            }

            if (targetForm == null) {
                System.out.println("error: Watch form not found.");
                break;
            }

// Extract the POST URL from the form's action attribute
                String postUrl = targetForm.attr("action");
                if (postUrl.isEmpty()) {
                    System.out.println("Post URL not found in form's action attribute.");
                    break;
                }

                Log.d(TAG, "fetchItem: postUrl:"+ postUrl);


                // Find the hidden input
            Element hiddenInput = targetForm.selectFirst("input[type='hidden']");
            if (hiddenInput == null) {
                System.out.println("Error Hidden input not found.");
                break;
            }

            // Extract the hidden input name dynamically
            String hiddenInputName = hiddenInput.attr("name");
            if (hiddenInputName.isEmpty()) {
                System.out.println("Error Hidden input name not found.");
                break;
            }
                Log.d(TAG, "fetchItem: hiddenInputName:"+ hiddenInputName);

            // Extract the hidden input value
            String hiddenInputValue = hiddenInput.val();

                Log.d(TAG, "fetchItem: hiddenInputValue:"+ hiddenInputValue);

            // Prepare the POST data
            Map<String, String> postData = new HashMap<>();
            postData.put(hiddenInputName, hiddenInputValue);

            // Make the POST request
            Connection connection = Jsoup.connect(postUrl)
                    .method(Connection.Method.POST)
                    .data(postData)
                    .followRedirects(true);

                // Add headers
                connection.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                connection.header("Content-Type", "application/x-www-form-urlencoded");
                connection.header("Accept-Language", "en-US,en;q=0.9");
                connection.header("Referer", domain);

                Connection.Response response = connection.execute();

                if (response.statusCode() == 200) {

                    // Print the response status code and body
                    System.out.println("Status Code: " + response.statusCode());
//                    Log.d(TAG, "Response Body: " + response.body());

                    return fetchServers(response.parse(), movie, domain, activityCallback);
                }

        } catch (IOException e) {
                System.out.println("Error fetching watch button: " + e.getMessage());
        }


        }
        activityCallback.onInvalidLink(movie);
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
    }

    private MovieFetchProcess fetchServers(Document doc, Movie movie, String referer, ActivityCallback<Movie> activityCallback) {
//        Log.d(TAG, "fetchServers run-1: " + movie.getVideoUrl());
//        Log.d(TAG, "fetchServers run-1 referer: " + referer);

//        Document doc = getRequestDoc(movie.getVideoUrl());
        try {
            if (doc == null){
                String host = Util.extractDomain(movie.getVideoUrl(), true, true);
                String newCookie = CookieManager.getInstance().getCookie(host);

                doc = Jsoup.connect(movie.getVideoUrl())
//                        .userAgent("Android 8")
                        .userAgent(getCustomUserAgent(movie.getState()))
                        .headers(getConfig().getHeaders())
                        .cookies(getConfig().getMappedCookies())
                        .header("referer", referer)
                        .header("Cookie", newCookie)
                        .followRedirects(false) // in this case dont follow redirect
                        .ignoreHttpErrors(true)
                        .timeout(0)
                        .ignoreContentType(true)
                        .get();
//                Log.d(TAG, "fetchServers: headers "+ getConfig().getHeaders().toString());
//                doc = getRequestDoc(movie.getVideoUrl());
                Log.d(TAG, "fetchServers: new request-1 title:"+ doc.title());
                if (doc.title().contains("Just a moment")) {
                    Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
//            return startWebForResultActivity(clonedMovie);
                    clonedMovie.setVideoUrl(movie.getVideoUrl() + "|Referer="+referer);
                    Log.d(TAG, "fetchServers: new request title:"+ doc.title());
                    activityCallback.onInvalidCookie(clonedMovie, getLabel());

                    return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, clonedMovie);
                }

            }

            Log.d(TAG, "fetchServers: title:"+ doc.title());
            Elements serverElems = doc.getElementsByClass("containerServers");
            Log.d(TAG, "fetchServers:serverElems: "+serverElems.size());
            if (serverElems.isEmpty()) {
                Movie clonedMovie = Movie.clone(movie);
                clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_LIST);
//                String userAgent = "Mozilla/5.0 (Linux; Android 9; SM-G960F Build/PPR1.180610.011) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Mobile Safari/537.36";
                String userAgent = "Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Mobile Safari/537.36";
                String newUrl = movie.getVideoUrl() + "|referer=" + referer + "&User-Agent=" + userAgent;
                clonedMovie.setVideoUrl(newUrl);
//                startWebForResultActivity(clonedMovie);
                activityCallback.onInvalidCookie(clonedMovie, getLabel());
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);//very important to return the original movie
            }
            Log.d(TAG, "fetchServers: serverElems:" + serverElems.size());
            String domain = Util.extractDomain(movie.getVideoUrl(), true, true);
            for (Element serverElem : serverElems) {
                Elements listElems = serverElem.getElementsByAttribute("data-link");
                int counter = 0;
                for (Element listElem : listElems) {
                    Movie server = Movie.clone(movie);
                    server.setState(Movie.RESOLUTION_STATE);
//                    String link = movie.getVideoUrl() + "??"+(counter++)+"||referer="+ referer;
                    String link = listElem.attr("data-link");
                    if (link == null) {
//                            link = movie.getVideoUrl() + "||Referer=" + domain;
                        continue;
                    }
//                        if (link.contains(".wiki")){
//                            continue;
//                        }
                    //              "if (title.includes(\"عرب سيد\")) {\n" +
                    //                        "        post.videoUrl = \""+movie.getVideoUrl()+"\"; \n" +
                    //                        "    }\n" +

                    link = link + "||Referer=" + domain;

//                        "&Accept-Encoding=\"gzip, deflate, br\""+
//                        "&Accept=\"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\""+
//                        "&Accept-Language=\"de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7\"";
//                        link = link + "||referer=https://arabseed.show/";
                    server.setVideoUrl(link);

                    Elements titleElems = listElem.getElementsByTag("span");
                    if (titleElems.size() > 0) {
                        if (titleElems.first() != null) {
                            String titleText = titleElems.first().text();
                            if (titleText.contains("عرب سيد")) {
                                continue;
                            }

                            server.setTitle(titleText);
                        }
                    } else {
                        server.setTitle(listElem.text());
                    }
                    Log.d(TAG, "fetchServers: servers: " + server);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(server);
                }

                break;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    /**
     * fetch movie resolutions and start an external video intent
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    private MovieFetchProcess fetchResolutions(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.i(TAG, "fetchResolutions: " + movie.getVideoUrl());

        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);

        activityCallback.onInvalidCookie(clonedMovie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, clonedMovie);
    }

    private MovieFetchProcess fetchCookie(Movie movie) {
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
    }

    /**
     * exactly same as fetchResolutions() fetch movie resolutions and start Exoplayer video intent
     *
     * @param movie
     * @return
     */
//    @Override
//    public Movie fetchToWatchLocally(final Movie movie) {
//        Log.i(TAG, "fetchToWatchLocally: " + movie.getVideoUrl());
//        if (movie.getState() == Movie.VIDEO_STATE) {
//            return movie;
//        }
//        Movie clonedMovie = Movie.clone(movie);
//        clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);
//
//        startWebForResultActivity(clonedMovie);
//        return null;
//    }

//    @Override
//    public void startVideo(final String link) {
//        Log.i(TAG, "startVideo: " + link);
//        ArabSeedServer.START_BROWSER_CODE = true;
//
//        WebView simpleWebView = activity.findViewById(R.id.webView);
//        simpleWebView.clearCache(true);
//        simpleWebView.clearFormData();
//        simpleWebView.clearHistory();
//
//
//        WebSettings webSettings = simpleWebView.getSettings();
//
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setAllowFileAccess(true);
//        //webSettings.setAppCacheEnabled(true);
//
//        simpleWebView.loadUrl(link);
//    }

//    @Override
//    public void startBrowser(String url) {
//        Log.i(TAG, "startBrowser: " + url);
//        if (url.contains("yout")) {
//            url = fixTrailerUrl(url);
//        }
//        WebView webView = activity.findViewById(R.id.webView);
//
//        webView.loadData("<html><body><iframe width=\"100%\" height=\"100%\" src=\"" + url + "\" frameborder=\"0\" allowfullscreen></iframe></body></html>", "text/html", "utf-8");
//    }

//    public String fixTrailerUrl(String url) {
//        Log.i(TAG, "browseTrailer: " + url);
//        String newUrl = url;
//        if (url.contains("v=")) {
//            newUrl = "https://www.youtube.com/embed/" +
//                    url.substring(url.indexOf("v=") + 2)
//                    + "?autoplay=1&fs=1";
//            Log.d(TAG, "browseTrailer: newUrl=" + newUrl);
//        }
////        Movie movie = new Movie();
////        movie.setVideoUrl(newUrl);
////        movie.setStudio(Movie.SERVER_AKWAM);
////        movie.setState(Movie.BROWSER_STATE);
////
////        Intent intent = new Intent(activity, BrowserActivity.class); //start a browser to fetch item
////        intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
////        activity.startActivity(intent);
//
//        //      WebView simpleWebView = activity.findViewById(R.id.webView);
//
////        simpleWebView.clearCache(true);
////        simpleWebView.clearFormData();
////        simpleWebView.clearHistory();
////
////
////        simpleWebView.setWebViewClient(new Browser_Home() {
////            // !url.contains("youtube") || !url.contains(WEBSITE_NAME);
////
////            @Override
////            public void onPageFinished(WebView view, String url) {
////                super.onPageFinished(view, url);
////                Log.d("WEBCLIENT", "onPageFinished");
////            }
////
////            @Override
////            public void onLoadResource(final WebView view, String url) {
////                Log.d("WEBCLIENT", "onLoadResource :url" + url);
////                super.onLoadResource(view, url);
////            }
////        });
////        simpleWebView.setWebChromeClient(new ChromeClient());
////        WebSettings webSettings = simpleWebView.getSettings();
////
////        webSettings.setJavaScriptEnabled(true);
////        webSettings.setAllowFileAccess(true);
////        webSettings.setAppCacheEnabled(true);
////        webSettings.setDomStorageEnabled(true);
////        webSettings.setLoadsImagesAutomatically(true);
////        webSettings.setBlockNetworkImage(false);
////
////        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
////        webSettings.setPluginState(WebSettings.PluginState.ON);
////        webSettings.setMediaPlaybackRequiresUserGesture(false);
////
////
////        simpleWebView.loadUrl(newUrl);
//        return newUrl;
//    }
    private boolean isSeries(Movie movie) {
        String u = movie.getVideoUrl();
        return u.contains("/series") || u.contains("/movies");
    }

//    public String getStudioText(String serverName) {
//
//        switch (serverName) {
//            case Movie.SERVER_SHAHID4U:
//                return "https://shahid4u";
//            case Movie.SERVER_FASELHD:
//                return "www.faselhd";
//            case Movie.SERVER_CIMA4U:
//                return "cima4u.io/";
//            case Movie.SERVER_AKWAM:
//                return "akwam.";
//
//        }
//
//        return "akwam.";
//    }
//
//    @Override
//    public boolean onLoadResource(Activity activity, WebView view, String url, Movie movie) {
//        return false;
//    }

    @Override
    public int detectMovieState(Movie movie) {
        if (isSeries(movie)) {
            return Movie.GROUP_OF_GROUP_STATE;
        }
        return Movie.ITEM_STATE;
    }

//    private Movie startWebForResultActivity(Movie movie) {
//        Intent browse = new Intent(activity, BrowserActivity.class);
//        browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//        browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
//        //   Log.d(TAG, "getResultFromWeb: activity:" + fragment.getClass().getName());
//        //activity.startActivity(browse);
//        fragment.startActivityForResult(browse, movie.getFetch());
//
//        return movie;
//    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        String script = null;
//        String referer = Util.extractDomain(movie.getVideoUrl(), true, true);
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
                        "        post.state = "+Movie.RESOLUTION_STATE+";\n" +
                        "        post.studio = \"" + movie.getStudio() + "\";\n" +
                        "        post.fetch = \"" + movie.getFetch() + "\";\n" +
                        "        post.cardImageUrl = \"" + movie.getCardImageUrl() + "\";\n" +
                        "        post.backgroundImageUrl = \"" + movie.getBackgroundImageUrl() + "\";\n" +
                        "        post.getMainMovieTitle = \"" + movie.getMainMovieTitle() + "\";\n" +
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
                        "        post.videoUrl = \"" + movie.getVideoUrl() + "\"; \n" +
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
                        "}" +
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
            else if (movie.getState() == Movie.COOKIE_STATE) {
                Log.d(TAG, "getScript: SERVER_ARAB_SEED COOKIE_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        "var postDivs = document.getElementsByClassName(\"MovieBlock\");\n" +
                        "if (postDivs.length > 0){\n" +
                        "var postList = [];\n" +
                        "for (var i = 0; i < postDivs.length; i++) {\n" +
                        "var post = {};\n" +
                        "var postDiv = postDivs[i];\n" +
                        "var category = postDiv.getElementsByClassName(\"category\")[0].textContent;" +
                        "if (category.match(/تطبيق|برامج|برنامج|موبايل|اغاني|العاب|لعبة|لعبه|صور/) != null){continue;}" +
                        "var poster = postDiv.getElementsByClassName(\"Poster\")[0];\n" +
                        "var imageDiv = poster.querySelector(\"[data-src]\");\n" +
                        "if (imageDiv == null) {\n" +
                        "                    continue;\n" +
                        "                }" +
                        "post.title = imageDiv.getAttribute(\"alt\");\n" +
                        "post.videoUrl = postDiv.getElementsByTagName(\"a\")[0].getAttribute('href');\n" +
                        "rateDiv = postDiv.getElementsByClassName(\"RateNumber\");\n" +
                        "if(rateDiv.length > 0) {\n" +
                        "post.rate = rateDiv[0].textContent;\n" +
                        "}\n" +
                        "post.cardImageUrl = imageDiv.getAttribute('data-src');" +
                        "if (post.cardImageUrl == null ){ post.cardImageUrl = imageDiv.getAttribute('src'); }" +
                        "post.bgImageUrl = post.cardImageUrl;\n" +
                        "post.studio = \"" + Movie.SERVER_ARAB_SEED + "\";\n" +
                        "post.state = 2;\n" +
                        "postList.push(post);\n" +
                        "}" +
                        "    if (postList.length > 0) {\n" +
                        "        MyJavaScriptInterface.myMethod(JSON.stringify(postList));\n" +
                        "    }" +
                        "}" +
                        "});";

            }
//            else if (movie.getState() == Movie.ITEM_STATE) {
//                Log.d(TAG, "getScript: SERVER_ARAB_SEED ITEM_STATE");
////                    script ="document.addEventListener(\"DOMContentLoaded\", () => {" +
////                            "fetchMovieData();" +
////                            "" +
////                            "async function fetchMovieData() {\n" +
////                            "    // Fetch description\n" +
////                            "    let storyElem = document.querySelector('.StoryLine .descrip:not([style])');\n" +
//////                            "    let description = storyElem ? storyElem.textContent.trim() : '';\n" +
//////                            "\n" +
//////                            "    // Set movie description\n" +
//////                            "    setDescription(description);\n" +
//////                            "\n" +
////                            "    // Fetch watch links\n" +
////                            "    let watchElem = document.querySelector('.WatchButtons');\n" +
////                            "    if (!watchElem) return;\n" +
////                            "\n" +
////                            "            let domain = \"" + Util.extractDomain(movie.getVideoUrl(), true, true) +"\";" +
////                            "    // Try fetching direct link from 'a' tags\n" +
////                            "    let linkElem = watchElem.querySelector('a[href]');\n" +
////                            "    if (linkElem) {\n" +
////                            "        let link = linkElem.getAttribute('href');\n" +
////                            "        if (link) {\n" +
////                            "            let newMovie = { videoUrl: link, state: " + Movie.BROWSER_STATE +" };\n" +
////                            "            return await fetchServers(null, newMovie, domain);\n" +
////                            "        }\n" +
////                            "    }\n" +
////                            "\n" +
////                            "    // Attempt to find a valid form for a POST request\n" +
////                            "    let form = Array.from(watchElem.querySelectorAll('form')).find(f => \n" +
////                            "        f.querySelector(\"input[type='hidden']\") && f.querySelector('button')\n" +
////                            "    );\n" +
////                            "    if (!form) return;\n" +
////                            "\n" +
////                            "    let postUrl = form.getAttribute('action');\n" +
////                            "    if (!postUrl) return;\n" +
////                            "\n" +
////                            "    let hiddenInput = form.querySelector(\"input[type='hidden']\");\n" +
////                            "    let inputName = hiddenInput?.getAttribute('name');\n" +
////                            "    let inputValue = hiddenInput?.value;\n" +
////                            "\n" +
////                            "    if (!inputName || !inputValue) return;\n" +
////                            "\n" +
////                            "    let postData = new URLSearchParams();\n" +
////                            "    postData.append(inputName, inputValue);\n" +
////                            "\n" +
////                            "    try {\n" +
////                            "        let response = await fetch(postUrl, {\n" +
////                            "            method: 'POST',\n" +
////                            "            headers: {\n" +
////                            "                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',\n" +
////                            "                'Content-Type': 'application/x-www-form-urlencoded',\n" +
////                            "                'Referer': domain,\n" +
////                            "            },\n" +
////                            "            body: postData\n" +
////                            "        });\n" +
////                            "\n" +
////                            "        if (response.ok) {\n" +
////                            "            let responseText = await response.text();\n" +
////                            "            let parser = new DOMParser();\n" +
////                            "            let responseDoc = parser.parseFromString(responseText, 'text/html');\n" +
////                            "            return await fetchServers(responseDoc, movie, domain );\n" +
////                            "        }\n" +
////                            "    } catch (error) {\n" +
////                            "        console.error('Error fetching POST request:', error);\n" +
////                            "    }\n" +
////                            "}\n" +
////                            "\n" +
////                            "async function fetchServers(responseDoc, movie, referer) {\n" +
////                            "let doc = responseDoc ? responseDoc : document;" +
////
////                            "    let serverElems = doc.querySelectorAll('.containerServers [data-link]');\n" +
////                            "    if (!serverElems?.length) {\n" +
////                            "        return;\n" +
////                            "    }\n" +
////                            "    let movieList = [];\n" +
////                            "\n" +
////                            "    serverElems.forEach(elem => {\n" +
////                            "        let link = elem.getAttribute('data-link');\n" +
////                            "        if (!link) return;\n" +
////                            "\n" +
////                            "        if (link.includes('عرب سيد')) return;\n" +
////                            "\n" +
////                            "        link += `||Referer=${domain}`;\n" +
////                            "\n" +
////                            "        let server = { videoUrl: link, state: " + Movie.RESOLUTION_STATE+ " };\n" +
////                            "        let title = elem.querySelector('span')?.textContent.trim() || elem.textContent.trim();\n" +
////                            "        if (title) server.title = title;\n" +
////                            "\n" +
////                            "        movieList.push(server);\n" +
////                            "    });\n" +
////                            "\n" +
////                            "if (movieList.length > 0){" +
////                            "        MyJavaScriptInterface.myMethod(JSON.stringify(movieList));\n" +
////                            "}" +
////                            "}\n" +
////                            "\n" +
////                            "" +
////
////                            "});";
//            }
        }
        Log.d(TAG, "getWebScript: " + script);
        return script;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
        return search(
//                config.url +"/category/netfilx/");
                getConfig().getUrl() + "/latest1/", activityCallback);
    }

    @Override
    public String getLabel() {
        return "عرب سيد";
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_ARAB_SEED;
    }

    public String getCustomUserAgent(int state) {
        return "Android 7";
    }

    public MovieFetchProcess handleJSResult(String elementJson, List<Movie> movies, Movie movie) {
        // just return the result if its browse state
        if (movie.getState() == Movie.BROWSER_STATE) {
            movie.setSubList(movies);
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_RETURN_RESULT, movie);
        }
        return super.handleJSResult(elementJson, movies, movie);
    }

    public boolean shouldOverrideUrlLoading(Movie movie, WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        String newUrl = request.getUrl().toString().length() > 25 ? request.getUrl().toString().substring(0, 25) : request.getUrl().toString();
        if (url.contains("/e/")) {
            Log.d(TAG, "shouldOverrideUrlLoading:0b false: " + url);
            return false;
        }
        if (newUrl.contains("gameland")) {
            Log.d(TAG, "shouldOverrideUrlLoading:0c false: " + url);
            return false;
        }
        return super.shouldOverrideUrlLoading(movie, view, request);
    }
}
