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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ParadiseHillServer extends AbstractServer {

    public static String TAG = "ParadiseHill";

    Activity activity;

    Fragment fragment;

    private static ParadiseHillServer instance;
    private ParadiseHillServer(Activity activity, Fragment fragment) {
        // Private constructor to prevent instantiation
        this.activity = activity;
        this.fragment = fragment;
    }

    public static synchronized ParadiseHillServer getInstance(Activity activity, Fragment fragment) {
        if (instance == null) {
            instance = new ParadiseHillServer(activity, fragment);
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

        Log.i(TAG, "title" + doc.title());
        //content of page one
        Elements divs = doc.getElementsByClass("item list-film-item");
        for (Element div : divs) {
            Elements items = div.getElementsByAttribute("itemprop");

            String title ="";
            String cardImageUrl = "";
            String description = "";

            for (Element item : items) {
                if (item.attr("itemprop").equals("name")) {
                    title = item.text();

                    Log.i("searchWeb", "name Found: " + title);
                }
                if (item.attr("itemprop").equals("image")) {
                    cardImageUrl = getConfig().getUrl() + "/" + item.attr("src");
                    Log.i("searchWeb", "image Found: " + cardImageUrl);
                }
                if (item.attr("itemprop").equals("genre")) {
                    description = item.text();
                    Log.i("searchWeb", "description Found: " + description);
                }
            }

            String videoUrl =  getConfig().getUrl() + "/" + div.child(0).attr("href");
            Log.i("searchWeb", "urFound: " + videoUrl);

            Movie movie = new Movie();
            movie.setTitle(title);
            movie.setVideoUrl(videoUrl);
            movie.setStudio(Movie.SERVER_PARADISE_HILL);
            movie.setState(Movie.ITEM_STATE);
            movie.setDescription("");
            movie.setCardImageUrl(cardImageUrl);
            movie.setBackgroundImageUrl(cardImageUrl);
            movie.setBgImageUrl(cardImageUrl);
            movie.setMainMovieTitle(videoUrl);
            movie.setMainMovie(movie);

            movieList.add(movie);

        }
        //pagination

        //Elements nextPages = doc.getElementsByTag("li");
        Elements nextPages = doc.getElementsByClass("next");
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
            movie.setStudio(Movie.SERVER_PARADISE_HILL);
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
        return Movie.SERVER_PARADISE_HILL;
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
        String webName = "https://en.paradisehill.cc/";

        if (query.startsWith("http")){
            return query;
        }

        String url = "";
        String query2 = query;
        if (query2.contains("ss=")){
            query2 = query2.replace("ss=","");
        }

        if (query2.contains("cat=")) {
            switch (query2) {
                case "cat=anal":
                    url = "https://en.paradisehill.cc/category/anal-sex/?sort=release";
                    break;
                case "cat=brazzers":
                    url = "https://en.paradisehill.cc/studio/89/?sort=release";
                    break;
                case "cat=pervcity":
                    url = "https://en.paradisehill.cc/studio/994/?sort=release";
                    break;
                case "cat=evilangel":
                    url = "https://en.paradisehill.cc/studio/16/?sort=release";
                    break;
                case "cat=elegentangel":
                    url = "https://en.paradisehill.cc/studio/12/?sort=release";
                    break;
                case "cat=realitykings":
                    url = "https://en.paradisehill.cc/studio/64/?sort=release";
                    break;
                case "cat=popular":
                    url = "https://en.paradisehill.cc/popular/?filter=all&sort=by_views";
                    break;
            }
        } else if (query2.contains("acct=")) {
            String actNumber = "";
            switch (query2) {
                case "acct=belladonna":
                    actNumber = "16670";
                    break;
                case "acct=lisa ann":
                    actNumber = "16779";
                    break;
                case "acct=adriana":
                    actNumber = "20851";
                    break;
                case "acct=bonnie":
                    actNumber = "24767";
                    break;
                case "acct=sasha":
                    actNumber = "16679";
                    break;
                case "acct=phoenix":
                    actNumber = "16719";
                    break;
                case "acct=rocco":
                    actNumber = "17549";
                    break;
                case "acct=megan":
                    actNumber = "26123";
                    break;
                case "acct=asa":
                    actNumber = "18739";
                    break;
                case "acct=remy":
                    actNumber = "24989";
                    break;
                case "acct=amy":
                    actNumber = "16864";
                    break;
                case "acct=holly":
                    actNumber = "27402";
                    break;
            }
            url = "https://en.paradisehill.cc/actor/" + actNumber + "/?sort=release";
        } else {

            url = "https://en.paradisehill.cc/search/?pattern=" + query2+ "&what=1";
        }
        return url;
    }

    @Override
    public String getLabel() {
        return "PDise";
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

        Document doc = this.getRequestDoc(movie.getVideoUrl());
//        final String html = HttpRetriever.retrieve(url);
//        Document doc = Jsoup.parse(html);
        if (doc == null) {
            return null;
        }

        ArrayList<Movie> qualities = fetchMovieQualities( movie);
        ArrayList<Movie> relatedVideos = fetchRelatedMovies(movie);
//        Log.d(TAG, "fetchItem: fetchMovieQualities: "+ qualities);
        Log.d(TAG, "fetchItem: relatedVideos: "+ relatedVideos);
        Collections.reverse(qualities);
        movie.setSubList(qualities);
        return movie;
    }

    public ArrayList<Movie> fetchMovieQualities(Movie movie) {
        Document doc = this.getRequestDoc(movie.getVideoUrl());
//        final String html = HttpRetriever.retrieve(url);
//        Document doc = Jsoup.parse(html);
        ArrayList<Movie> movieList = new ArrayList<>();
        if (doc == null) {
            return movieList;
        }
        Elements scripts = doc.getElementsByTag("script");
        for (Element script: scripts) {
//            Log.d(TAG, "run: scripts: " + script.html());
            if (script.html().contains("sources")){
                Pattern pattern = Pattern.compile("\"src\":\"(.*?)\"");
                Matcher matcher = pattern.matcher(script.html());

                int partCounter = 1;

                while (matcher.find()) {
                    String sourceUrl = matcher.group(1);
                    if (sourceUrl != null){
                        sourceUrl = sourceUrl.replace("\\", "");
//                        Log.d(TAG, "run: xxx: " + sourceUrl);


                        Movie resolution = Movie.clone(movie);
                        resolution.setTitle("part_"+partCounter++);
                        resolution.setVideoUrl(sourceUrl);
                        resolution.setStudio(Movie.SERVER_PARADISE_HILL);
                        resolution.setState(Movie.VIDEO_STATE);

                        movieList.add(resolution);
                    }
                    // Do something with the source URL, for example, print it
                }
            }
        }
        return movieList;
    }

    public ArrayList<Movie> fetchRelatedMovies(Movie movie) {

        ArrayList<Movie> relatedVideos = new ArrayList<>();
        String webName = "https://en.paradisehill.cc/";
        String url = movie.getVideoUrl();
        Document doc = this.getRequestDoc(movie.getVideoUrl());
//        final String html = HttpRetriever.retrieve(url);
//        Document doc = Jsoup.parse(html);
        if (doc == null) {
            return relatedVideos;
        }

            //content of page one
            Elements divs = doc.getElementsByClass("item list-film-item");
            Log.i("searchWeb", "urFound: " + url);
            for (Element div : divs) {
                Elements items = div.getElementsByAttribute("itemprop");

                String title ="";
                String cardImageUrl = "";
                String description = "";

                for (Element item : items) {
                    if (item.attr("itemprop").equals("name")) {
                        title = item.text();

                        Log.i("searchWeb", "name Found: " + title);
                    }
                    if (item.attr("itemprop").equals("image")) {
                        cardImageUrl =webName + item.attr("src");
                        Log.i("searchWeb", "image Found: " + cardImageUrl);
                    }
                    if (item.attr("itemprop").equals("genre")) {
                        description = item.text();
                        Log.i("searchWeb", "description Found: " + description);
                    }
                }

                String videoUrl = webName + div.child(0).attr("href");

            Movie mov = Movie.clone(movie);
            mov.setState(Movie.ITEM_STATE);
            mov.setTitle(title);
            mov.setDescription(description);
            mov.setVideoUrl(videoUrl);
            if (!cardImageUrl.isEmpty()){
                mov.setCardImageUrl(cardImageUrl);
            }

            relatedVideos.add(mov);
        }
        return relatedVideos;
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
        return search(getConfig().getUrl() + "/category/anal-sex/?sort=created_at");
    }

}
