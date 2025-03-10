package com.omerflex.server;

import android.util.Log;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.view.BrowserActivity;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class CimaNowServer extends AbstractServer{

    static String TAG = "CimaNowServer";
    static String WEBSITE_URL = "https://cimanow.cc";

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
        Document doc = this.getSearchRequestDoc(url);
        if (doc == null) {
            activityCallback.onInvalidLink("Invalid link");
            return null;
        }
        ArrayList<Movie> movieList = new ArrayList<>();

        if (doc.title().contains("moment")) {
//        if (true) {
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
            m.setBgImageUrl(backgroundImageUrl);
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
        if (articles.isEmpty()){
            articles = doc.select("a:has(li[aria-label=title])");
        }
//        Log.d(TAG, "generateSearchResultFromDoc: "+doc.body().toString());
//        Log.d(TAG, "generateSearchResultFromDoc: articles: "+articles.size());

        ArrayList<Movie> movieList = new ArrayList<>();
        for (Element article : articles) {
            Movie movie = new Movie();

            // Extract the video URL from the anchor tag
            Element anchor = article.selectFirst("a");
            String title = anchor.attr("href");
//            Log.d(TAG, "generateSearchResultFromDoc: article: "+anchor);
            if (title == null) {
                if (article.attr("href") == null){
                    continue;
                }
                title = article.attr("href");
            }
            movie.setVideoUrl(title);

            // Extract the movie title and category
            Element titleElement = article.selectFirst("li[aria-label=title]");
            if (titleElement != null) {
                String titleText = titleElement.text();
                String[] parts = titleText.split("<em>");
                if (parts.length > 0) {
                    Element episodeElement = article.selectFirst("li[aria-label=episode]");
                    String episode = (episodeElement != null) ? episodeElement.text().replace("الحلقة", "").trim() : "";

                    movie.setTitle( "الحلقة " + episode + " " + parts[0].trim());
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
        Element episodesList = doc.selectFirst("ul.tabcontent");

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
            case Movie.BROWSER_STATE:
//            case Movie.RESOLUTION_STATE:
                return fetchBrowseItem(movie, activityCallback);
//            case Movie.COOKIE_STATE:
//                return fetchCookie(movie);
            case Movie.ACTION_WATCH_LOCALLY:
                return fetchWatchLocally(movie, activityCallback);
            case Movie.RESOLUTION_STATE:
                return fetchResolutions(movie, activityCallback);
//            case Movie.VIDEO_STATE:
//                return fetchVideo(movie);
            default:
                return fetchItem(movie, activityCallback);
        }
    }
    public MovieFetchProcess fetchResolutions(Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.d(TAG, "fetchResolutions: ");
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
        // to do nothing and wait till result returned to activity only the first fetch
//        return startWebForResultActivity(clonedMovie);
        activityCallback.onInvalidCookie(clonedMovie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, clonedMovie);
    }
    public MovieFetchProcess fetchBrowseItem(Movie movie, ActivityCallback<Movie> activityCallback) {
        Movie clonedMovie = Movie.clone(movie);
        clonedMovie.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
        // to do nothing and wait till result returned to activity only the first fetch
//        return startWebForResultActivity(clonedMovie);
        activityCallback.onInvalidCookie(clonedMovie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE, clonedMovie);
    }

    private MovieFetchProcess fetchWatchLocally(Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.d(TAG, "fetchWatchLocally: ");
        if (movie.getState() == Movie.BROWSER_STATE || movie.getState() == Movie.RESOLUTION_STATE) {
//            Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_EXOPLAYER);
//            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, clonedMovie);
            activityCallback.onInvalidCookie(movie, getLabel());
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);
        }
        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_EXOPLAYER, movie);
    }

    @Override
    public int fetchNextAction(Movie movie) {
        Log.d(TAG, "fetchNextAction: " + movie);
        switch (movie.getState()) {
            case Movie.GROUP_STATE:
            case Movie.ITEM_STATE:
                return VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY;
            case Movie.VIDEO_STATE:
                return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
//           case Movie.RESOLUTION_STATE:
//               if (movie.getFetch() == 1 || movie.getFetch() == 0){
//                   return VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY;
//               }
//                break;
        }
        return VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY;
    }

    private MovieFetchProcess fetchItem(final Movie movie, ActivityCallback<Movie> activityCallback) {
        Log.i(TAG, "fetchItem: " + movie.getVideoUrl());

        // get watch link referer
        Document doc1 = getRequestDoc(movie.getVideoUrl());
        if (doc1 == null) {
            activityCallback.onInvalidLink(movie);
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
        }
        Log.d(TAG, "fetchItem: title: "+ doc1.title()+", "+doc1.title().contains("Just a moment"));
        if (doc1.title().contains("Just a moment")) {
//            Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
//            return startWebForResultActivity(clonedMovie);
            activityCallback.onInvalidCookie(movie, getLabel());
        }


        // Find all <a> elements
        Elements links = doc1.select("a[href]");

        String referer = null;
        for (Element link : links) {
            // Flexible filtering based on text or child elements
            if (link.text().contains("شاهد") || !link.select("i.fa-play").isEmpty()) {
                String href =link.attr("href");
                System.out.println("Extracted Href: " + href);
                referer =Util.extractDomain(href, true, true);
                System.out.println("Extracted referer: " + referer);
                break; // Stop after finding the first match
            }
        }

        String url = movie.getVideoUrl() + "watching/";
        try {

            Document doc = Jsoup.connect(url)
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .headers(getConfig().getHeaders())
                    .header("Referer", referer)
                    .cookies(getConfig().getMappedCookies())
//                    .userAgent("Android 7")
//                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
//                    .timeout(16000)
                    .timeout(0)
                    .get();
        //Elements links = doc.select("a[href]");


            if (doc == null) {
                activityCallback.onInvalidLink(movie);
                return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN, movie);
            }
            Log.d(TAG, "fetchItem: title: "+ doc.title()+", "+doc.title().contains("Just a moment"));
            if (doc.title().contains("Just a moment")) {
//            Movie clonedMovie = Movie.clone(movie);
//            clonedMovie.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
//            return startWebForResultActivity(clonedMovie);
                activityCallback.onInvalidCookie(movie, getLabel());
            }


            // Find the img element inside the figure
            Elements imgElements = doc.select("figure img");

            // Extract the src attribute
            if (!imgElements.isEmpty()) {
                Element img = imgElements.first();
                String cardImageUrl = img.attr("src");
                movie.setBgImageUrl(cardImageUrl);
                movie.setBackgroundImageUrl(cardImageUrl);
                movie.setBgImageUrl(cardImageUrl);
                // Output the extracted image URL
                System.out.println("cardImageUrl = " + cardImageUrl);
            } else {
                System.out.println("No img element found.");
            }

            // Select the <li> element with the attribute aria-label="story"
            Element storyElement = doc.select("li[aria-label=story] p").first();

            if (storyElement != null) {
                // Extract the text inside the <p> tag
                String des = storyElement.text();
                System.out.println("Description: " + des);
                movie.setDescription(des);
            } else {
                System.out.println("No story element found.");
            }


            // Select all <li> elements that have a data-index attribute
            Elements movieElements = doc.select("li[data-index]");

            for (Element movieElement : movieElements) {
//            if (movieElement.hasClass("active")){
//                //ignore active item as its fetched before
//                continue;
//            }
                String dataIndex = movieElement.attr("data-index");
                String title = movieElement.text();  // Get the text content of the <li>
                // Create a new Movie object and add it to the list
                Movie serverItem = Movie.clone(movie);
                serverItem.setTitle(title);
                serverItem.setVideoUrl(url + "#"+dataIndex  + "|Referer="+getConfig().getReferer());
                serverItem.setState(Movie.RESOLUTION_STATE);
                movie.addSubList(serverItem);
                Log.d(TAG, "fetchItem: serverItem:"+serverItem.getVideoUrl());
            }

            activityCallback.onSuccess(movie, getLabel());
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
        } catch (
    IOException e) {
        //builder.append("Error : ").append(e.getMessage()).append("\n");
        Log.i(TAG, "error: " + e.getMessage() + ", url: "+ url);
//            String errorMessage = "error: " + getServerId() + ": " + e.getMessage();

            activityCallback.onInvalidCookie(movie, getLabel());
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);
    }

//        Document doc = getRequestDoc(url);


// Loop through the elements and extract the movie information
//        Element embedElement = doc.selectFirst("iframe");
//
//        if (embedElement != null){
//            String src = embedElement.attr("src");
//            if (src != null){
//                Movie serverItem = Movie.clone(movie);
//                if (src.startsWith("//")){
//                    src = "https:"+src;
//                }
//                serverItem.setVideoUrl(src + "|Referer="+getConfig().getReferer());
//                serverItem.setState(Movie.BROWSER_STATE);
//                movie.addSubList(serverItem);
//            }
//        }


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
        String script = null;

        Log.d(TAG, "getWebScript: m:" + mode + ", f:" + movie.getFetch());
        if (mode == BrowserActivity.WEB_VIEW_MODE_ON_PAGE_STARTED) {
            if (movie.getState() == Movie.COOKIE_STATE) {
                Log.d(TAG, "getScript:WEB_VIEW_MODE_ON_PAGE_STARTED COOKIE_STATE");
                script = "document.addEventListener(\"DOMContentLoaded\", () => {" +
                        "let articles = document.querySelectorAll(\"article[aria-label='post']\");\n" +
                        "if (articles.length === 0) {\n" +
                        "    articles = document.querySelectorAll(\"a:has(li[aria-label='title'])\");\n" +
                        "}\n" +
                        "\n" +
                        "let movieList = [];\n" +
                        "\n" +
                        "articles.forEach(article => {\n" +
                        "    let movie = {};\n" +
                        "\n" +
                        "    // Extract the video URL from the anchor tag\n" +
                        "    let anchor = article.querySelector(\"a\");\n" +
                        "    let title = anchor ? anchor.getAttribute(\"href\") : null;\n" +
                        "\n" +
                        "    if (!title) {\n" +
                        "        title = article.getAttribute(\"href\");\n" +
                        "        if (!title) {\n" +
                        "            return;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    movie.videoUrl = title;\n" +
                        "\n" +
                        "    \n" +
                        "\n" +
                        "    // Extract the movie title and category\n" +
                        "    let titleElement = article.querySelector(\"li[aria-label='title']\");\n" +
                        "    if (titleElement) {\n" +
                        "        let titleText = titleElement.textContent;\n" +
                        "        let parts = titleText.split(\"<em>\");\n" +
                        "        if (parts.length > 0) {\n" +
                        "let titleElement = article.querySelector(\"li[aria-label='title']\");\n" +
                        "    let episodeElement = article.querySelector(\"li[aria-label='episode']\");\n" +
                        "    let episode = episodeElement ? episodeElement.textContent.replace(\"الحلقة\", \"\").trim() : \"\";\n" +
                        "\n" +
                        "               movie.title = episode + ' الحلقة ' + parts[0].trim();\n" +
                        "        }\n" +
                        "        if (parts.length > 1) {\n" +
                        "            movie.group = parts[1].replace(\"</em>\", \"\").trim();\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    // Extract the card image URL from the img tag\n" +
                        "    let imgElement = article.querySelector(\"img\");\n" +
                        "    if (imgElement) {\n" +
                        "        movie.cardImageUrl = imgElement.getAttribute(\"data-src\");\n" +
                        "        movie.setBackgroundImageUrl = imgElement.getAttribute(\"data-src\");\n" +
                        "        movie.setBgImageUrl = imgElement.getAttribute(\"data-src\");\n" +
                        "    }\n" +
                        "\n" +
                        "    movie.studio = '" + Movie.SERVER_CimaNow + "';\n" +
                        "    movie.state = title.includes(\"/selary\") ? " + Movie.GROUP_STATE +" : " + Movie.ITEM_STATE +";\n" +
                        "    movie.mainMovieTitle = movie.videoUrl;\n" +
                        "\n" +
                        "    movieList.push(movie);\n" +
                        "});" +
                        "" +
                        "let nextPage = {};\n" +
                        "let pagination = document.querySelector(\"ul[aria-label='pagination']\");\n" +
                        "\n" +
                        "if (pagination) {\n" +
                        "    // Find the active <li> element\n" +
                        "    let activePage = pagination.querySelector(\"li.active\");\n" +
                        "\n" +
                        "    if (activePage) {\n" +
                        "        // Get the next <li> after the active one\n" +
                        "        let nextPageElem = activePage.nextElementSibling;\n" +
                        "\n" +
                        "        // Check if the next <li> contains an <a> tag and has a valid href attribute\n" +
                        "        if (nextPageElem && nextPageElem.querySelector(\"a\")) {\n" +
                        "            let nextPageUrl = nextPageElem.querySelector(\"a\").getAttribute(\"href\");\n" +
                        "\n" +
                        "            nextPage.title= \"التالي\";\n" +
                        "                nextPage.description = \"0\";\n" +
                        "                nextPage.studio= 'imaNow';\n" +
                        "                nextPage.videoUrl= nextPageUrl;\n" +
                        "                nextPage.cardImageUrl= \"https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png\";\n" +
                        "                nextPage.backgroundImageUrl= \"https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png\";\n" +
                        "                nextPage.state= 5;\n" +
                        "                nextPage.mainMovieTitle= nextPageUrl;\n" +
                        "            console.log(\"Next page URL:\", nextPageUrl);\n" +
                        "        } else {\n" +
                        "            console.log(\"No next page found.\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "} " +
                        "movieList.push(nextPage);" +

                        "MyJavaScriptInterface.myMethod(JSON.stringify(movieList));" +
                        "});";
            }
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
            if (movie.getState() == Movie.RESOLUTION_STATE) {
                String referer = Util.extractDomain(movie.getVideoUrl(), true, true);
                int hashIndex = movie.getVideoUrl().indexOf('#');

                // Check if the hash symbol is found in the string
                if (hashIndex != -1 && hashIndex < movie.getVideoUrl().length() - 1) {
                    // Return the substring after the hash symbol
                  String  serverId = movie.getVideoUrl().substring(hashIndex + 1);

                    // Find the index of the '|' symbol in the substring after the hash
                    int pipeIndex = serverId.indexOf('|');

                    // If '|' is found, return the substring up to the '|' symbol
                    if (pipeIndex != -1) {
                        serverId = serverId.substring(0, pipeIndex);
                    }


                    script = "document.addEventListener('DOMContentLoaded', () => {\n" +
                            "    let elements = document.querySelectorAll('li[data-index]');\n" +
                            "    elements.forEach(function (element) {\n" +
                            "        if (element.getAttribute('data-index') == "+serverId+") {\n" +
                            "            // Simulate a click on the matched element\n" +
                            "            element.click();\n" +
                            "// Wait for the second element to appear after the click\n" +
                            "        waitForElement('iframe', function(secondElement) {\n" +
                            "            console.log('xxxx: Second element appeared:', secondElement);\n" +
                            "makeFullScreen(secondElement);" +
                            "        });" +

                            "        }\n" +
                            "    });" +

                            "function makeFullScreen(iframe) {" +
                            "if (iframe) {\n" +
                            "  // Clone the iframe element\n" +
                            "        var clonedIframe = iframe.cloneNode(true);\n" +
                            "        \n" +
                            "        // Clear the entire body content\n" +
//                            "        document.body.innerHTML = '';\n" +
                            "        iframe.innerHTML = '';\n" +
                            "\n" +
                            "        // Append the cloned iframe to the body\n" +
                            "        document.body.appendChild(clonedIframe);\n" +
                            " // Modify the cloned iframe to make it fullscreen\n" +
                            "        clonedIframe.style.position = \"fixed\";\n" +
                            "        clonedIframe.style.top = \"0\";\n" +
                            "        clonedIframe.style.left = \"0\";\n" +
                            "        clonedIframe.style.width = \"100%\";\n" +
                            "        clonedIframe.style.height = \"100%\";\n" +
                            "        clonedIframe.style.zIndex = \"9999\";\n" +
                            "        clonedIframe.style.border = \"none\"; // Remove any borders\n" +
                            "        clonedIframe.setAttribute(\"allowfullscreen\", \"true\");\n" +
                            "        clonedIframe.setAttribute(\"scrolling\", \"yes\");\n" +
                            "\n" +
                            "        // Optional: if you want to apply fullscreen mode programmatically (only works if triggered by a user gesture)\n" +
                            "        if (iframe.requestFullscreen) {\n" +
                            "            iframe.requestFullscreen();\n" +
                            "        } else if (iframe.webkitRequestFullscreen) { // Safari\n" +
                            "            iframe.webkitRequestFullscreen();\n" +
                            "        } else if (iframe.mozRequestFullScreen) { // Firefox\n" +
                            "            iframe.mozRequestFullScreen();\n" +
                            "        } else if (iframe.msRequestFullscreen) { // IE/Edge\n" +
                            "            iframe.msRequestFullscreen();\n" +
                            "        }\n" +
                            "    } else {\n" +
                            "        console.log(\"Iframe not found.\");\n" +
                            "    }" +
                            "\n}" +
                            "// Function to wait for an element to appear\n" +
                            "    function waitForElement(selector, callback) {\n" +
                            "        const observer = new MutationObserver(function(mutations) {\n" +
                            "            mutations.forEach(function(mutation) {\n" +
                            "                const element = document.querySelector(selector);\n" +
                            "                if (element) {\n" +
                            "                    callback(element);\n" +
                            "                    observer.disconnect(); // Stop observing after the element appears\n" +
                            "                }\n" +
                            "            });\n" +
                            "        });\n" +
                            "\n" +
                            "        // Observe changes in the DOM\n" +
                            "        observer.observe(document.body, {\n" +
                            "            childList: true,\n" +
                            "            subtree: true\n" +
                            "        });\n" +
                            "    }" +
                            "" +
                            "" +
                            " });";
                }

            }


        }
        Log.d(TAG, "getWebScript: "+script);
        return script;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
        return search(getConfig().getUrl() + "/الاحدث/", activityCallback);
//        return search(getConfig().getUrl() + "/category/افلام-اجنبية/", activityCallback);
//        return search(getConfig().getUrl() + "/category/المسلسلات", activityCallback);
//        return search("sonic", activityCallback);
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
