package com.omerflex.server;

import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.ServerConfigManager;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractServer implements ServerInterface {

    private static final String TAG = "AbstractServer";

    @Override
    public ArrayList<Movie> search(String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Log.i(getLabel(), "search: " + query);
        String url = query;
        if (!query.contains("http")) {
            url = this.getSearchUrl(query);
        }

        Document doc = this.getRequestDoc(url);
        if (doc == null) {
            return null;
        }
        return this.getSearchMovieList(doc);
    }

    @Override
    public MovieFetchProcess fetch(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        if (movie == null) {
            Log.d(TAG, "fetch: invalid link");
            activityCallback.onInvalidLink("invalid link");
            return null;
        }
        switch (action) {
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.GROUP_STATE:
                Log.d(TAG, "fetch: fetchSeriesAction");
                return fetchSeriesAction(movie, action, activityCallback);
//            case Movie.ITEM_STATE:
//                return fetchItem(movie, Movie.ITEM_STATE);
//            case Movie.BROWSER_STATE:
//                return fetchItem(movie, Movie.BROWSER_STATE);
//            case Movie.RESOLUTION_STATE:
//                if (movie.getFetch() == Movie.REQUEST_CODE_MOVIE_UPDATE){
//                    return movie;
//                }
//                return fetchResolutions(movie);
//            case Movie.VIDEO_STATE:
//                return fetchVideo(movie);
//            case Movie.COOKIE_STATE:
//                return fetchCookie(movie);
            default:
                Log.d(TAG, "fetch: default fetchItemAction: "+ action+ "m: "+ movie);
              return fetchItemAction(movie, action, activityCallback);
        }
    }
    protected ServerConfig getConfig(){
        return ServerConfigManager.getConfig(getServerId());
    }
    public boolean shouldOverrideUrlLoading(Movie movie, WebView view, WebResourceRequest request){
        boolean result = false;
        String url = request.getUrl().toString();
        String newUrl = request.getUrl().toString().length() > 25 ? request.getUrl().toString().substring(0, 25) : request.getUrl().toString();

        if (getConfig() != null) {
            if (newUrl.contains(getConfig().getUrl())) {
                return false;
            }
            Log.d(TAG, "shouldOverrideUrlLoading:0 false: s: " + getConfig().getUrl() + ", u: " + url);
        }

        if (newUrl.contains(Util.extractDomain(movie.getVideoUrl(), false, false))) {
//                Log.d(TAG, "shouldOverrideUrlLoading:0 false: domain: " + Util.extractDomain(url, false, false) + ", u: " + url);
            return false;
        }

        if (url.contains("embed")) {
//                if (url.contains("embed") || sameSite) {
            view.loadUrl(url);
            Log.d(TAG, "shouldOverrideUrlLoading:1 false: " + url);
            return false;
        }


        if (movie.getState() == Movie.COOKIE_STATE || !Util.shouldOverrideUrlLoading(newUrl)) {
            if (url.startsWith("##")) {
                url = url.replace("##", "");
            }
            Log.d(TAG, "shouldOverrideUrlLoading:5 false: " + url);
            view.loadUrl(url);
            return true;
            //  CURRENT_WEB_NAME = getWebName(url);
        }


        return true;
    }
    /**
     * @param url request link
     * @return Document or null if an exception occurs
     */
    protected Document getRequestDoc(String url) {
        Document doc = null;
        ServerConfig config = getConfig();
        Log.d(TAG, "getRequestDoc: "+url);
        try {
            doc = Jsoup.connect(url)
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .headers(config.getHeaders())
                    .cookies(config.getMappedCookies())
//                    .userAgent("Android 7")
//                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
//                    .timeout(16000)
                    .timeout(0)
                    .get();

//            String docTitle = doc.title();
//            Log.d(TAG, "getRequestDoc: " + docTitle);
//            if (docTitle.contains("Just a moment")) {
//                return fetchDocUsingWebView(url);
//            }

        } catch (IOException e) {
            //builder.append("Error : ").append(e.getMessage()).append("\n");
            Log.i(TAG, "error: " + e.getMessage());
            String errorMessage = "error: " + getServerId() + ": " + e.getMessage();
        }
        return doc;
    }

    public int fetchNextAction(Movie movie) {
//        Log.d(TAG, "fetchNextAction: "+ (movie.getFetch() == Movie.REQUEST_CODE_MOVIE_UPDATE) );
        if (movie.getFetch() == Movie.REQUEST_CODE_MOVIE_UPDATE) {
            return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
        }
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY;
            case Movie.BROWSER_STATE:
                return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
        }
        return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
    }

    public String determineRelatedMovieLabel(Movie movie) {
        switch (movie.getState()){
            case Movie.GROUP_OF_GROUP_STATE:
                return "المواسم";
            case Movie.GROUP_STATE:
                return "الحلقات";
            case Movie.ITEM_STATE:
                return "الجودة";
            default:
                return "الروابط";
        }
    }

    public boolean shouldInterceptRequest(WebView view, WebResourceRequest request){
        return true;
    }

    protected abstract String getSearchUrl(String query);

    protected abstract ArrayList<Movie> getSearchMovieList(Document doc);

    protected abstract MovieFetchProcess fetchSeriesAction(Movie movie, int action, ActivityCallback<Movie> activityCallback);

    protected abstract MovieFetchProcess fetchItemAction(Movie movie, int action, ActivityCallback<Movie> activityCallback);
    public abstract int detectMovieState(Movie movie);

    public abstract String getWebScript(int mode, Movie movie);

    public String getCustomUserAgent(int state){
        String defaultUserAgent = "Android 7";
        switch (state){
            case Movie.COOKIE_STATE:
                return defaultUserAgent;
            default:
                return null;
        }
    }

    public MovieFetchProcess handleJSResult(String elementJson, List<Movie> movies, Movie movie){
        movie.setSubList(movies);
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_DETAILS_ACTIVITY_REQUIRE, movie);
    }

    public boolean shouldUpdateDomainOnSearchResult(){
        return true;
    }
}
