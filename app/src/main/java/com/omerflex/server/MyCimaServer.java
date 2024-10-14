package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.DetailsActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.util.ArrayList;

public class MyCimaServer extends AbstractServer {
    static String TAG = "MyCima";
    public static String WEBSITE_URL = "https://mycima.io";

    public MyCimaServer() {
    }

    @Override
    public ArrayList<Movie> search(String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Log.i(getLabel(), "search: " + query);
        String searchContext = query;
        String url = query;
        boolean multiSearch = false;
        if (!query.contains("http")) {
            url = this.getSearchUrl(query);
            multiSearch = true;
        }
        Log.d(TAG, "search: url: " + url);
        Document doc = this.getRequestDoc(url);
        if (doc == null) {
            activityCallback.onInvalidLink("Invalid link");
            return null;
        }
        ArrayList<Movie> movieList = new ArrayList<>();

        if (doc.title().contains("moment")) {
//            setCookieRefreshed(false);
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
            m.setStudio(Movie.SERVER_MyCima);
            m.setVideoUrl(url);
            //  m.setVideoUrl("https://www.google.com/");
            m.setState(Movie.COOKIE_STATE);
            // m.setState(Movie.RESULT_STATE);
            m.setCardImageUrl(cardImageUrl);
            m.setBackgroundImageUrl(backgroundImageUrl);
            m.setRate("");
            m.setSearchContext(searchContext);
            movieList.add(m);

            activityCallback.onInvalidCookie(movieList, getLabel());
            return movieList;
        }


        Elements lis = null;
        if (query.contains("http")) {
            Elements box = doc.getElementsByClass("Grid--WecimaPosts");
            if (box.size() > 0) {
                Elements boxItems = box.first().getElementsByClass("GridItem");
                if (boxItems.size() > 0) {
                    lis = boxItems;
                }
            }
        }
        if (lis == null) {
            lis = doc.getElementsByClass("GridItem");
        }
        for (Element li : lis) {
            Movie movie = this.generateMovieFromDocElement(li);
            if (movie != null) {
                movieList.add(movie);
            }
        }
        if (multiSearch) {
            this.getExtraSearchMovieList(doc.baseUri(), movieList);
        }

        Movie nextPage = this.generateNextPageMovie(doc);

        Log.d(TAG, "search: nextPage: " + nextPage);

        if (nextPage != null) {
            movieList.add(nextPage);
        }

        activityCallback.onSuccess(movieList, getLabel());
        return movieList;
    }

    @Override
    public void shouldInterceptRequest(WebView view, WebResourceRequest request) {

    }

    protected String getSearchUrl(String query) {
//        String searchUrl = query;
        if (query.contains("http")) {
            return query;
        }
        String searchPart = "/search/";
        ServerConfig config = getConfig();
        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            return WEBSITE_URL + searchPart + query;
        }

        if (query.startsWith("/")) {
            return config.getUrl() + query;
        }

        return config.getUrl() + searchPart + query;
    }

    private void getExtraSearchMovieList(String url, ArrayList<Movie> movies) {
        //search series
        String seriesSearch = url + "/list/series/";
        Document doc = getRequestDoc(seriesSearch);

        Elements lis2 = doc.getElementsByClass("GridItem");
        for (Element li : lis2) {
            Movie movie = generateMovieFromDocElement(li);
            if (movie != null) {
                movies.add(movie);
            }
        }

        //search anime
        String animeSearch = url + "/list/anime/";
        doc = getRequestDoc(animeSearch);
        Elements lis3 = doc.getElementsByClass("GridItem");
        for (Element li : lis3) {
            //              Log.i(TAG, "element found: ");
            Movie movie = generateMovieFromDocElement(li);
            if (movie != null) {
                movies.add(movie);
            }
        }
    }

    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return new ArrayList<>();
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_MyCima;
    }

    private Movie generateNextPageMovie(Document doc) {
        Movie nextPage = null;
        //nextpage
        Elements nextLinkNaviElements = doc.getElementsByClass("next");
        //        Log.d(TAG, "search: nextpage1 found :"+nextLinkNaviElements.size());
        if (!nextLinkNaviElements.isEmpty()) {
            Element nextLinkNaviElement = nextLinkNaviElements.first();
            if (nextLinkNaviElement != null) {
                String videoUrl = nextLinkNaviElement.attr("href");
                nextPage = new Movie();
                nextPage.setTitle("التالي");
                nextPage.setDescription("0");
                nextPage.setStudio(Movie.SERVER_MyCima);
                nextPage.setVideoUrl(videoUrl);
                nextPage.setCardImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                nextPage.setBackgroundImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                nextPage.setState(Movie.NEXT_PAGE_STATE);
                nextPage.setMainMovie(nextPage);
                nextPage.setMainMovieTitle(videoUrl);
            }
        }

        return nextPage;
    }

    private Movie generateMovieFromDocElement(Element li) {
        Movie movie = null;
        try {
            Element linkElem = li.getElementsByAttribute("href").first();
            if (linkElem != null) {
                movie = new Movie();
                movie.setStudio(Movie.SERVER_MyCima);

                String videoUrl = linkElem.attr("href");

                Element nameElem = li.getElementsByAttribute("title").first();
                String title = "";
                if (nameElem != null) {
                    title = nameElem.attr("title");
                }

                Element imageElem = li.getElementsByAttribute("style").first();
                String image = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
                if (imageElem != null) {
                    image = imageElem.attr("style");
                }

                if (!image.contains("http")) {
                    // image = li.getElementsByClass("BG--GridItem").attr("style");
                    Element image2Elem = li.getElementsByAttribute("data-lazy-style").first();
                    if (image2Elem != null) {
                        image = image2Elem.attr("data-lazy-style");
                    }
                }

                if (image.equals("")) {
                    Element image3Elem = li.getElementsByClass("BG--GridItem").first();
                    if (image3Elem != null) {
                        image = image3Elem.attr("data-owl-img");
                    }
                }

                if (image.contains("(") && image.contains(")")) {
                    image = image.substring(image.indexOf('(') + 1, image.indexOf(')'));
                }

//                Log.d(TAG, "generateMovieFromDocElement: image: "+image);
                movie.setTitle(title);
                movie.setVideoUrl(videoUrl);
//                if (isSeries(movie)) {
//                    movie.setState(Movie.GROUP_OF_GROUP_STATE);
//                } else {
//                    movie.setState(Movie.ITEM_STATE);
//                }
                // detectMovieState important after setting the title and videoUrl
                movie.setState(detectMovieState(movie));
                movie.setDescription("");
                movie.setStudio(Movie.SERVER_MyCima);
                movie.setCardImageUrl(image);
                movie.setBackgroundImageUrl(image);
                movie.setBgImageUrl(image);
                movie.setState(movie.getState());
                movie.setMainMovieTitle(videoUrl);
                movie.setMainMovie(movie);
            }

        } catch (Exception e) {
            Log.i(getLabel(), "error: " + e.getMessage());
        }
        return movie;
    }

    public MovieFetchProcess fetchBrowseItem(Movie movie, ActivityCallback<Movie> activityCallback) {
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
        // to do nothing and wait till result returned to activity only the first fetch
//        return startWebForResultActivity(clonedMovie);
        activityCallback.onInvalidCookie(clonedMovie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, clonedMovie);
    }

    private void addMovieToHistory(Movie movie) {

        final Movie m = new Movie();
        /* final Movie m = Movie.buildMovieInfo(
                movie.getHistoryTitle(),
                movie.getDescription(),
                movie.getStudio(),
                movie.getHistoryUrl(),
                // movie.getHistoryUrl(),
                movie.getCardImageUrl(),
                movie.getBackgroundImageUrl(),
                movie.getHistoryState(),
                movie.getRate(),
                movie.getHistoryUrl(),
                movie.getHistoryTitle(),
                movie.getHistoryState(),
                movie.getHistoryCardImageUrl(),
                movie.getCreatedAt(),
                movie.getHistoryDescription()
        );
        */

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
//            case Movie.RESOLUTION_STATE:
//                return fetchResolutions(movie);
//            case Movie.VIDEO_STATE:
//                return fetchVideo(movie);
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
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_EXOPLAYER, movie);
    }

    @Override
    public int detectMovieState(Movie movie) {
        String u = movie.getVideoUrl();
        String n = movie.getTitle();
        // Log.d(TAG, "isSeries: title:" + n + ", url=" + u);
        boolean seriesCase = n.contains("انمي") || n.contains("برنامج") || n.contains("مسلسل")
                || u.contains("series");
        boolean itemCase = u.contains("/watch") || n.contains("حلقة") || n.contains("حلقه");

        if (itemCase) {
            return Movie.ITEM_STATE;
        } else if (seriesCase) {
            return Movie.GROUP_OF_GROUP_STATE;
        }
        return Movie.ITEM_STATE;
    }

    public String determineRelatedMovieLabel(Movie movie) {
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
                return "المواسم/الحلقات";
            case Movie.GROUP_STATE:
                return "الحلقات";
            case Movie.ITEM_STATE:
                return "الجودة";
            default:
                return "الروابط";
        }
    }

    private MovieFetchProcess fetchGroupOfGroup(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());
        String url = movie.getVideoUrl();
        Log.i(TAG, "ur:" + url);
        Document doc = getRequestDoc(url);
        if (doc == null) {
            activityCallback.onInvalidLink(movie);
            return null;
        }
        if (doc.title().contains("Just a moment")) {
//            Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
//            return startWebForResultActivity(clonedMovie);
            activityCallback.onInvalidCookie(movie, getLabel());
        }

        return generateGroupOfGroupMovie(doc, movie, activityCallback);
    }

    private MovieFetchProcess startWebForResultActivity(Movie movie) {
        Log.d(TAG, "startWebForResultActivity: " + movie);
//        Intent browse = new Intent(activity, BrowserActivity.class);
//        browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//        browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
//
//        fragment.startActivityForResult(browse, movie.getFetch());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);
    }

    private MovieFetchProcess generateGroupOfGroupMovie(Document doc, Movie movie, ActivityCallback<Movie> activityCallback) {
        //get link of episodes page
        Log.d(TAG, "generateGroupOfGroupMovie: " + doc.title());
        Element descElem = doc.getElementsByClass("PostItemContent").first();
        String desc = "";
        if (descElem != null) {
            desc = descElem.text();
            movie.setDescription(desc);
        }

        //fetch session
        Elements boxs = doc.getElementsByClass("List--Seasons--Episodes");

        if (boxs.isEmpty()) {
            movie.setState(Movie.GROUP_STATE);
            if (movie.getVideoUrl() == null) {
                return null;
            }
            return fetchGroup(movie, activityCallback);
        }

        for (Element box : boxs) {
            Elements lis = box.getElementsByTag("a");
            for (Element li : lis) {
                String title = li.text();
                String videoUrl = li.attr("href");
                String cardImageUrl = movie.getCardImageUrl();
                String backgroundImageUrl = movie.getCardImageUrl();
                Movie episode = Movie.clone(movie);
                episode.setTitle(title);
                episode.setDescription(desc);
                episode.setVideoUrl(videoUrl);
                episode.setState(Movie.GROUP_STATE);
                if (movie.getSubList() == null) {
                    movie.setSubList(new ArrayList<>());
                }
                movie.addSubList(episode);
            }
        }
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    private MovieFetchProcess fetchGroup(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.i(TAG, "fetchGroup: " + movie.getVideoUrl());

        String url = movie.getVideoUrl();
        //         Log.i(TAG, "ur:" + url);
//            Document doc = Jsoup.connect(url).header(
//                    "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8").header(
//                    "User-Agent", " Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36").header(
//                    "accept-encoding", "gzip, deflate").header(
//                    "accept-language", "en,en-US;q=0.9").header(
//                    "x-requested-with", "pc1"
//            ).timeout(6000).get();
        //Elements links = doc.select("a[href]");

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

        return generateGroupMovie(doc, movie, activityCallback);
    }

    private MovieFetchProcess generateGroupMovie(Document doc, Movie movie, ActivityCallback<Movie> activityCallback) {
        //get link of episodes page
        Element descElem = doc.getElementsByClass("PostItemContent").first();
        String desc = "";
        if (descElem != null) {
            desc = descElem.text();
            movie.setDescription(desc);
        }

        //fetch session
        Elements boxs = doc.getElementsByClass("Episodes--Seasons--Episodes");
        for (Element box : boxs) {

            Elements lis = box.getElementsByTag("a");

            //            Log.i("Count", "boxs:" + boxs.size());
            for (Element li : lis) {
                String title = li.text();
                String videoUrl = li.attr("href");

                Movie a = new Movie();
                a.setStudio(Movie.SERVER_MyCima);

                Movie episode = Movie.clone(movie);
                episode.setTitle(title);
                episode.setVideoUrl(videoUrl);
                episode.setState(Movie.ITEM_STATE);
                if (movie.getSubList() == null) {
                    movie.setSubList(new ArrayList<>());
                }
                movie.addSubList(episode);
            }
        }
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    private MovieFetchProcess fetchItem(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());
        String url = movie.getVideoUrl();
//            Document doc = Jsoup.connect(url).header(
//                    "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8").header(
//                    "User-Agent", " Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36").header(
//                    "accept-encoding", "gzip, deflate").header(
//                    "accept-language", "en,en-US;q=0.9").header(
//                    "x-requested-with", "pc1"
//            ).timeout(0).get();
        //Elements links = doc.select("a[href]");

        Document doc = getRequestDoc(url);
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

        return generateItemMovie(doc, movie, activityCallback);
    }

    private MovieFetchProcess fetchCookie(Movie movie) {
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
    }

    private boolean isSeries(Movie movie) {
        String u = movie.getVideoUrl();
        String n = movie.getTitle();
        // Log.d(TAG, "isSeries: title:" + n + ", url=" + u);
        return n.contains("انمي") || n.contains("برنامج") || n.contains("مسلسل")
                || u.contains("series");
    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        String script = null;

        Log.d(TAG, "getWebScript: m:" + mode + ", f:" + movie.getFetch());
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_STARTED) {
////            if (movie.getState() == Movie.GROUP_OF_GROUP_STATE){
//                Log.d(TAG, "getScript:mycima REQUEST_CODE_FETCH_HTML");
//                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
////                        "var serverElems = document.getElementsByClassName('PostItemContent');\n" +
////                        "console.log(serverElems.length);" +
////                        "if(serverElems.length > 0){" +
//                        "var html = ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');" +
//                        "MyJavaScriptInterface.myMethod(html);" +
////                        "}" +
//                        "});";
////            }
//

            if (
                    movie.getState() == Movie.GROUP_OF_GROUP_STATE ||
                            movie.getState() == Movie.GROUP_STATE
            ) {
                //Log.d(TAG, "getScript:mycima GROUP_OF_GROUP_STATE");
                String itemBoxName = "List--Seasons--Episodes";

                int state = Movie.GROUP_STATE;
                if (movie.getState() == Movie.GROUP_STATE) {
                    Log.d(TAG, "getScript:mycima GROUP_STATE");
                    itemBoxName = "Episodes--Seasons--Episodes";
                    state = Movie.ITEM_STATE;
                }
                script = "  document.addEventListener('DOMContentLoaded', () => {\n" +
                        "if (!document.title.includes('Just a moment')){" +
                        "console.log('title: '+ document.title);" +
                        "var descElems = document.getElementsByClassName('StoryMovieContent');\n" +
                        "                        var postList = [];\n" +
                        "                        var desc = '';\n" +
                        "                       if(descElems.length > 0){\n" +
                        "                        desc = descElems[0].textContent;\n" +
                        "                        }else{\n" +
                        "                                descElems = document.getElementsByClassName('PostItemContent');\n" +
                        "                                if(descElems.length > 0){\n" +
                        "                        desc = descElems[0].textContent;\n" +
                        "                            }\n" +
                        "                        }" +
                        "var boxs = document.getElementsByClassName('" + itemBoxName + "');" +
                        "if(boxs.length == 0){\n" +
                        "   boxs = document.getElementsByClassName('Episodes--Seasons--Episodes');" +
                        "}" +
                        "if(boxs.length > 0){" +
                        "    var box = boxs[0];\n" +
                        "    var lis = box.getElementsByTagName('a');\n" +
                        "     if(lis.length > 0){\n" +
                        "                             for (let l = 0; l < lis.length; l++) {\n" +
                        "                                var li = lis[l];\n" +
                        "                                var post = {};\n" +
                        "                                post.title = li.textContent;\n" +
                        "                                post.videoUrl = li.getAttribute('href');\n" +
                        "                                post.description = desc;\n" +
                        "                                post.state = '" + state + "';\n" +
                        "                                // Clone 'movie' object\n" +
                        "                                post.studio = '" + movie.getStudio() + "' ;\n" +
                        "                                post.fetch = '" + movie.getFetch() + "';\n" +
                        "                                post.studio = '" + Movie.SERVER_MyCima + "';\n" +
                        "                                post.cardImageUrl = '" + movie.getStudio() + "';\n" +
                        "                                post.backgroundImageUrl = '" + movie.getBackgroundImageUrl() + "';\n" +
                        "                                post.cardImageUrl = '" + movie.getCardImageUrl() + "';\n" +
                        "                                post.getMainMovieTitle = '" + movie.getMainMovieTitle() + "';\n" +
                        "                             }" +
                        "                             MyJavaScriptInterface.myMethod(JSON.stringify(postList));\n" +
                        "                          }\n" +
                        "     }\n" +
                        "     }\n" +
                        " });";
            } else if (movie.getState() == Movie.ITEM_STATE) {
                String referer = Util.extractDomain(movie.getVideoUrl(), true, true);
                script = "document.addEventListener('DOMContentLoaded', () => {\n" +
                        "if (!document.title.includes('Just a moment')){" +
                        "var descElems = document.getElementsByClassName('StoryMovieContent');\n" +
                        "                        var postList = [];\n" +
                        "                        var desc = \"\";\n" +
                        "                        console.log(descElems.length);\n" +
                        "                        if(descElems.length > 0){\n" +
                        "                        desc = descElems[0].textContent;\n" +
                        "                        }else{\n" +
                        "                                descElems = document.getElementsByClassName('PostItemContent');\n" +
                        "                                if(descElems.length > 0){\n" +
                        "                        desc = descElems[0].textContent;\n" +
                        "                            }\n" +
                        "                        }\n" +
                        "                        // download links\n" +
                        "var uls = document.getElementsByClassName('List--Download--Wecima--Single');\n" +
                        "if(uls.length > 0){\n" +
                        "   var box = uls[0];\n" +
                        "       var lis = box.getElementsByTagName('li');\n" +
                        "        if(lis.length > 0){\n" +
                        "                                for (let l = 0; l < lis.length; l++) {\n" +
                        "                                   var li = lis[l];\n" +
                        "                                   var post = {};\n" +
                        "\n" +
                        "                                   var videoLinkElems = li.getElementsByTagName('a');\n" +
                        "                                   if(videoLinkElems.length > 0){\n" +
                        "                                    var videoLinkElem = videoLinkElems[0];\n" +
                        "                                    var delimiter  = '||'; " +
                        "                                    var videoUrl = videoLinkElem.getAttribute('href');\n" +
                        "                                    post.videoUrl = videoUrl + delimiter + 'referer=' + '" + referer + "' ;\n" +
                        "\n" +
                        "                                    var titleElems = li.getElementsByTagName('resolution');\n" +
                        "                                     if(titleElems.length > 0){\n" +
                        "                                        var titleElem = titleElems[0];\n" +
                        "                                         post.title = titleElem.textContent;\n" +
                        "                                      }\n" +
                        "\n" +
                        "                                      post.description = desc;\n" +
                        "                                                                         post.state = '" + Movie.RESOLUTION_STATE + "' ;\n" +
                        "\n" +
                        "                                                                         // Clone 'movie' object\n" +
                        "                                                                         post.studio = '" + movie.getStudio() + "' ;\n" +
                        "                                                                         post.fetch = '" + movie.getFetch() + "' ;\n" +
                        "                                                                         post.cardImageUrl = '" + movie.getStudio() + "' ;\n" +
                        "                                                                         post.backgroundImageUrl = '" + movie.getBackgroundImageUrl() + "' ;\n" +
                        "                                                                         post.cardImageUrl = '" + movie.getCardImageUrl() + "' ;\n" +
                        "                                                                         post.getMainMovieTitle = '" + movie.getMainMovieTitle() + "' ;\n" +
                        "                                                                         postList.push(post);\n" +
                        "                                 }\n" +
                        "                                }\n" +
                        "                             }\n" +
                        "}\n" +
                        "\n" +
                        "                        // watch links\n" +
                        "var ulsWatch = document.getElementsByClassName('WatchServersList');\n" +
                        "console.log('ulsWatch: '+ulsWatch.length);" +
                        "console.log('title: '+ document.title);" +
                        "if(ulsWatch.length > 0){\n" +
                        "   var boxWatch = ulsWatch[0];\n" +
                        "       var lisWatch = boxWatch.getElementsByTagName('btn');\n" +
                        "        if(lisWatch.length > 0){\n" +
                        "                                for (let l = 0; l < lisWatch.length; l++) {\n" +
                        "                                   var li = lisWatch[l];\n" +
                        "                                   var post = {};\n" +
                        "                                    var videoUrl = li.getAttribute('data-url');\n" +
                        "\n" +
                        "                                    if(videoUrl === \"\"){\n" +
                        "                                    continue;\n" +
                        "                                    }\n" +
//                        "                                    // todo: handle if the link already has headers like ?key\n" +

                        "                                    var delimiter  = '||'; " +
                        "                                    post.videoUrl = videoUrl + delimiter + 'referer=' + '" + referer + "' ;\n" +
                        "                                    var titleElems = li.getElementsByTagName('strong');\n" +
                        "                                     if(titleElems.length > 0){\n" +
                        "                                        var titleElem = titleElems[0];\n" +
                        "                                         post.title = titleElem.textContent;\n" +
                        "                                      }\n" +
                        "\n" +
                        "                                      post.description = desc;\n" +
                        "                                                                         post.state = '" + Movie.BROWSER_STATE + "' ;\n" +
                        "\n" +
                        "                                                                         // Clone 'movie' object\n" +
                        "                                                                         post.studio = '" + movie.getStudio() + "' ;\n" +
                        "                                                                         post.fetch = '" + movie.getFetch() + "' ;\n" +
                        "                                                                         post.cardImageUrl = '" + movie.getCardImageUrl() + "' ;\n" +
                        "                                                                         post.backgroundImageUrl = '" + movie.getBackgroundImageUrl() + "' ;\n" +
                        "                                                                         post.cardImageUrl = '" + movie.getCardImageUrl() + "' ;\n" +
                        "                                                                         post.getMainMovieTitle = '" + movie.getMainMovieTitle() + "' ;\n" +
                        "                                                                         postList.push(post);\n" +
                        "                                }\n" +
                        "                             }\n" +
                        "}\n" +
                        "MyJavaScriptInterface.myMethod(JSON.stringify(postList));\n" +
                        "}\n" +
                        " });";
            }


        }
//        Log.d(TAG, "getWebScript: "+script);
        return script;
    }


    private void handleJSWebResult__(Activity activity, Movie movie, String jsResult, ActivityCallback<Movie> activityCallback) {
        Log.d(TAG, "handleJSWebResult - " + movie);
        if (jsResult != null && jsResult.startsWith("<html>")) {

            Document doc = Jsoup.parse(jsResult);

            Movie newMovie = generateGroupOfGroupMovie(doc, movie, activityCallback).movie;

            Log.d(TAG, "handleJSWebResult: sublist; " + movie.getSubList().size());
            Intent intent = new Intent();
            intent.putExtra(DetailsActivity.MOVIE, (Serializable) newMovie);
            if (newMovie != null && newMovie.getSubList() != null) {
//                Gson gson = new Gson();
//                String movieJson = gson.toJson(newMovie.getSubList());
                intent.putExtra(DetailsActivity.MOVIE_SUBLIST, (Serializable) newMovie.getSubList());
            }
//                                intent.putExtra(DetailsActivity.MOVIE_SUBLIST, (Serializable) movieJson);


            activity.setResult(Activity.RESULT_OK, intent);
            Log.d(TAG, "handleJSWebResult - 2");
            activity.finish();
        }
    }

    public MovieFetchProcess handleOnActivityResultHtml(String html, Movie m, ActivityCallback<Movie> activityCallback) {
        if (html == null) {
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, m);
        }
        Document doc = Jsoup.parse(html);
        Log.d(TAG, "handleOnActivityResultHtml: s: ");
        Log.d(TAG, "handleJSWebResult: sublistm ; " + m.getSubList().size());
        if (m == null) {
            m = new Movie();
            m.setSubList(new ArrayList<>());
        }
        if (m.getState() == Movie.GROUP_OF_GROUP_STATE) {
            return generateGroupOfGroupMovie(doc, m, activityCallback);
        } else if (m.getState() == Movie.GROUP_STATE) {
            return generateGroupMovie(doc, m, activityCallback);
        } else if (m.getState() == Movie.ITEM_STATE) {
            return generateItemMovie(doc, m, activityCallback);
        }
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, m);
    }

    private MovieFetchProcess generateItemMovie(Document doc, Movie movie, ActivityCallback<Movie> activityCallback) {

        //get link of episodes page
        Element descElem = doc.getElementsByClass("StoryMovieContent").first();
        String desc = "";
        if (descElem != null) {
            desc = descElem.text();
            movie.setDescription(desc);
        }
        String referer = Util.extractDomain(movie.getVideoUrl(), true, true);
        Elements uls = doc.getElementsByClass("List--Download--Wecima--Single");
//        Log.d(TAG, "generateItemMovie: html: " + doc.html());
//        Log.d(TAG, "generateItemMovie: title: " + doc.title());
//        Log.d(TAG, "generateItemMovie: uls: " + uls.size());
        for (Element ul : uls) {
            Elements lis = ul.getElementsByTag("li");
            for (Element li : lis) {

                Element videoUrlElem = li.getElementsByAttribute("href").first();
                if (videoUrlElem != null) {
                    String videoUrl = videoUrlElem.attr("href");

                    Element titleElem = li.getElementsByTag("resolution").first();
                    String title = movie.getTitle();
                    if (titleElem != null) {
                        title = titleElem.text();
                    }

                    Movie episode = Movie.clone(movie);
                    episode.setTitle(title);
                    episode.setDescription(desc);
                    if (!getConfig().getHeaders().containsKey("referer")) {
                        getConfig().getHeaders().put("referer", referer);
                    }
                    episode.setVideoUrl(videoUrl + Util.generateHeadersForVideoUrl(getConfig().getHeaders()));
                    episode.setState(Movie.RESOLUTION_STATE);
                    if (movie.getSubList() == null) {
                        movie.setSubList(new ArrayList<>());
                    }
                    movie.addSubList(episode);
                }
            }
            break;
        }

        uls = doc.getElementsByClass("WatchServersList");
        for (Element ul : uls) {
            Elements lis = ul.getElementsByAttribute("data-url");
            for (Element li : lis) {
                String videoUrl = li.attr("data-url");

                if (videoUrl == null || videoUrl.equals("")) {
                    continue;
                }
                videoUrl = videoUrl + "|referer=" + referer;
//                videoUrl = videoUrl + Util.generateHeadersForVideoUrl(headers);

                Element titleElem = li.getElementsByTag("strong").first();
                String title = movie.getTitle();
                if (titleElem != null) {
                    title = titleElem.text();
                }

//                        Movie a = new Movie();
//
//                        a.setState(Movie.BROWSER_STATE);
//
//                        a.setStudio(Movie.SERVER_MyCima);

                Movie episode = Movie.clone(movie);
                episode.setTitle(title);
                episode.setDescription(desc);
                episode.setVideoUrl(videoUrl);
                episode.setState(Movie.BROWSER_STATE);
                if (movie.getSubList() == null) {
                    movie.setSubList(new ArrayList<>());
                }
                movie.addSubList(episode);
            }
            Log.d(TAG, "generateItemMovie: " + movie);
            break;
        }
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
        return search("sonic", activityCallback);
//        return search("ratched");
//        return search(getConfig().getUrl() + "/movies/");
//        return search(config.url + "/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2024/list/");
//        return search(config.url);
    }

    @Override
    public String getLabel() {
        return "ماي سيما";
    }
}
