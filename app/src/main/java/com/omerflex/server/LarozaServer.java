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

public class LarozaServer extends AbstractServer{

    static String TAG = "Laroza";
    static String WEBSITE_URL = "https://www.laroza.now";

    @Override
    protected String getSearchUrl(String query) {
        if (query.contains("http")) {
            return query;
        }
        String searchPart = "/search.php?keywords=";
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
            m.setStudio(Movie.SERVER_LAROZA);
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

        // Get all thumbnail elements
        Elements thumbnails = doc.select(".thumbnail");
        ArrayList<Movie> movieList = new ArrayList<>();
        // Iterate through each thumbnail
        for (Element thumbnail : thumbnails) {
            Movie movie = new Movie();
            // Extract title
            Element titleElement = thumbnail.selectFirst(".caption");
            String title = titleElement != null ? titleElement.text().trim() : null;

            // Extract video URL
            Element videoUrlElement = thumbnail.selectFirst(".pm-video-thumb a:not(.pm-watch-later-add)");
            String videoUrl = videoUrlElement != null ? videoUrlElement.attr("href") : null;
if (videoUrl == null){continue;}

if (!videoUrl.startsWith("http")){
    videoUrl = getConfig().getUrl() + "/" + videoUrl;
}
            Log.d(TAG, "search result url: " + videoUrl);

            // Extract card image URL
            Element cardImageElement = thumbnail.selectFirst(".pm-video-thumb img");
// Check for common attributes where the real image URL might be stored
            String cardImage = null;
            if (cardImageElement != null) {

                if (cardImageElement.hasAttr("data-echo")) {
                    cardImage = cardImageElement.attr("data-echo"); // Lazy-loaded image
                } else
                if (cardImageElement.hasAttr("data-src")) {
                    cardImage = cardImageElement.attr("data-src"); // Lazy-loaded image
                } else if (cardImageElement.hasAttr("data-original")) {
                    cardImage = cardImageElement.attr("data-original"); // Alternative attribute
                } else {
                    cardImage = cardImageElement.attr("src"); // Default src attribute
                }
            }

            Log.d(TAG, "generateSearchResultFromDoc: image:"+cardImage);

            movie.setTitle(title);
            movie.setDescription(title);
            movie.setCardImageUrl(cardImage);
            movie.setBackgroundImageUrl(cardImage);
            movie.setBgImageUrl(cardImage);
            movie.setVideoUrl(videoUrl);
            movie.setStudio(Movie.SERVER_LAROZA);
            movie.setState(detectMovieState(movie));
            movie.setMainMovie(movie);
            movie.setMainMovieTitle(videoUrl);

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
        // Find the <i> element with class "fa fa-arrow-left"
        Element iElement = doc.selectFirst("i.fa.fa-arrow-left");
        if (iElement == null) {
            return null; // or throw an exception
        }

        // Find the closest ancestor 'a' element
        Element aElement = iElement.closest("a");

        if (aElement == null) {
            return null; // or throw an exception if no 'a' is found
        }

 String nextPageUrl = aElement.attr("href");;
                    nextPage = new Movie();
                    nextPage.setTitle("التالي");
                    nextPage.setDescription("0");
                    nextPage.setStudio(Movie.SERVER_LAROZA);
                    nextPage.setVideoUrl(nextPageUrl);
                    nextPage.setCardImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                    nextPage.setBackgroundImageUrl("https://colorslab.com/blog/wp-content/uploads/2012/03/next-button-usability.png");
                    nextPage.setState(Movie.NEXT_PAGE_STATE);
                    nextPage.setMainMovie(nextPage);
                    nextPage.setMainMovieTitle(nextPageUrl);

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

        ArrayList<Movie> movieList = generateSearchResultFromDoc(doc);
        movie.setSubList(movieList);
        activityCallback.onSuccess(movie, getLabel());
        return  new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
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

        String url = movie.getVideoUrl();
        if (url.contains("video.php")){
            url = movie.getVideoUrl().replace("video.php", "play.php");
        }

        // get watch link referer
        Document doc = getRequestDoc(url);
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
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);
        }


        // Select the .description element
        Element descriptionDiv = doc.selectFirst(".description");

        if (descriptionDiv != null) {
            // Select all p elements within the .description element
            Elements pElements = descriptionDiv.select("p");

            // Iterate through the p elements and extract the text
            for (Element pElement : pElements) {
                String text = pElement.text();
                if (!text.trim().isEmpty()) {
                    System.out.println("Extracted Text: " + text);
                    movie.setDescription(text);
                }
            }
        } else {
            System.out.println("Could not find the .description element.");
        }

        Elements servers = doc.select("ul.WatchList li");

        if (servers.isEmpty()){
            activityCallback.onInvalidCookie(movie, getLabel());
            return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE, movie);
        }
        String referer = Util.extractDomain(movie.getVideoUrl(), true, true);
        // Extract and print server details
        for (Element server : servers) {
            String title = server.select("strong").text();
            String dataIndex = server.attr("data-embed-url");
//            String dataIndex = server.attr("data-embed-id");
            Movie resolution = Movie.clone(movie);
            resolution.setState(Movie.RESOLUTION_STATE);
            resolution.setTitle(title);

            resolution.setVideoUrl(dataIndex + "|Referer="+referer);
//            resolution.setVideoUrl(url + "#"+dataIndex);

            movie.addSubList(resolution);
        }

        activityCallback.onSuccess(movie, getLabel());
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);
    }


    @Override
    public int detectMovieState(Movie movie) {
        if (movie.getVideoUrl().contains("-serie")){
            return Movie.GROUP_STATE;
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

                    script =
//                            "document.addEventListener('DOMContentLoaded', () => {\n" +
                            "" +
                            "let elements = document.querySelectorAll('li[data-embed-id]');\n" +
                            "elements.forEach(function (element) {\n" +
                            "    if (element.getAttribute('data-embed-id') == " + serverId+ ") {\n" +
                            "        // Simulate a click on the matched element\n" +
                            "        element.click();\n" +
                            "console.log('Valid iframe found with src:', element.getAttribute('data-embed-url'));" +
                            "\n" +
//                            "        // Wait for the second element to appear after the click\n" +
                            "        waitForElement('iframe', function (secondElement) {\n" +
                            "    console.log('xxxx: Second element appeared:', secondElement);\n" +
                            "\n" +
                            "    // Get the iframe src attribute\n" +
                            "    let iframeSrc = secondElement.getAttribute('src');\n" +
                            "\n" +
                            "    // Check if src is valid (not null, not 'about:blank')\n" +
                            "    if (iframeSrc && iframeSrc.trim() !== '' && iframeSrc !== 'about:blank') {\n" +
                            "        console.log('Valid iframe found with src:', iframeSrc);\n" +
                            "\n" +
                            "        // Scroll to the iframe\n" +
                            "        secondElement.scrollIntoView({ behavior: 'smooth', block: 'center' });\n" +
                            "\n" +
                            "        // Optional: Click the iframe if needed\n" +
                            "        secondElement.click();\n" +
                            "    } else {\n" +
                            "        console.log('Skipping iframe with invalid src:', iframeSrc);\n" +
                            "    }\n" +
                            "});" +

                            "return;" +

                            "    }\n" +
                            "});" +

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
                            ""
//                            " });"
                    ;

                    script = "document.addEventListener('DOMContentLoaded', () => {\n" +
                            "    setTimeout(() => {\n" +
                            "        let elements = document.querySelectorAll('li[data-embed-id]');\n" +
                            "        elements.forEach(function (element) {\n" +
                            "            if (element.getAttribute('data-embed-id') == serverId) {\n" +
                            "                console.log('Matching element found:', element.getAttribute('data-embed-url'));\n" +
                            "\n" +
                            "                // Trigger click\n" +
                            "                element.click();\n" +
                            "            }\n" +
                            "        });\n" +
                            "    }, 1000); // Wait for 1 second before executing\n" +
                            "});\n";
                }

            }


        }
        Log.d(TAG, "getWebScript: "+script);
        return script;
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
//        return search(getSearchUrl("البطل"), activityCallback);
        return search(getConfig().getUrl() + "/newvideos.php", activityCallback);
//        return search(getConfig().getUrl() + "/moslslat1.php", activityCallback);
//        return search(getConfig().getUrl() + "/category/افلام-اجنبية/", activityCallback);
//        return search(getConfig().getUrl() + "/category/المسلسلات", activityCallback);
//        return search("sonic", activityCallback);
    }

    @Override
    public String getLabel() {
        return "لاروزا";
    }

    @Override
    public String getServerId() {
        return Movie.SERVER_LAROZA;
    }
}
