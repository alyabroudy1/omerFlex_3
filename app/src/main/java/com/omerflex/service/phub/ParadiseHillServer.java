package com.omerflex.service.phub;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.server.AbstractServer;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ParadiseHillServer extends AbstractServer {

    public static String TAG = "ParadiseHill";

    static String WEBSITE_URL = "https://en.paradisehill.cc";


    public ArrayList<Movie> search(String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
//       String url = "https://www.pornhub.com/view_video.php?viewkey=66493c4c967b4";
        Log.i(TAG, "search: " + query);
        String url = this.getSearchUrl(query);

        Document doc = this.getRequestDoc(url);
//        final String html = HttpRetriever.retrieve(url);
//        Document doc = Jsoup.parse(html);
        if (doc == null) {
            return null;
        }
        Log.d(TAG, "search: doc title: "+doc.title());
        return generateSearchMovieList(doc, activityCallback);
    }

    private ArrayList<Movie> generateSearchMovieList(Document doc, ActivityCallback<ArrayList<Movie>> activityCallback) {
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
        activityCallback.onSuccess(movieList, getLabel());
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
    protected String getSearchUrl(String query) {
//        String webName = "https://en.paradisehill.cc/";
        if (query.startsWith("http")){
            return query;
        }
        ServerConfig config = getConfig();

        String url = "";
        String query2 = query;
        if (query2.contains("ss=")){
            query2 = query2.replace("ss=","");
        }

        if (query2.contains("cat=")) {
            switch (query2) {
                case "cat=anal":
                    url = config.getUrl() + "/category/anal-sex/?sort=release";
                    break;
                case "cat=brazzers":
                    url = config.getUrl() + "/studio/89/?sort=release";
                    break;
                case "cat=pervcity":
                    url = config.getUrl() + "/studio/994/?sort=release";
                    break;
                case "cat=evilangel":
                    url = config.getUrl() + "/studio/16/?sort=release";
                    break;
                case "cat=elegentangel":
                    url = config.getUrl() + "/studio/12/?sort=release";
                    break;
                case "cat=realitykings":
                    url = config.getUrl() + "/studio/64/?sort=release";
                    break;
                case "cat=popular":
                    url = config.getUrl() + "/popular/?filter=all&sort=by_views";
                    break;
                case "cat=feature":
                    url = config.getUrl() + "/category/feature-films/?sort=created_at";
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
            url = config.getUrl() + "/actor/" + actNumber + "/?sort=release";
        } else {

            url = config.getUrl() + "/search/?pattern=" + query2+ "&what=1";
        }
        return url;
    }

    @Override
    protected MovieFetchProcess fetchSeriesAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        return fetchItem(movie, activityCallback);
    }

    @Override
    public String getLabel() {
        return "PDise";
    }


    @Override
    protected MovieFetchProcess fetchItemAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
//        Log.d(TAG, "fetchItemAction: 55");
        switch (action) {
//            case Movie.BROWSER_STATE:
////            case Movie.RESOLUTION_STATE:
//                return fetchBrowseItem(movie, activityCallback);
////            case Movie.COOKIE_STATE:
////                return fetchCookie(movie);
            case Movie.ACTION_WATCH_LOCALLY:
                return fetchWatchLocally(movie, activityCallback);
//            case Movie.RESOLUTION_STATE:
//                return fetchResolutions(movie, activityCallback);
            case Movie.VIDEO_STATE:
                return fetchVideo(movie, activityCallback);
            default:
                return fetchItem(movie, activityCallback);
        }
    }

    public MovieFetchProcess fetchVideo(Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.d(TAG, "fetchVideo: ");
//        Movie clonedMovie = Movie.clone(movie);
//        clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
        // to do nothing and wait till result returned to activity only the first fetch
//        return startWebForResultActivity(clonedMovie);
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_EXOPLAYER, movie);
    }

    private MovieFetchProcess fetchWatchLocally(Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.d(TAG, "fetchWatchLocally: ");
//        if (movie.getState() == Movie.BROWSER_STATE || movie.getState() == Movie.RESOLUTION_STATE) {
////            Movie clonedMovie = Movie.clone(movie);
////            clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);
////            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, clonedMovie);
//            activityCallback.onInvalidCookie(movie, getLabel());
//            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);
//        }
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_EXOPLAYER, movie);
    }

    private MovieFetchProcess fetchItem(Movie movie, ActivityCallback<Movie> activityCallback) {

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
        qualities.addAll(relatedVideos);
        movie.setSubList(qualities);
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }

    @Override
    public int fetchNextAction(Movie movie) {
        Log.d(TAG, "fetchNextAction: " + movie);
        switch (movie.getState()) {
            case Movie.GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY;
            case Movie.VIDEO_STATE:
                return VideoDetailsFragment.ACTION_OPEN_EXOPLAYER_ACTIVITY;
//           case Movie.RESOLUTION_STATE:
//               if (movie.getFetch() == 1 || movie.getFetch() == 0){
//                   return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
//               }
//                break;
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
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
    public int detectMovieState(Movie movie) {
        return 0;
    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        return null;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
        return search(getConfig().getUrl() + "/category/anal-sex/?sort=created_at", activityCallback);
    }
}
