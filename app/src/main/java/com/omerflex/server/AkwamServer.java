package com.omerflex.server;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.ServerConfigManager;
import com.omerflex.service.logging.Logger;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AkwamServer extends AbstractServer {

    static final String TAG = "Akwam";
    public static final int REQUEST_CODE = 1;
    public static final String WEBSITE_URL = "https://www.akwam.cc";

    public AkwamServer() {
        try {
            initialize(ServerConfigManager.getContext());
            if (context != null) {
                ServerOptimizer.initialize(context);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error initializing AkwamServer", e);
        }
    }

    @Override
    public ArrayList<Movie> search(String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Logger.i(TAG, "search: " + query);
        String searchContext = query;
        String url = query;
        if (!query.contains("http")) {
            url = this.getSearchUrl(query);
        }
        Logger.d(TAG, "search: " + url);

        try {
            Document doc = ServerOptimizer.getDocumentWithCache(url, getConfig());
            if (doc == null) {
                Logger.w(TAG, "Failed to get document for URL: " + url);
                if (activityCallback != null) {
                    activityCallback.onInvalidLink("Invalid link");
                }
                return null;
            }

            ArrayList<Movie> securityCheckResult = handleSearchSecurityCheck(doc, searchContext, url, activityCallback);
            if (securityCheckResult != null) {
                return securityCheckResult;
            }

            ArrayList<Movie> movieList = fetchSearchMovies(searchContext, doc);
            if (activityCallback != null) {
                activityCallback.onSuccess(movieList, getLabel());
            }
            return movieList;
        } catch (Exception e) {
            Logger.e(TAG, "Error during search operation", e);
            if (activityCallback != null) {
                activityCallback.onInvalidLink("Error: " + e.getMessage());
            }
            return null;
        }
    }

    private ArrayList<Movie> handleSearchSecurityCheck(Document doc, String searchContext, String url, ActivityCallback<ArrayList<Movie>> activityCallback) {
        if (doc.title().contains("Just a moment")) {
            Logger.i(TAG, "Detected security check, needs cookie authentication");
            Movie m = createSecurityCheckMovie(searchContext, url);
            ArrayList<Movie> movieList = new ArrayList<>();
            movieList.add(m);
            if (activityCallback != null) {
                activityCallback.onInvalidCookie(movieList, getLabel());
            }
            return movieList;
        }
        return null;
    }

    private Movie createSecurityCheckMovie(String searchContext, String url) {
        String title = searchContext;
        String cardImageUrl = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
        String backgroundImageUrl = "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/bg.jpg";
        Movie m = new Movie();
        m.setTitle(title);
        m.setDescription("نتائج البحث في الاسفل...");
        m.setStudio(Movie.SERVER_AKWAM);
        m.setVideoUrl(url);
        m.setState(Movie.COOKIE_STATE);
        m.setCardImageUrl(cardImageUrl);
        m.setBackgroundImageUrl(backgroundImageUrl);
        m.setRate("");
        m.setSearchContext(searchContext);
        return m;
    }

    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return fetchSearchMovies(doc.baseUri(), doc);
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_AKWAM;
    }

    @Override
    protected String getSearchUrl(String query) {
        if (query.contains("http")) {
            return query;
        }
        String searchPart = "/search?q=";
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
        activityCallback.onInvalidCookie(clonedMovie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, clonedMovie);
    }

    @Override
    protected MovieFetchProcess fetchSeriesAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        Logger.d(TAG, "fetchSeriesAction: " + action);
        if (action == Movie.GROUP_OF_GROUP_STATE) {
            return fetchGroupOfGroup(movie, activityCallback);
        }
        return fetchGroup(movie, activityCallback);
    }

    @Override
    protected MovieFetchProcess fetchItemAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        Logger.d(TAG, "fetchItemAction: " + action);
        switch (action) {
            case Movie.BROWSER_STATE:
                return fetchBrowseItem(movie, activityCallback);
            case Movie.COOKIE_STATE:
                return fetchCookie(movie);
            case Movie.ACTION_WATCH_LOCALLY:
                return fetchWatchLocally(movie, activityCallback);
            case Movie.RESOLUTION_STATE:
                return fetchResolutions(movie, activityCallback);
            default:
                return fetchItem(movie, activityCallback);
        }
    }

    private MovieFetchProcess fetchWatchLocally(Movie movie, ActivityCallback<Movie> activityCallback) {
        if (movie.getState() == Movie.BROWSER_STATE) {
            activityCallback.onInvalidCookie(movie, getLabel());
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);
        }
        if (movie.getState() == Movie.RESOLUTION_STATE) {
            return fetchResolutions(movie, activityCallback);
        }
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_EXOPLAYER, movie);
    }

    private ArrayList<Movie> fetchSearchMovies(String searchContext, @NonNull Document doc) {
        ArrayList<Movie> movieList = new ArrayList<>();
        try {
            Elements links = doc.getElementsByClass("entry-box");
            for (Element link : links) {
                try {
                    Elements linkUrlElements = link.getElementsByClass("box");
                    if (linkUrlElements.isEmpty()) {
                        continue;
                    }
                    String linkUrl = linkUrlElements.attr("href");
                    if (linkUrl.contains("/movie") || linkUrl.contains("/series") || linkUrl.contains("/episode")) {
                        Movie movie = extractMovieFromElement(link, linkUrl, searchContext);
                        if (movie != null) {
                            movieList.add(movie);
                        }
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "Error processing search result item", e);
                }
            }

            Elements nextLinkNaviElements = doc.getElementsByAttribute("rel");
            for (Element naviElem : nextLinkNaviElements) {
                if ("next".equals(naviElem.attr("rel"))) {
                    String videoUrl = naviElem.attr("href");
                    Logger.d(TAG, "search: nextPage: " + videoUrl);
                    Movie nextPage = createNextPageMovie(videoUrl, searchContext);
                    movieList.add(nextPage);
                    break;
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error fetching search movies", e);
        }
        return movieList;
    }

    private Movie extractMovieFromElement(Element link, String linkUrl, String searchContext) {
        try {
            String rate = "";
            Elements rateElem = link.getElementsByClass("label rating");
            if (!rateElem.isEmpty()) {
                rate = rateElem.text();
            }

            Elements titleElem = link.getElementsByAttribute("src");
            String title = "";
            String cardImageUrl = "";
            String backgroundImageUrl = "";
            if (!titleElem.isEmpty()) {
                title = titleElem.attr("alt");
                cardImageUrl = titleElem.attr("data-src");
                backgroundImageUrl = titleElem.attr("data-src");
            }

            Movie movie = new Movie();
            movie.setTitle(title);
            movie.setDescription("");
            movie.setStudio(Movie.SERVER_AKWAM);
            movie.setVideoUrl(linkUrl);
            movie.setCardImageUrl(cardImageUrl);
            movie.setBackgroundImageUrl(backgroundImageUrl);
            movie.setState(isSeries(linkUrl) ? Movie.GROUP_STATE : Movie.ITEM_STATE);
            movie.setRate(rate);
            movie.setSearchContext(searchContext);
            movie.setMainMovie(movie);
            movie.setMainMovieTitle(linkUrl);
            return movie;
        } catch (Exception e) {
            Logger.e(TAG, "Error extracting movie from element", e);
            return null;
        }
    }

    private Movie createNextPageMovie(String videoUrl, String searchContext) {
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
        return nextPage;
    }

    @Override
    public int fetchNextAction(Movie movie) {
        Logger.d(TAG, "fetchNextAction: " + movie);
        switch (movie.getState()) {
            case Movie.GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY;
            case Movie.VIDEO_STATE:
                return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
    }

    private MovieFetchProcess fetchCookie(Movie movie) {
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
    }

    private MovieFetchProcess fetchGroupOfGroup(Movie movie, ActivityCallback<Movie> activityCallback) {
        Logger.i(TAG, "fetchGroupOfGroup: " + movie.getVideoUrl());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    private MovieFetchProcess fetchGroup(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Logger.i(TAG, "fetchGroup: " + movie.getVideoUrl());
        try {
            Document doc = ServerOptimizer.getDocumentWithCache(movie.getVideoUrl(), getConfig());
            if (doc == null) {
                Logger.w(TAG, "Failed to get document for URL: " + movie.getVideoUrl());
                if (activityCallback != null) {
                    activityCallback.onInvalidLink(movie);
                }
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
            }

            if (handleSecurityCheck(doc, movie, activityCallback)) {
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
            }

            extractDescription(doc, movie);
            extractBackgroundAndTrailer(doc, movie);

            Elements links = doc.select("a");
            for (Element link : links) {
                try {
                    if (link.attr("href").contains("/episode") && link.getElementsByAttribute("src").hasAttr("alt")) {
                        Movie episode = extractEpisodeMovie(link, movie);
                        safeAddToSublist(movie, episode);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "Error processing episode", e);
                }
            }

            if (activityCallback != null) {
                activityCallback.onSuccess(movie, getLabel());
            }
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
        } catch (Exception e) {
            Logger.e(TAG, "Error during fetchGroup operation", e);
            if (activityCallback != null) {
                activityCallback.onInvalidLink("Error: " + e.getMessage());
            }
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
        }
    }

    private void extractDescription(Document doc, Movie movie) {
        Elements decDivs = doc.select("h2");
        String description = "";
        for (Element div : decDivs) {
            String desc = div.getElementsByTag("p").html();
            if (desc != null && !desc.isEmpty()) {
                description = desc;
                break;
            }
        }
        if (!description.isEmpty()) {
            movie.setDescription(description);
        }
    }

    private void extractBackgroundAndTrailer(Document doc, Movie movie) {
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
            if (!bgImage.isEmpty()) {
                break;
            }
        }
        movie.setBackgroundImageUrl(bgImage);
        movie.setTrailerUrl(ytLink);
    }

    private Movie extractEpisodeMovie(Element link, Movie parentMovie) {
        Movie episode = Movie.clone(parentMovie);
        String title = link.getElementsByAttribute("src").attr("alt");
        String cardImageUrl = link.getElementsByAttribute("src").attr("src");
        String videoUrl = link.attr("href");
        episode.setTitle(title);
        episode.setVideoUrl(videoUrl);
        episode.setCardImageUrl(cardImageUrl);
        episode.setState(Movie.ITEM_STATE);
        return episode;
    }

    private MovieFetchProcess fetchItem(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Logger.i(TAG, "fetchItem: " + movie.getVideoUrl());
        try {
            Document doc = ServerOptimizer.getDocumentWithCache(movie.getVideoUrl(), getConfig());
            if (doc == null) {
                Logger.w(TAG, "Failed to get document for URL: " + movie.getVideoUrl());
                if (activityCallback != null) {
                    activityCallback.onInvalidLink(movie);
                }
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
            }

            if (handleSecurityCheck(doc, movie, activityCallback)) {
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
            }

            extractBackgroundAndTrailer(doc, movie);
            if (movie.getMainMovie() != null) {
                movie.getMainMovie().setTrailerUrl(movie.getTrailerUrl());
                movie.getMainMovie().setBackgroundImageUrl(movie.getBackgroundImageUrl());
            }

            extractDescription(doc, movie);

            Elements divs = doc.getElementsByClass("tab-content quality");
            for (Element div : divs) {
                try {
                    Movie resolution = extractResolutionMovie(div, movie);
                    safeAddToSublist(movie, resolution);
                } catch (Exception e) {
                    Logger.e(TAG, "Error processing resolution", e);
                }
            }

            if (activityCallback != null) {
                activityCallback.onSuccess(movie, getLabel());
            }
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
        } catch (Exception e) {
            Logger.e(TAG, "Error during fetchItem operation", e);
            if (activityCallback != null) {
                activityCallback.onInvalidLink("Error: " + e.getMessage());
            }
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
        }
    }

    private Movie extractResolutionMovie(Element div, Movie parentMovie) {
        String p2Caption = "/link/";
        Elements links = div.getElementsByAttribute("href");
        String title = "";
        String videoUrl = "";
        for (Element link : links) {
            if (link.attr("href").contains(p2Caption) || link.attr("href").contains("/download/")) {
                videoUrl = link.attr("href");
                title = link.text();
                break;
            }
        }
        if (videoUrl.isEmpty()) {
            return null;
        }
        Movie resolution = Movie.clone(parentMovie);
        resolution.setTitle(title);
        resolution.setDescription(parentMovie.getDescription());
        resolution.setVideoUrl(videoUrl);
        resolution.setBackgroundImageUrl(parentMovie.getBackgroundImageUrl());
        resolution.setState(Movie.RESOLUTION_STATE);
        return resolution;
    }

    private MovieFetchProcess fetchResolutions(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Logger.i(TAG, "fetchResolutions: " + movie.getVideoUrl());
        try {
            Movie resolution = Movie.clone(movie);
            String url = movie.getVideoUrl();
            Logger.d(TAG, "fetchResolutions: URL = " + url);

            if (!url.contains("/link")) {
                Logger.d(TAG, "fetchResolutions: URL doesn't contain /link/ to akwam download page: " + url);
            }

            Document doc = ServerOptimizer.getDocumentWithCache(url, getConfig());
            if (doc == null) {
                Logger.w(TAG, "Failed to get document for URL: " + url);
                if (activityCallback != null) {
                    activityCallback.onInvalidLink(movie);
                }
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
            }

            if (handleSecurityCheck(doc, movie, activityCallback)) {
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie);
            }

            url = findDownloadUrl(doc, url);
            Document doc2 = ServerOptimizer.getDocumentWithCache(url, getConfig());
            Movie movie2 = Movie.clone(movie);
            movie2.setVideoUrl(url);

            if (doc2 == null) {
                Logger.w(TAG, "Failed to get document for second URL: " + url);
                if (activityCallback != null) {
                    activityCallback.onInvalidLink(movie2);
                }
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie2);
            }

            if (handleSecurityCheck(doc2, movie2, activityCallback)) {
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, movie2);
            }

            Elements divs = doc2.getElementsByClass("btn-loader");
            if (!divs.isEmpty()) {
                String videoUrl = extractDirectDownloadUrl(divs);
                if (!videoUrl.isEmpty()) {
                    videoUrl = videoUrl + "|referer=" + getConfig().getReferer();
                    resolution.setVideoUrl(videoUrl);
                    resolution.setState(Movie.VIDEO_STATE);
                    resolution.setSubList(new ArrayList<>());
                    if (activityCallback != null) {
                        activityCallback.onSuccess(resolution, getLabel());
                    }
                    return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, resolution);
                }
            } else {
                Movie newMovie = Movie.clone(movie);
                newMovie.setVideoUrl(url);
                Logger.d(TAG, "fetchResolutions: Security check needed, URL: " + url);
                if (activityCallback != null) {
                    activityCallback.onInvalidCookie(newMovie, getLabel());
                }
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, newMovie);
            }
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
        } catch (Exception e) {
            Logger.e(TAG, "Error during fetchResolutions operation", e);
            if (activityCallback != null) {
                activityCallback.onInvalidLink("Error: " + e.getMessage());
            }
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
        }
    }

    private String findDownloadUrl(Document doc, String originalUrl) {
        String url = originalUrl;
        String regex = "(?:a[kwamoc])?.*/[download]{1,6}";
        Pattern pattern = Pattern.compile(regex);
        Elements links = doc.getElementsByClass("download-link");
        for (Element link : links) {
            String pLink = link.attr("href");
            Matcher matcher = pattern.matcher(pLink);
            if (matcher.find()) {
                Logger.d(TAG, "Found download URL in download-link: " + pLink);
                url = pLink;
                break;
            }
        }
        if (url.equals(originalUrl)) {
            links = doc.getElementsByTag("a");
            for (Element link : links) {
                String pLink = link.attr("href");
                Matcher matcher = pattern.matcher(pLink);
                if (matcher.find()) {
                    Logger.d(TAG, "Found download URL in anchor tag: " + pLink);
                    url = pLink;
                    break;
                }
            }
        }
        return url;
    }

    private String extractDirectDownloadUrl(Elements divs) {
        String regex = "(?:a[kwamoc])?.*/[download]{1,6}";
        Pattern pattern = Pattern.compile(regex);
        for (Element div : divs) {
            Elements links = div.getElementsByAttribute("href");
            for (Element link : links) {
                String pLink = link.attr("href");
                Matcher matcher = pattern.matcher(pLink);
                if (matcher.find()) {
                    Logger.i(TAG, "Found direct download URL: " + pLink);
                    return pLink;
                }
            }
        }
        return "";
    }

    public boolean isSeries(String url) {
        return url.contains("/series") || url.contains("/movies");
    }

    public boolean isSeries(Movie movie) {
        String u = movie.getVideoUrl();
        return isSeries(u);
    }

    @Override
    public int detectMovieState(Movie movie) {
        String u = movie.getVideoUrl();
        if (u.contains("/series") || u.contains("/movies")) {
            return Movie.GROUP_STATE;
        }
        return Movie.ITEM_STATE;
    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        int state = movie.getState();
        String script = "";
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_STARTED && state == Movie.RESOLUTION_STATE) {
            script = createDataExtractionScript(movie, ".btn-loader",
                    "let aElem = elements[0].getElementsByTagName('a');" +
                            "let extractedUrl = '';" +
                            "if(aElem.length > 0){" +
                            "    extractedUrl = aElem[0].getAttribute('href');" +
                            "}");
        }
        return script;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
        return search(getConfig().getUrl() + "/recent", activityCallback);
    }

    @Override
    public String getLabel() {
        return "أكوام";
    }

    @Override
    public MovieFetchProcess handleJSResult(String elementJson, ArrayList<Movie> movies, Movie movie) {
        Movie resultMovie = movies.isEmpty() ? movie : movies.get(0);
        resultMovie.setMainMovie(movie.getMainMovie());

        ServerConfig config = getConfig();
        Logger.d(TAG, "handleAkwamServer: resultActivity finish");
        String movieReferer = WEBSITE_URL;
        if (config != null) {
            config.setReferer(movieReferer);
            config.setUrl(movieReferer);
            ServerConfigManager.updateConfig(config);
        }
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_UPDATE_CONFIG_AND_RETURN_RESULT, resultMovie);
    }

    @Override
    public boolean shouldUpdateDomainOnSearchResult() {
        return false;
    }
}