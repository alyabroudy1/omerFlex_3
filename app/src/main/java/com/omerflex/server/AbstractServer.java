package com.omerflex.server;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.LruCache;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.omerflex.OmerFlexApplication;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.ServerConfigManager;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;
import com.omerflex.service.network.HttpClientManager;
import com.omerflex.view.VideoDetailsFragment;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractServer implements ServerInterface {

    private static final String TAG = "AbstractServer";
    private static final int MAX_REDIRECTS = 5;
    private static final int CACHE_SIZE = 50;
    private static final long CACHE_EXPIRY_MS = TimeUnit.MINUTES.toMillis(5);

    protected Context context;
    protected ThreadPoolManager threadPoolManager;
    protected HttpClientManager httpClientManager;

    private static final LruCache<String, CachedDocument> documentCache = new LruCache<>(CACHE_SIZE);

    private static class CachedDocument {
        final Document document;
        final long timestamp;

        CachedDocument(Document document) {
            this.document = document;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    protected void initialize(Context context) {
        this.context = context;

        if (context != null) {
            OmerFlexApplication app = OmerFlexApplication.getInstance();
            if (app != null) {
                threadPoolManager = app.getThreadPoolManager();
                httpClientManager = app.getHttpClientManager();
            }
        }
    }

    @Override
    public ArrayList<Movie> getHomepageMovies(ActivityCallback<ArrayList<Movie>> activityCallback) {
        ServerConfig config = getConfig();
        String url = config != null && config.getUrl() != null ?
                config.getUrl() : "";

        if (!url.isEmpty()) {
            Logger.i(TAG, "Fetching homepage content for " + url);
            return search(url, activityCallback);
        }

        if (activityCallback != null) {
            activityCallback.onInvalidLink("Server URL not configured");
        }
        return null;
    }

    /**
     * Helper method to handle common security check detection pattern
     *
     * @param doc              The document to check
     * @param movie            The movie being processed
     * @param activityCallback Callback to notify about security check
     * @return true if security check detected and handled, false otherwise
     */
    protected <T> boolean handleSecurityCheck(Document doc, Movie movie, ActivityCallback<T> activityCallback) {
        if (doc != null && doc.title().contains("Just a moment")) {
            Logger.i(TAG, "Detected security check, needs cookie authentication");
            if (activityCallback != null) {
                ArrayList<Movie> movieList = new ArrayList<>();
                movieList.add(movie);
                activityCallback.onInvalidCookie((T) movieList, getLabel());
            }
            return true;
        }
        return false;
    }

    /**
     * Create common JS scripts for post-processing
     *
     * @param movie         The movie to create script for
     * @param selector      CSS selector for target elements
     * @param dataProcessor JS function to process data (without function declaration)
     * @return JavaScript to inject
     */
    protected String createDataExtractionScript(Movie movie, String selector, String dataProcessor) {
        return "if(document != null){" +
                "document.addEventListener(\"DOMContentLoaded\", () => {" +
                "  let postList = [];" +
                "  let elements = document.querySelectorAll('" + selector + "');" +
                "  if (elements.length > 0){" +
                "    " + dataProcessor +
                "    let post = {};" +
                "    post.videoUrl = extractedUrl;" +
                "    post.rowIndex = '" + movie.getRowIndex() + "';" +
                "    post.title = '" + movie.getTitle() + "';" +
                "    post.fetch = '" + movie.getFetch() + "';" +
                "    post.cardImageUrl = '" + movie.getCardImageUrl() + "';" +
                "    post.bgImageUrl = '" + movie.getBgImageUrl() + "';" +
                "    post.description = '" + movie.getDescription() + "';" +
                "    post.state = '" + Movie.VIDEO_STATE + "';" +
                "    post.studio = '" + movie.getStudio() + "';" +
                "    postList.push(post);" +
                "  }" +
                "  MyJavaScriptInterface.myMethod(JSON.stringify(postList));" +
                "}); }";
    }

    /**
     * Safe method to add a movie to a sublist, handling null case
     *
     * @param parentMovie The parent movie with the sublist
     * @param childMovie  The movie to add
     */
    protected void safeAddToSublist(Movie parentMovie, Movie childMovie) {
        if (childMovie == null) return;

        if (parentMovie.getSubList() == null) {
            parentMovie.setSubList(new ArrayList<>());
        }
        parentMovie.addSubList(childMovie);
    }

    @Override
    public ArrayList<Movie> search(String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Logger.i(TAG, "search: " + query + " [" + getLabel() + "]");

        try {
            String url = formatSearchUrl(query);

            if (threadPoolManager != null) {
                executeSearchAsync(url, query, activityCallback);
                return null;
            } else {
                return executeSearchSync(url, query, activityCallback);
            }
        } catch (Exception e) {
            handleSearchError(e, activityCallback);
            return null;
        }
    }

    private String formatSearchUrl(String query) {
        if (!query.contains("http")) {
            return getSearchUrl(query);
        }
        return query;
    }

    private void executeSearchAsync(String url, String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Executor executor = threadPoolManager.getNetworkExecutor();
        executor.execute(() -> {
            try {
                ArrayList<Movie> results = executeSearchSync(url, query, activityCallback);
                if (results != null && activityCallback != null) {
                    activityCallback.onSuccess(results, getLabel());
                }
            } catch (Exception e) {
                handleSearchError(e, activityCallback);
            }
        });
    }

    private ArrayList<Movie> executeSearchSync(String url, String query, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Document doc = getDocumentWithCache(url);
        if (doc == null) {
            Logger.w(TAG, "search: Failed to get document from URL: " + url);
            if (activityCallback != null) {
                activityCallback.onInvalidLink("Failed to load search results");
            }
            return null;
        }

        ArrayList<Movie> results = getSearchMovieList(doc);
        if (results == null || results.isEmpty()) {
            Logger.w(TAG, "No search results found for: " + query);
            if (activityCallback != null) {
                activityCallback.onInvalidLink("No results found");
            }
        }
        return results;
    }

    private void handleSearchError(Exception e, ActivityCallback<ArrayList<Movie>> activityCallback) {
        Logger.e(TAG, "Error during search operation", e);
        if (context != null) {
            ErrorHandler.handleError(context, ErrorHandler.NETWORK_ERROR,
                    "Error searching for content", e);
        }
        if (activityCallback != null) {
            activityCallback.onInvalidLink("Error: " + e.getMessage());
        }
    }

    protected Document getDocumentWithCache(String url) {
        synchronized (documentCache) {
            CachedDocument cached = documentCache.get(url);
            if (cached != null && !cached.isExpired()) {
                Logger.d(TAG, "Cache hit for URL: " + url);
                return cached.document;
            }
        }

        Document doc = getSearchRequestDoc(url);

        if (doc != null) {
            synchronized (documentCache) {
                documentCache.put(url, new CachedDocument(doc));
            }
        }

        return doc;
    }

    @Override
    public MovieFetchProcess fetch(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        if (movie == null) {
            Logger.w(TAG, "fetch: invalid movie");
            if (activityCallback != null) {
                activityCallback.onInvalidLink("invalid movie");
            }
            return null;
        }

        try {
            if (threadPoolManager != null && shouldExecuteAsynchronously(action)) {
                return executeFetchAsync(movie, action, activityCallback);
            } else {
                return executeFetchSync(movie, action, activityCallback);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error during fetch operation", e);
            if (context != null) {
                ErrorHandler.handleError(context, ErrorHandler.NETWORK_ERROR,
                        "Error fetching content", e);
            }
            if (activityCallback != null) {
                activityCallback.onInvalidLink("Error: " + e.getMessage());
            }
            return null;
        }
    }

    private MovieFetchProcess executeFetchAsync(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        Executor executor = threadPoolManager.getNetworkExecutor();
        final MovieFetchProcess fetchProcess = new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_SUCCESS, movie);

        executor.execute(() -> {
            try {
                MovieFetchProcess result = executeFetchSync(movie, action, activityCallback);
                if (result != null) {
                    fetchProcess.movie = result.movie;
                    fetchProcess.stateCode = result.stateCode;
                } else {
                    fetchProcess.stateCode = MovieFetchProcess.FETCH_PROCESS_ERROR_UNKNOWN;
                }
            } catch (Exception e) {
                Logger.e(TAG, "Error during async fetch operation", e);
                if (context != null) {
                    ErrorHandler.handleError(context, ErrorHandler.NETWORK_ERROR,
                            "Error fetching content", e);
                }
                if (activityCallback != null) {
                    activityCallback.onInvalidLink("Error: " + e.getMessage());
                }
            }
        });

        return fetchProcess;
    }

    private MovieFetchProcess executeFetchSync(Movie movie, int action, ActivityCallback<Movie> activityCallback) {
        switch (action) {
            case Movie.GROUP_OF_GROUP_STATE:
            case Movie.GROUP_STATE:
                Logger.d(TAG, "fetch: fetchSeriesAction for movie: " + movie.getTitle());
                return fetchSeriesAction(movie, action, activityCallback);
            default:
                Logger.d(TAG, "fetch: default fetchItemAction: " + action + ", movie: " + movie.getTitle());
                return fetchItemAction(movie, action, activityCallback);
        }
    }

    protected boolean shouldExecuteAsynchronously(int action) {
        return true;
    }

    protected ServerConfig getConfig() {
        return ServerConfigManager.getConfig(getServerId());
    }

    public boolean shouldOverrideUrlLoading(Movie movie, WebView view, WebResourceRequest request) {
        try {
            if (movie == null || request == null || view == null) {
                Logger.w(TAG, "shouldOverrideUrlLoading: movie, view, or request is null");
                return false;
            }

            final String url = request.getUrl().toString();
            final String host = request.getUrl().getHost();
            final String newUrl = url.length() > 25 ? url.substring(0, 25) : url;

            ServerConfig config = getConfig();
            if (config != null) {
                if (newUrl.contains(config.getUrl())) {
                    Logger.d(TAG, "shouldOverrideUrlLoading: URL matches server config URL, not overriding");
                    return false;
                }
                Logger.d(TAG, "shouldOverrideUrlLoading: URL doesn't match server config URL: " +
                        config.getUrl() + " vs " + url);
            }

            Uri movieUri = Uri.parse(movie.getVideoUrl());
            String movieDomain = movieUri.getHost();
            if (movieDomain != null && newUrl.contains(movieDomain)) {
                Logger.d(TAG, "shouldOverrideUrlLoading: URL matches movie domain, not overriding");
                return false;
            }

            if (host != null && host.contains("game") && url.contains("post")) {
                Logger.d(TAG, "shouldOverrideUrlLoading: URL is a game post, not overriding");
                return false;
            }

            if (url.contains("embed")) {
                Logger.d(TAG, "shouldOverrideUrlLoading: Loading embed URL in WebView");
                view.loadUrl(url);
                return false;
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error in shouldOverrideUrlLoading", e);
            if (context != null) {
                ErrorHandler.handleError(context, ErrorHandler.GENERAL_ERROR,
                        "Error processing URL", e);
            }
            return false;
        }

        return true;
    }

    protected Document getSearchRequestDoc(String url) {
        final int MAX_REDIRECTS = 5;
        ServerConfig config = getConfig();
        Document doc = null;
        int redirectCount = 0;
        String currentUrl = url;
        boolean isDomainUpdated = false;
        String initialHost = extractDomain(url);

        try {
            while (redirectCount < MAX_REDIRECTS) {
                Logger.d(TAG, "Processing URL: " + currentUrl + ", follow: " + isDomainUpdated);

                Connection.Response response = Jsoup.connect(currentUrl)
                        .headers(config.getHeaders())
                        .cookies(config.getMappedCookies())
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .timeout(10000)
                        .execute();

                int statusCode = response.statusCode();
                Logger.i(TAG, "HTTP Status: " + statusCode + " for " + currentUrl);

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    doc = response.parse();
                    return doc;
                } else if (isRedirect(statusCode)) {
                    String newLocation = response.header("Location");
                    if (newLocation == null || newLocation.isEmpty()) {
                        Logger.w(TAG, "Redirect without Location header: " + currentUrl);
                        doc = response.parse();
                        return doc;
                    }
                    currentUrl = resolveRedirectUrl(currentUrl, newLocation);
                    Logger.d(TAG, "Redirecting to: " + currentUrl);
                    isDomainUpdated = checkForDomainUpdate(currentUrl, initialHost);
                    redirectCount++;
                } else {
                    Logger.e(TAG, "Unexpected status " + response.statusCode() + " for " + currentUrl);
                    return statusCode == HttpURLConnection.HTTP_NOT_FOUND ? null : response.parse();
                }
            }
            Logger.w(TAG, "Too many redirects (" + MAX_REDIRECTS + ") for: " + url);
        } catch (IOException e) {
            Logger.e(TAG, "Network error for " + currentUrl + ": " + e.getMessage());
        } catch (Exception e) {
            Logger.e(TAG, "Unexpected error processing " + currentUrl + ": " + e.getMessage());
        }
        return null;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode >= HttpURLConnection.HTTP_MOVED_PERM
                && statusCode < HttpURLConnection.HTTP_BAD_REQUEST;
    }

    private String resolveRedirectUrl(String baseUrl, String location) throws MalformedURLException {
        if (location.startsWith("http")) {
            return location;
        }
        URL base = new URL(baseUrl);
        return new URL(base, location).toString();
    }

    private String extractDomain(String url) {
        try {
            Uri uri = Uri.parse(url);
            return uri.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean checkForDomainUpdate(String finalUrl, String initialHost) {
        if (!shouldUpdateDomainOnSearchResult()) return true;

        Uri finalUri = Uri.parse(finalUrl);
        String finalHost = finalUri.getHost();
        String finalScheme = finalUri.getScheme();

        if (finalHost == null || finalScheme == null) {
            return false;
        }

        if (!initialHost.equals(finalHost)) {
            String schemeAndHost = finalScheme + "://" + finalHost;
            Logger.i(TAG, "Updating domain from " + initialHost + " to " + finalHost);
            updateDomain(schemeAndHost);
            return true;
        }
        return false;
    }

    private void updateDomain(String newUrl) {
        Logger.d(TAG, "updateDomain: " + newUrl);
        getConfig().setUrl(newUrl);
        getConfig().setReferer(newUrl + "/");
    }

    public int fetchNextAction(Movie movie) {
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
        switch (movie.getState()) {
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

    public boolean shouldInterceptRequest(WebView view, WebResourceRequest request, Movie movie) {
        int state = movie.getState();
        return state == Movie.RESOLUTION_STATE || state == Movie.BROWSER_STATE;
    }

    public boolean shouldCleanWebPage(String pageUrl, Movie movie) {
        int state = movie.getState();
        return state == Movie.RESOLUTION_STATE || state == Movie.BROWSER_STATE;
    }

    protected abstract String getSearchUrl(String query);

    protected abstract ArrayList<Movie> getSearchMovieList(Document doc);

    protected abstract MovieFetchProcess fetchSeriesAction(Movie movie, int action, ActivityCallback<Movie> activityCallback);

    protected abstract MovieFetchProcess fetchItemAction(Movie movie, int action, ActivityCallback<Movie> activityCallback);

    public abstract int detectMovieState(Movie movie);

    public abstract String getWebScript(int mode, Movie movie);

    public String getCustomUserAgent(int state) {
        return "Android 6";
    }

    public MovieFetchProcess handleJSResult(String elementJson, ArrayList<Movie> movies, Movie movie) {
        movie.setSubList(movies);
        return new MovieFetchProcess(MovieFetchProcess.FETCH_PROCESS_DETAILS_ACTIVITY_REQUIRE, movie);
    }

    public boolean shouldUpdateDomainOnSearchResult() {
        return true;
    }

    public static void clearCache() {
        synchronized (documentCache) {
            documentCache.evictAll();
            Logger.i(TAG, "Document cache cleared");
        }
    }
}