package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.ServerConfigManager;
import com.omerflex.service.database.MovieDbHelper;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.DetailsActivity;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractServer_old implements ServerInterface {

    private static final String TAG = "AbstractServer";
    private boolean cookieRefreshed = false;

    /**
     * Search for the query and add the result to movieList
     *
     * @param query name to search for
     * @return
     */
    public ArrayList<Movie> search(String query) {
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

    protected abstract ArrayList<Movie> getSearchMovieList(Document doc);

    protected Document getRequestDoc(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .headers(this.getHeaders())
                    .cookies(this.getMappedCookies())
//                    .userAgent("Android 7")
//                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .timeout(16000)
                    .get();

//            String docTitle = doc.title();
//            Log.d(TAG, "getRequestDoc: " + docTitle);
//            if (docTitle.contains("Just a moment")) {
//                return fetchDocUsingWebView(url);
//            }

        } catch (IOException e) {
            //builder.append("Error : ").append(e.getMessage()).append("\n");
            Log.i(TAG, "error: " + e.getMessage());
            String errorMessage = "error: " +getServerId() + ": "+e.getMessage();
            Util.showToastMessage(errorMessage, getActivity());
        }
        return doc;
    }


    protected Document getRequestDoc(String url, Movie movie, int operation) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .headers(this.getHeaders())
//                    .userAgent("Android 7")
//                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .timeout(16000)
                    .get();

            String docTitle = doc.title();
            Log.d(TAG, "getRequestDoc: " + docTitle);
            if (docTitle.contains("Just a moment")) {
                return fetchDocUsingWebView(url, movie, operation);
            }

        } catch (IOException e) {
            //builder.append("Error : ").append(e.getMessage()).append("\n");
            Log.i(TAG, "error: " + e.getMessage());
        }
        return doc;
    }

    private Document fetchDocUsingWebView(String url, Movie selectedMovie, int operation) {
        Log.d(TAG, "fetchDocUsingWebView: " + url);
        if (getActivity() != null && getFragment() != null) {

            Movie movie = Movie.clone(selectedMovie);
            movie.setVideoUrl(url);
            movie.setStudio(getServerId());
//            movie.setState(Movie.HTML_STATE);
            movie.setVideoUrl(url);
            movie.setFetch(Movie.REQUEST_CODE_FETCH_HTML);

            Intent browse = new Intent(getActivity(), BrowserActivity.class);
            browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//            browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
            Log.d(TAG, "getResultFromWeb: activity:" + getFragment().getClass().getName());
            //activity.startActivity(browse);
            getFragment().startActivityForResult(browse, movie.getFetch());
        }
        // return null to stop further steps and let webview return the result to the activity
        return null;
    }

    public abstract String getServerId();

    protected abstract Fragment getFragment();

    protected abstract Activity getActivity();

    protected abstract String getSearchUrl(String query);

    public abstract String getLabel();

    /**
     * fetch a url and return its items
     *
     * @param movie Movie object to fetch its url
     */
    public MovieFetchProcess fetch(Movie movie) {
        if (movie == null) {
            return null;
        }
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
                return fetchGroupOfGroup(movie);
            case Movie.GROUP_STATE:
                return fetchGroup(movie);
            case Movie.ITEM_STATE:
                return fetchItem(movie);
            case Movie.BROWSER_STATE:
                return fetchBrowseItem(movie);
            case Movie.RESOLUTION_STATE:
                return fetchResolutions(movie);
            case Movie.VIDEO_STATE:
                return fetchVideo(movie);
            case Movie.COOKIE_STATE:
                return fetchCookie(movie);
            default:
                return null;
        }
    }

    public MovieFetchProcess fetch(Movie movie, int action) {
        if (movie == null) {
            return null;
        }
        switch (movie.getState()) {
            case Movie.GROUP_OF_GROUP_STATE:
                return fetchGroupOfGroup(movie);
            case Movie.GROUP_STATE:
                return fetchGroup(movie);
            case Movie.ITEM_STATE:
                return fetchItem(movie);
            case Movie.BROWSER_STATE:
                return fetchBrowseItem(movie);
            case Movie.RESOLUTION_STATE:
                return fetchResolutions(movie);
            case Movie.VIDEO_STATE:
                return fetchVideo(movie);
            case Movie.COOKIE_STATE:
                return fetchCookie(movie);
            default:
                return null;
        }
    }

    protected MovieFetchProcess fetchVideo(Movie movie) {
//        if (movie != null && movie.getVideoUrl() != null) {
//            String type = "video/*"; // It works for all video application
//            String url = movie.getVideoUrl();
//            url = url.trim().replace(" ", "");
//            //  url = url.replace("/video.mp4", "");
//            Uri uri = Uri.parse(url + "");
//            Intent videoIntent = new Intent(Intent.ACTION_VIEW, uri);
//            videoIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            //  in1.setPackage("org.videolan.vlc");
//            videoIntent.setDataAndType(uri, type);
//            Log.i("video started", uri.toString() + "");
//        }

        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    protected abstract MovieFetchProcess fetchBrowseItem(Movie movie);

    /**
     * fetch next action
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    public int fetchNextAction(Movie movie) {
        Log.d(TAG, "fetchNextAction: "+ (movie.getFetch() == Movie.REQUEST_CODE_MOVIE_UPDATE) );
        if (movie == null || movie.getFetch() == Movie.REQUEST_CODE_MOVIE_UPDATE) {
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

    protected Movie fetchToWatchLocally(Movie movie) {
        Movie resolution = Movie.clone(movie);
        resolution.setState(Movie.VIDEO_STATE);
        if (movie.getSubList() == null) {
            movie.setSubList(new ArrayList<>());
        }
        movie.addSubList(resolution);
        return movie;
    }

    /**
     * fetch a url and return its group item
     *
     * @param movie Movie object to fetch its url if it's a series of group
     * @return
     */
    protected abstract MovieFetchProcess fetchGroupOfGroup(Movie movie);

    /**
     * fetch a url and return its episode items
     *
     * @param movie Movie object to fetch its url if it's a group of episodes
     * @return
     */
    protected abstract MovieFetchProcess fetchGroup(Movie movie);

    /**
     * fetch a url and return its serverLinks or resolution links
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    protected abstract MovieFetchProcess fetchItem(Movie movie);

    protected abstract void fetchWebResult(Movie movie);

    /**
     * fetch a url and return its serverLinks
     *
     * @param movie Movie object to fetch its url
     */
    protected abstract void fetchServerList(Movie movie);

    /**
     * fetch a url and return its resolution links
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    protected abstract MovieFetchProcess fetchResolutions(Movie movie);

    /**
     * starts a video intent
     *
     * @param url String url to run video intent
     */
    protected abstract void startVideo(String url);

    /**
     * starts a browserActivity
     *
     * @param url String url of the video
     */
    protected abstract void startBrowser(String url);

    protected abstract MovieFetchProcess fetchCookie(Movie movie);

    /**
     * check if movie url an item or group
     *
     * @param movie Movie object to check its url
     * @return true if is series link
     */
    protected abstract boolean isSeries(Movie movie);

    public void setCookies(String cookies){
        ServerConfig config = getConfig();
        if (config != null){
            config.setStringCookies(cookies);
            ServerConfigManager.updateConfig(config);
        }
    }

    public String getCookies(){
        if (getConfig() != null){
            return getConfig().getStringCookies();
        }
        return "";
    }

    public Map<String, String> getMappedCookies(){
        if (getConfig() != null){
            return getConfig().getMappedCookies();
        }
        return new HashMap<>();
    }

    public void setHeaders(Map<String, String> headers){
        ServerConfig config = getConfig();
        if (config != null){
            config.setHeaders(headers);
            ServerConfigManager.updateConfig(config);
        }
    };

    public Map<String, String> getHeaders(){
        if (getConfig() != null){
//            Log.d(TAG, "getHeaders: "+getConfig());
            return getConfig().getHeaders();
        }
        return new HashMap<>();
    }

    public abstract boolean onLoadResource(Activity activity, WebView view, String url, Movie movie);

    public abstract int detectMovieState(Movie movie);

    public void setReferer(String referer){
        ServerConfig config = getConfig();
        if (config != null){
            config.setReferer(referer);
            ServerConfigManager.updateConfig(config);
        }
    }

    public String getReferer(){
        if (getConfig() != null){
            return getConfig().getReferer();
        }
        return null;
    }

    public abstract String getWebScript(int mode, Movie movie);

    public void setConfig(ServerConfig serverConfig){
        ServerConfig config = getConfig();
        if (config != null){
            ServerConfigManager.updateConfig(serverConfig);
        }else {
            ServerConfigManager.addConfig(serverConfig);
        }
    }

    public ServerConfig getConfig(){
        return ServerConfigManager.getConfig(getServerId());
    }

    /**
     * @return ArrayList<Movie> of movies to be in homepage
     */
    public abstract ArrayList<Movie> getHomepageMovies();

    public void handleJSWebResult(Activity activity, Movie movie, String jsResult) {
        Log.d(TAG, "handleJSWebResult - "+movie);

            Intent intent = new Intent();
//            intent.putExtra("result", jsResult);
            intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);

            intent.putExtra(DetailsActivity.MOVIE_SUBLIST, (Serializable) jsResult);
            activity.setResult(Activity.RESULT_OK, intent);
            Log.d(TAG, "handleJSWebResult - 2" );
            activity.finish();
    }

    public Movie handleOnActivityResultHtml(String html, Movie m) {
        Movie movie = Movie.clone(m);
        Document doc = Jsoup.parse(html);

        return movie;
    }

    public void shouldInterceptRequest(WebView view, WebResourceRequest request, MovieDbHelper dbHelper) {
//        String url = "https://wecima.show/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-fox-spirit-matchmaker-love-in-pavilion-%d9%85%d9%88%d8%b3%d9%85-1-%d8%ad%d9%84%d9%82%d8%a9-10/";

        ServerConfig config = getConfig();
        if (config == null || config.getUrl() == null){
            return;
        }

        String cookieTest = CookieManager.getInstance().getCookie(config.getUrl());
        if (cookieTest == null){
            return;
        }
            //                String url = "https://www.faselhd.link/account/login";
            Connection connection = Jsoup.connect(config.getUrl());//.sslSocketFactory(getSSLSocketFactory());
            connection.ignoreHttpErrors(true);
            connection.ignoreContentType(true);
            connection.headers(request.getRequestHeaders());
            // String cookie = CookieManager.getInstance().getCookie("https://shahid4uu.cam");
            connection.cookies(Util.getMapCookies(cookieTest));


            // new cookie already refreshed
            if (isCookieRefreshed()){
                Log.d(TAG, "shouldInterceptRequest: refresh cookie: refreshed");
                return;
            }

            Document doc = null;
            try {
                doc = connection.get();
                String title = doc.title();
                Log.d(TAG, "testCookie shouldInterceptRequest: cookie test title:"+title);
                if (!title.contains("moment")){
                    Log.d(TAG, "testCookie shouldInterceptRequest: success headers:"+request.getRequestHeaders().toString());
                    Log.d(TAG, "testCookie shouldInterceptRequest: success cookies:"+cookieTest);
                    setCookies(cookieTest);
                    setHeaders(request.getRequestHeaders());
//                    dbHelper.saveHeadersAndCookies(this, getServerId(), getConfig());
                    setCookieRefreshed(true);
                }
            } catch (IOException e) {
//            throw new RuntimeException(e);
                //Document doc = Jsoup.parse(htmlContent);
                Log.d(TAG, "testCookie: error: "+e.getMessage());

            }

    }

    public boolean isCookieRefreshed() {
        return cookieRefreshed;
    }

    public void setCookieRefreshed(boolean refreshed) {
        this.cookieRefreshed = refreshed;
    }
}
