package com.omerflex.server;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;

import com.omerflex.entity.Movie;
import com.omerflex.entity.dto.ServerConfig;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public abstract class AbstractServer {

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
        if (doc == null){
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
//                    .userAgent("Android 7")
//                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .timeout(16000)
                    .get();

        } catch (IOException e) {
            //builder.append("Error : ").append(e.getMessage()).append("\n");
            Log.i(getLabel(), "error: " + e.getMessage() + "");
        }
        return doc;
    }

    protected abstract String getSearchUrl(String query);

    public abstract String getLabel();

    /**
     * fetch a url and return its items
     *
     * @param movie Movie object to fetch its url
     */
    public Movie fetch(Movie movie) {
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

    public Movie fetchVideo(Movie movie) {
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

        return movie;
    }

    public abstract Movie fetchBrowseItem(Movie movie);

    /**
     * fetch next action
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    public int fetchNextAction(Movie movie) {
        if (movie == null) {
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

    public Movie fetchToWatchLocally(Movie movie){
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
    public abstract Movie fetchGroupOfGroup(Movie movie);

    /**
     * fetch a url and return its episode items
     *
     * @param movie Movie object to fetch its url if it's a group of episodes
     * @return
     */
    public abstract Movie fetchGroup(Movie movie);

    /**
     * fetch a url and return its serverLinks or resolution links
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    public abstract Movie fetchItem(Movie movie);

    public abstract void fetchWebResult(Movie movie);

    /**
     * fetch a url and return its serverLinks
     *
     * @param movie Movie object to fetch its url
     */
    public abstract void fetchServerList(Movie movie);

    /**
     * fetch a url and return its resolution links
     *
     * @param movie Movie object to fetch its url
     * @return
     */
    public abstract Movie fetchResolutions(Movie movie);

    /**
     * starts a video intent
     *
     * @param url String url to run video intent
     */
    public abstract void startVideo(String url);

    /**
     * starts a browserActivity
     *
     * @param url String url of the video
     */
    public abstract void startBrowser(String url);

    public abstract Movie fetchCookie(Movie movie);

    /**
     * check if movie url an item or group
     *
     * @param movie Movie object to check its url
     * @return true if is series link
     */
    public abstract boolean isSeries(Movie movie);

    public abstract void setCookies(String cookies);

    public abstract String getCookies();

    public abstract void setHeaders(Map<String, String> headers);

    public abstract Map<String, String> getHeaders();

    public abstract boolean onLoadResource(Activity activity, WebView view, String url, Movie movie);

    public abstract int detectMovieState(Movie movie);

    public abstract void setReferer(String referer);

    public abstract String getReferer();

    public abstract String getWebScript(int mode, Movie movie);

    public abstract void setConfig(ServerConfig serverConfig);

    public abstract ServerConfig getConfig();

    /**
     * @return ArrayList<Movie> of movies to be in homepage
     */
    public abstract ArrayList<Movie> getHomepageMovies();
}
