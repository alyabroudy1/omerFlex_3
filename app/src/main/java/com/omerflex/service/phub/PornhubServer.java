package com.omerflex.service.phub;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import com.omerflex.entity.Movie;
import com.omerflex.server.AbstractServer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;


public class PornhubServer extends AbstractServer {

    public static String TAG = "PornhubServer";

    Activity activity;

    Fragment fragment;

    private static PornhubServer instance;
    private PornhubServer(Activity activity, Fragment fragment) {
        // Private constructor to prevent instantiation
        this.activity = activity;
        this.fragment = fragment;
    }

    public static synchronized PornhubServer getInstance(Activity activity, Fragment fragment) {
        if (instance == null) {
            instance = new PornhubServer(activity, fragment);
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

    public ArrayList<Movie> search(String query) {
//       String url = "https://www.pornhub.com/view_video.php?viewkey=66493c4c967b4";
        Log.i(getLabel(), "search: " + query);
        String url = this.getSearchUrl(query);

        Document doc = this.getRequestDoc(url);
//        final String html = HttpRetriever.retrieve(url);
//        Document doc = Jsoup.parse(html);
        if (doc == null) {
            return null;
        }
        Log.d(TAG, "search: doc title: "+doc.title());
        return generateSearchMovieList(doc);
    }

    private ArrayList<Movie> generateSearchMovieList(Document doc) {
        ArrayList<Movie> movieList = new ArrayList<>();

        Element ul = doc.getElementById("videoSearchResult");

//        Log.d(TAG, "run:classs "+ul);

        // Elements li = ul.getElementsByAttribute("data-webm");
        Elements li = null;
        if (ul == null){
            li = doc.getElementsByClass("pcVideoListItem");
        }else {
            li = ul.getElementsByTag("li");
        }
        String title = "";
        String image = "";
        String videoUrl = "";
        Log.d(TAG, "run: size::"+ li.size());
        for (Element item: li){
            // title = item.attr("data-title");
            Elements links = item.getElementsByAttribute("data-video-vkey");

            for (Element link: links){
                videoUrl = "https://www.pornhub.com/view_video.php?viewkey="+ link.attr("data-video-vkey");
                Log.d("TAG", "search: url found: "+videoUrl);
            }
            Elements imageElems = item.getElementsByTag("img");
            if (!imageElems.isEmpty()){
                image = imageElems.get(0).attr("src");
                if (image == null || image.isEmpty()){
                    image = imageElems.get(0).attr("data-src");
                }
            }
            title = item.getElementsByTag("img").attr("alt");
            Log.d("TAG", "search: image found: "+image);
            Log.d("TAG", "search: title found: "+title);

            if (title == null || title.isEmpty()){
                continue;
            }

//            final Movie m = Movie.buildMovieInfo(title, description, Movie.SERVER_PORN_HUB, videoUrl, image, image, Movie.ITEM_STATE,"", videoUrl, title, Movie.ITEM_STATE);
            Movie movie = new Movie();
            movie.setTitle(title);
            movie.setVideoUrl(videoUrl);
            movie.setStudio(Movie.SERVER_PORN_HUB);
            movie.setState(Movie.ITEM_STATE);
            movie.setDescription("");
            movie.setCardImageUrl(image);
            movie.setBackgroundImageUrl(image);
            movie.setBgImageUrl(image);
            movie.setMainMovieTitle(videoUrl);
            movie.setMainMovie(movie);

            movieList.add(movie);
//            activity.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    //Collections.reverse(LinkSeriesActivity.seriesMovieList);
//                    listRowAdapter.add(
//                            m
//                    );
//                }});
        }

        Elements nextPages = doc.getElementsByClass("page_next");
        String nextPageUrl = null;
        if (!nextPages.isEmpty()){
            Element nextPageElem = nextPages.get(0);
            Elements nextLinkElems = nextPageElem.getElementsByTag("a");
            if (!nextLinkElems.isEmpty()){
                Element nextLinkElem = nextLinkElems.get(0);
                nextPageUrl = nextLinkElem.attr("href");
                if (nextPageUrl != null){
                    nextPageUrl = getConfig().getUrl() + nextPageUrl;

                    Movie movie = new Movie();
                    movie.setTitle("next");
                    movie.setVideoUrl(nextPageUrl);
                    movie.setStudio(Movie.SERVER_PORN_HUB);
                    movie.setState(Movie.NEXT_PAGE_STATE);
                    movie.setDescription("0");
                    movie.setCardImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                    movie.setBackgroundImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");

                    movieList.add(movie);
                }
            }
        }
        return movieList;
    }

    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return null;
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_PORN_HUB;
    }

    @Override
    protected Fragment getFragment() {
        return null;
    }

    @Override
    protected Activity getActivity() {
        return activity;
    }

    @Override
    protected String getSearchUrl(String query) {
        if (query.startsWith("http")){
            return query;
        }
        return getConfig().getUrl()+"/video/search?search="+query;
    }

    @Override
    public String getLabel() {
        return "PHub";
    }

    @Override
    public Movie fetchBrowseItem(Movie movie) {
        return null;
    }

    @Override
    public Movie fetchGroupOfGroup(Movie movie) {
        return null;
    }

    @Override
    public Movie fetchGroup(Movie movie) {
        return null;
    }

    @Override
    public Movie fetchItem(Movie movie) {

        ArrayList<Movie> qualities = fetchMovieQualities(movie);
        ArrayList<Movie> relatedVideos = fetchRelatedMovies(movie);
//        Log.d(TAG, "fetchItem: fetchMovieQualities: "+ qualities);
        Log.d(TAG, "fetchItem: relatedVideos: "+ relatedVideos);
        Collections.reverse(qualities);
        movie.setSubList(qualities);
        return movie;
    }

    public ArrayList<Movie> fetchRelatedMovies( Movie movie) {
//        Log.d(TAG, "fetchRelatedMovies html: "+ html);
        Document doc = getRequestDoc(movie.getVideoUrl());
        if (doc == null) {
            Log.d(TAG, "fetchRelatedMovies doc null");
            return null;
        }
//        Log.d(TAG, "fetchRelatedMovies doc title: "+doc.title());
        Log.d(TAG, "fetchRelatedMovies doc url: "+movie.getVideoUrl());
        ArrayList<Movie> relatedVideos = new ArrayList<>();

        Element ul = doc.getElementById("relatedVideos");
        Log.d(TAG, "fetchRelatedMovies: ul: "+ul);
       Elements lis = doc.getElementsByTag("li");
        String title = "";
        String image = "";
        String videoUrl = "";
        for (Element li : lis) {
            if (!li.hasClass("pcVideoListItem")){
                continue;
            }
            Elements links = li.getElementsByAttribute("data-video-vkey");

            for (Element link: links){
                if (link.attr("data-video-vkey") == null){
                    continue;
                }
                videoUrl = "https://www.pornhub.com/view_video.php?viewkey="+ link.attr("data-video-vkey");
                Log.d("TAG", "search: url found: "+videoUrl);
                break;
            }
            image = li.getElementsByTag("img").attr("data-src");
            title = li.getElementsByTag("img").attr("alt");

            if (videoUrl.isEmpty() || null == title){
                continue;
            }
            Movie mov = Movie.clone(movie);
            mov.setState(Movie.ITEM_STATE);
            mov.setTitle(title);
            mov.setVideoUrl(videoUrl);
            if (null != image && !image.isEmpty()){
                mov.setCardImageUrl(image);
            }

            relatedVideos.add(mov);
            Log.d(TAG, "run: size::"+ title);
        }
//       Elements li = doc.getElementsByClass("pcVideoListItem");


        return relatedVideos;
    }

    public ArrayList<Movie> fetchMovieQualities(Movie movie) {

        Log.d(TAG, "sendRequest: html-1: ");
        final String html = HttpRetriever.retrieve(movie.getVideoUrl());
        return PornhubParser.getGeneralList(html, movie);
    }

    @Override
    public void fetchWebResult(Movie movie) {

    }

    @Override
    public void fetchServerList(Movie movie) {

    }

    @Override
    public Movie fetchResolutions(Movie movie) {
        return null;
    }

    @Override
    public void startVideo(String url) {

    }

    @Override
    public void startBrowser(String url) {

    }

    @Override
    public Movie fetchCookie(Movie movie) {
        return null;
    }

    @Override
    public boolean isSeries(Movie movie) {
        return false;
    }

    @Override
    public boolean onLoadResource(Activity activity, WebView view, String url, Movie movie) {
        return false;
    }

    @Override
    public int detectMovieState(Movie movie) {
        return 0;
    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        return null;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies() {
        return search(getConfig().getUrl() +"/video?c=35&o=mv");
    }

}
