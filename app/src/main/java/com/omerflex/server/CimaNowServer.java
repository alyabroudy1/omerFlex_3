package com.omerflex.server;

import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class CimaNowServer extends AbstractServer{

    static String TAG = "CimaNowServer";
    static String WEBSITE_URL = "https://cimanow.cc";
    @Override
    public void shouldInterceptRequest(WebView view, WebResourceRequest request) {

    }

    @Override
    protected String getSearchUrl(String query) {
        if (query.contains("http")) {
            return query;
        }
        String searchPart = "/?s=";
        ServerConfig config = getConfig();
        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            return WEBSITE_URL + searchPart + query;
        }

        if (query.startsWith("/")) {
            return config.getUrl() + query;
        }

        return config.getUrl() + searchPart + query;
    }

    public ArrayList<Movie> search(String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Log.i(getLabel(), "search: " + query);
        String url = query;
        if (!query.contains("http")) {
            url = this.getSearchUrl(query);
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
            String title = query;
            //int imageResourceId = R.drawable.default_image;
            // String cardImageUrl = "android.resource://" + activity.getPackageName() + "/" + imageResourceId;
            String cardImageUrl = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
            String backgroundImageUrl = "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/bg.jpg";
            Movie m = new Movie();
            m.setTitle(title);
            m.setDescription("نتائج البحث في الاسفل...");
            m.setStudio(Movie.SERVER_CimaNow);
            m.setVideoUrl(url);
            //  m.setVideoUrl("https://www.google.com/");
            m.setState(Movie.COOKIE_STATE);
            // m.setState(Movie.RESULT_STATE);
            m.setCardImageUrl(cardImageUrl);
            m.setBackgroundImageUrl(backgroundImageUrl);
            m.setRate("");
            m.setSearchContext(query);
            movieList.add(m);

            activityCallback.onInvalidCookie(movieList, getLabel());
            return movieList;
        }
        movieList = generateSearchResultFromDoc(doc);
        activityCallback.onSuccess(movieList, getLabel());
        return movieList;
    }

    private ArrayList<Movie> generateSearchResultFromDoc(Document doc) {
        Elements articles = doc.select("article[aria-label=post]");

        ArrayList<Movie> movieList = new ArrayList<>();
        for (Element article : articles) {
            Movie movie = new Movie();

            // Extract the video URL from the anchor tag
            Element anchor = article.selectFirst("a");
            if (anchor != null) {
                movie.setVideoUrl(anchor.attr("href"));
            }

            // Extract the movie title and category
            Element titleElement = article.selectFirst("li[aria-label=title]");
            if (titleElement != null) {
                String titleText = titleElement.text();
                String[] parts = titleText.split("<em>");
                if (parts.length > 0) {
                    movie.setTitle(parts[0].trim());
                }
                if (parts.length > 1) {
                    movie.setGroup(parts[1].replace("</em>", "").trim());
                }
            }

            // Extract the card image URL from the img tag
            Element imgElement = article.selectFirst("img");
            if (imgElement != null) {
                movie.setCardImageUrl(imgElement.attr("data-src"));
//                Log.d(TAG, "generateSearchResultFromDoc: image: "+ imgElement.attr("src"));
            }

            movie.setStudio(Movie.SERVER_CimaNow);
            movie.setState(detectMovieState(movie));
            movie.setMainMovie(movie);
            movie.setMainMovieTitle(movie.getVideoUrl());

            movieList.add(movie);
        }
        Movie nextPage = generateNextPageMovie(doc);
        if (nextPage != null){
            movieList.add(nextPage);
        }
        return movieList;
    }

    private Movie generateNextPageMovie(Document doc) {
        // Find the pagination <ul>
        Movie nextPage = null;
        Element pagination = doc.selectFirst("ul[aria-label=pagination]");

        if (pagination != null) {
            // Find the active <li> element
            Element activePage = pagination.selectFirst("li.active");

            if (activePage != null) {
                // Get the next <li> after the active one
                Element nextPageElem = activePage.nextElementSibling();

                // Check if the next <li> contains an <a> tag and has a valid href attribute
                if (nextPageElem != null && nextPageElem.selectFirst("a") != null) {
                    String nextPageUrl = nextPageElem.selectFirst("a").attr("href");
                    nextPage = new Movie();
                    nextPage.setTitle("التالي");
                    nextPage.setDescription("0");
                    nextPage.setStudio(Movie.SERVER_CimaNow);
                    nextPage.setVideoUrl(nextPageUrl);
                    nextPage.setCardImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                    nextPage.setBackgroundImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                    nextPage.setState(Movie.NEXT_PAGE_STATE);
                    nextPage.setMainMovie(nextPage);
                    nextPage.setMainMovieTitle(nextPageUrl);
                    System.out.println("Next page URL: " + nextPageUrl);
                } else {
                    System.out.println("No next page found.");
                }
            } else {
                System.out.println("Active page not found.");
            }
        } else {
            System.out.println("Pagination not found.");
        }
        return nextPage;
    }

    @Override
    protected ArrayList<Movie> getSearchMovieList(Document doc) {
        return null;
    }

    @Override
    protected MovieFetchProcess fetchSeriesAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        if (action == Movie.GROUP_OF_GROUP_STATE) {
            return fetchGroupOfGroup(movie, activityCallback);
        }
        return fetchGroup(movie, activityCallback);
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

        // Find the seasons <section> by its aria-label attribute
        Element seasonsSection = doc.selectFirst("section[aria-label=seasons]");
        if (seasonsSection != null) {
            // Select all <li> elements within the <ul>
            Elements seasonItems = seasonsSection.select("ul li");

            // Iterate over each <li> element
            for (Element li : seasonItems) {
                Movie season = Movie.clone(movie);

                // Extract the link and the title from the <a> tag
                Element link = li.selectFirst("a");
                if (link != null) {
                    String videoUrl = link.attr("href").trim();
                    String title = link.text().trim();

                    // Set the movie properties
                    season.setVideoUrl(videoUrl);
                    season.setTitle(title);
                    season.setState(Movie.GROUP_STATE);

                    // Output the movie object
                    System.out.println(season);
                    movie.addSubList(season);
                }
            }
            activityCallback.onSuccess(movie, getLabel());
        } else {
            System.out.println("Seasons section not found.");
        }

//        return generateGroupOfGroupMovie(doc, movie, activityCallback);
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

        // Find the episodes section by its class "tabcontent active"
        Element episodesList = doc.selectFirst("ul.tabcontent.active");

        if (episodesList != null) {
            // Select all <li> elements inside the ul
            Elements episodeItems = episodesList.select("li");

            // Iterate over each <li> element
            for (Element li : episodeItems) {
                Movie episode = Movie.clone(movie);

                // Extract the link <a> element
                Element link = li.selectFirst("a");

                if (link != null) {
                    String videoUrl = link.attr("href").trim();
                    episode.setVideoUrl(videoUrl);

                    // Extract the image URLs from the <img> tags
                    Elements images = link.select("img");
                    if (images.size() > 0) {
                        // Assume the first <img> is the cardImageUrl
                        String cardImageUrl = images.get(0).attr("src").trim();
                        episode.setCardImageUrl(cardImageUrl);

                        // Assume the second <img> has the title in the alt attribute
                        if (images.size() > 1) {
                            String title = images.get(1).attr("alt").trim();
                            episode.setTitle(title);
                        }
                    }
                    episode.setState(Movie.ITEM_STATE);

                    // Output the episode object
                    System.out.println(episode);
                    movie.addSubList(episode);
                }
            }
            activityCallback.onSuccess(movie, getLabel());
        } else {
            System.out.println("No episodes found.");
        }

        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }


    @Override
    protected MovieFetchProcess fetchItemAction(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
//        Log.d(TAG, "fetchItemAction: 55");
        switch (action) {
//            case Movie.BROWSER_STATE:
//                return fetchBrowseItem(movie, activityCallback);
//            case Movie.COOKIE_STATE:
//                return fetchCookie(movie);
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



        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }


    @Override
    public int detectMovieState(Movie movie) {
        if (movie.getVideoUrl().contains("/selary")){
            return Movie.GROUP_OF_GROUP_STATE;
        }
        return Movie.ITEM_STATE;
    }

    @Override
    public String getWebScript(int mode, Movie movie) {
        return null;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
        return search(getConfig().getUrl() + "/الاحدث/", activityCallback);
//        return search(getConfig().getUrl() + "/category/المسلسلات", activityCallback);
    }

    @Override
    public String getLabel() {
        return "سيماناو";
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_CimaNow;
    }
}
