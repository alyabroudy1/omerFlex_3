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
            // Ensure application context is used if possible to avoid leaks,
            // though for singletons from Application class, it might be okay.
            OmerFlexApplication app = OmerFlexApplication.getInstance(); // Assuming getInstance() provides the Application instance
            if (app != null) {
                this.threadPoolManager = app.getThreadPoolManager();
                this.httpClientManager = app.getHttpClientManager();
                Logger.d(TAG, "HttpClientManager initialized in AbstractServer for: " + this.getClass().getSimpleName());
            } else {
                Logger.e(TAG, "OmerFlexApplication.getInstance() returned null in AbstractServer.initialize for: " + this.getClass().getSimpleName());
            }
        } else {
            Logger.w(TAG, "Context is null in AbstractServer.initialize for: " + this.getClass().getSimpleName());
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
        ServerConfig config = getConfig(); // Relies on getConfig() being available
        if (config == null) {
            Logger.w(TAG, "getSearchRequestDoc: ServerConfig is null for serverId: " + getServerId());
            // Attempt to load config if null, or handle error
            // For now, assume getConfig() provides a valid or default config if possible
            // If still null, we cannot proceed.
            return null;
        }

        if (this.httpClientManager == null) {
             Logger.e(TAG, "getSearchRequestDoc: HttpClientManager is not initialized in AbstractServer for " + getServerId() + "!");
             // Attempt to initialize it here if context is available
             if (this.context != null) {
                OmerFlexApplication app = OmerFlexApplication.getInstance();
                if (app != null) {
                    this.httpClientManager = app.getHttpClientManager();
                    Logger.i(TAG, "getSearchRequestDoc: Re-initialized HttpClientManager.");
                }
             }
             if (this.httpClientManager == null) {
                Logger.e(TAG, "getSearchRequestDoc: Failed to initialize HttpClientManager. Cannot proceed for " + getServerId());
                return null; // Cannot make the call
             }
        }

        String currentUrl = url; // Keep initial URL for error reporting if needed
        String initialHost = extractDomain(url);

        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder().url(currentUrl);

        if (config.getHeaders() != null) {
            for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        String cookieString = config.getStringCookies();
        if (cookieString != null && !cookieString.isEmpty()) {
            requestBuilder.addHeader("Cookie", cookieString);
        }

        // Use a client that automatically follows redirects from HttpClientManager
        okhttp3.OkHttpClient client = httpClientManager.getDefaultClient().newBuilder()
                                .followRedirects(true)
                                .followSslRedirects(true)
                                .build(); // Build a new client instance with redirect handling enabled

        okhttp3.Response response = null; // Declare response outside try to use in catch
        try {
            response = client.newCall(requestBuilder.build()).execute();
            int statusCode = response.code();
            String finalUrl = response.request().url().toString(); // URL after redirects
            Logger.i(TAG, "HTTP Status: " + statusCode + " for " + finalUrl + " (Initial: " + url + ")");

            checkForDomainUpdate(finalUrl, initialHost); // Check if domain changed

            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    Document doc = Jsoup.parse(responseBody, finalUrl);

                    // The existing handleSecurityCheck can be called by the methods using getSearchRequestDoc
                    // e.g., in search() or fetchSeriesAction() after this method returns the doc.
                    // if (doc != null && doc.title() != null && doc.title().contains("Just a moment")) {
                    //    Logger.w(TAG, "Detected 'Just a moment...' page for " + finalUrl);
                    // }
                    return doc;
                } else {
                    Logger.w(TAG, "Response body is null for " + finalUrl);
                    return null;
                }
            } else {
                Logger.e(TAG, "Unexpected status " + statusCode + " for " + finalUrl);
                // Optionally parse error body if it can contain useful info, otherwise return empty/null
                // String errorBody = response.body() != null ? response.body().string() : "";
                // Document errorDoc = Jsoup.parse(errorBody, finalUrl);
                // return errorDoc; // Or null if error pages aren't useful Jsoup docs
                return null;
            }
        } catch (IOException e) {
            String attemptedUrl = currentUrl;
            if (response != null) { // If response exists, the error might have occurred after redirects
                attemptedUrl = response.request().url().toString();
            }
            Logger.e(TAG, "Network error for " + url + " (final attempted: " + attemptedUrl + ") : " + e.getMessage(), e);
        } catch (Exception e) {
            Logger.e(TAG, "Unexpected error processing " + url + ": " + e.getMessage(), e);
        } finally {
            if (response != null) {
                response.close(); // Ensure response body is closed
            }
        }
        return null;
    }

    // isRedirect and resolveRedirectUrl are no longer needed as OkHttp handles redirects.

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