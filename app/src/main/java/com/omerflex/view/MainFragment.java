package com.omerflex.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.lifecycle.ViewModelProvider; // Added for ViewModel
import dagger.hilt.android.AndroidEntryPoint; // Added for Hilt
import java.util.HashMap; // Added for serverRowAdapterMap
import java.util.Map; // Added for serverRowAdapterMap
import com.omerflex.entity.ServerConfig; // Added for ViewModel
import com.omerflex.utils.Resource; // Added for ViewModel
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.server.Util;
import com.omerflex.service.ServerManager; // Will be refactored or injected later
import com.omerflex.service.UpdateService;
// import com.omerflex.service.database.MovieDbHelper; // Removed for Hilt
// import com.omerflex.view.MainViewControl; // To be removed

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AndroidEntryPoint // Added for Hilt
public class MainFragment extends BrowseSupportFragment {
    private static final String TAG = "MainFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private int HEADER_ROWS_COUNTER = 0;

    private int IPTV_HEADER_ROWS_COUNTER = 0;
    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 15;

    private final Handler mHandler = new Handler(Looper.myLooper());
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;

    //*****
    // MovieDbHelper dbHelper; // Removed for Hilt
    private MainFragmentViewModel viewModel; // Added for ViewModel
    private Map<String, ArrayObjectAdapter> serverRowAdapterMap = new HashMap<>(); // Added
    private List<ListRow> iptvChannelRows = new ArrayList<>(); // Added for IPTV channel rows

    Fragment fragment;
    // ServerManager serverManager; // To be refactored or Injected
    Activity activity;
    ArrayObjectAdapter rowsAdapter; // This is mRowsAdapter from BrowseSupportFragment, usually not directly reassigned
    public static List<Movie> iptvList; // Static usage - consider refactoring

    private boolean isInitialized = false;

    // private MainViewControl mainViewControl; // To be phased out - REMOVED
    int defaultHeadersCounter = 0;
    int totalHeadersCounter = 0; // Consider if this is still needed or managed by adapter size
    ArrayObjectAdapter clickedMovieAdapter; // For item click context, may still be needed or refactored
    int clickedMovieIndex = 0;   // For item click context
    //*****

    UpdateService updateService;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        start();
    }

    public void start() {

        if (isInitialized) {
            return;
        }

        setRetainInstance(true);

        initializeThings();

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();

        isInitialized = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        start();
    }

    private void initializeThings() {
        fragment = this;
        activity = getActivity();
        // dbHelper = MovieDbHelper.getInstance(activity); // Removed
        updateService = new UpdateService(fragment); // Keep for now, may need refactor
        // serverManager = new ServerManager(activity, fragment, updateService); // Removed, Hilt/ViewModel will manage data
        // serverManager.updateServers(); // Removed
        // rowsAdapter needs to be initialized before setAdapter is called.
        // BrowseSupportFragment initializes its own adapter (mRowsAdapter). We get it via getAdapter().
        // rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter()); // This creates a new one, not the one used by fragment
        iptvList = new ArrayList<>(); // Static usage, review
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
        updateService.handleOnDestroy();
    }

    private void loadRows() {
        setAdapter(rowsAdapter);
        CookieManager.getInstance().setAcceptCookie(true);

        // Get the adapter from BrowseSupportFragment
        rowsAdapter = (ArrayObjectAdapter) getAdapter();
        if (rowsAdapter == null || !(rowsAdapter.getPresenterSelector() instanceof ListRowPresenter)) {
             // Ensure rowsAdapter is not null and uses ListRowPresenter, or initialize it here if getAdapter() returned null
            rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
            setAdapter(rowsAdapter); // Set it for the fragment
        }


        // mainViewControl initialization and usage will be reduced/removed.
        // mainViewControl = new MainViewControl(activity, fragment, null /* dbHelper removed */) { ... }; // REMOVED
        // mainViewControl.loadCategoriesInBackground(""); // REMOVED

        // ViewModel Initialization and Observation
        viewModel = new ViewModelProvider(this).get(MainFragmentViewModel.class);
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.serverConfigs.observe(getViewLifecycleOwner(), serverConfigs -> {
            if (serverConfigs == null) return;

            Log.d(TAG, "Server configs loaded: " + serverConfigs.size());
            rowsAdapter.clear(); // Clear existing rows
            serverRowAdapterMap.clear();
            totalHeadersCounter = 0; // Reset if used for HeaderItem IDs

            for (ServerConfig config : serverConfigs) {
                HeaderItem header = new HeaderItem(totalHeadersCounter++, config.getLabel());
                ArrayObjectAdapter movieAdapter = new ArrayObjectAdapter(new CardPresenter());
                serverRowAdapterMap.put(config.getName(), movieAdapter);
                rowsAdapter.add(new ListRow(header, movieAdapter));
            }

            // Optionally, load movies for the first category or based on some other logic
            if (!serverConfigs.isEmpty()) {
                viewModel.setSelectedServerId(serverConfigs.get(0).getName());
            }
        });

        viewModel.moviesForSelectedServer.observe(getViewLifecycleOwner(), resource -> {
            String currentServerId = viewModel.getSelectedServerIdLiveData().getValue();
            if (currentServerId == null || resource == null) {
                Log.w(TAG, "MoviesForSelectedServer updated, but currentServerId or resource is null.");
                return;
            }

            ArrayObjectAdapter movieAdapter = serverRowAdapterMap.get(currentServerId);
            if (movieAdapter == null) {
                Log.e(TAG, "No adapter found in map for serverId: " + currentServerId);
                return;
            }

            switch (resource.status) {
                case LOADING:
                    Log.d(TAG, "Loading movies for server: " + currentServerId);
                    // TODO: Implement a way to show loading in the row, e.g., clear and add a loading card
                    movieAdapter.clear(); // Simple way to indicate loading
                    break;
                case SUCCESS:
                    Log.d(TAG, "Movies loaded for server: " + currentServerId);
                    movieAdapter.clear();
                    if (resource.data != null && !resource.data.isEmpty()) {
                        movieAdapter.addAll(0, resource.data);
                    } else {
                        // TODO: Optionally add a "No movies found" card
                        Log.d(TAG, "No movies found for server: " + currentServerId);
                    }
                    break;
                case ERROR:
                    Log.e(TAG, "Error loading movies for " + currentServerId + ": " + resource.message);
                    // TODO: Optionally add an error card to the row
                    movieAdapter.clear();
                    break;
            }
        });

        viewModel.getMovieActionOutcome().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.status) {
                case LOADING:
                    Log.d(TAG, "Movie action loading for: " + (resource.data != null ? resource.data.getTitle() : "Unknown"));
                    // TODO: Show loading feedback in Leanback UI, perhaps a temporary message card or spinner
                    Toast.makeText(activity, "Processing: " + (resource.data != null ? resource.data.getTitle() : "..."), Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    Movie resultMovie = resource.data;
                    if (resultMovie != null) {
                        Log.d(TAG, "Movie action success: " + resultMovie.getTitle() + " state: " + resultMovie.getState());
                        if (resultMovie.getState() == Movie.COOKIE_STATE) {
                            // Util.openBrowserIntent(resultMovie, fragment, true, true); // Old call
                            // Assuming Util.openBrowserIntent is updated to take Activity and requestCode
                            if (getActivity() != null) {
                                Log.d(TAG, "Action outcome: COOKIE_STATE, opening browser for cookie fix (TV).");
                                Util.openBrowserIntent(getActivity(), resultMovie, MainFragmentViewModel.REQUEST_CODE_FIX_COOKIE);
                            }
                        } else if (resultMovie.getState() == Movie.VIDEO_STATE && resultMovie.getVideoUrl() != null && !resultMovie.getVideoUrl().isEmpty()) {
                            Util.openExoPlayer(resultMovie, activity, resultMovie.getStudio().equals(Movie.SERVER_IPTV));
                            viewModel.markAsWatched(resultMovie);
                        } else if (resultMovie.getState() == Movie.BROWSER_STATE && resultMovie.getVideoUrl() != null && !resultMovie.getVideoUrl().isEmpty()) {
                            Util.openBrowserIntent(resultMovie, fragment, false, false); // Pass fragment
                        } else if (resultMovie.getState() == Movie.PLAYLIST_STATE && Movie.SERVER_IPTV.equals(resultMovie.getStudio())) {
                            // This case might need special handling to refresh or load new IPTV group rows.
                            // For now, logging. A more complex solution might involve another LiveData for IPTV groups.
                            Log.d(TAG, "IPTV Playlist selected: " + resultMovie.getTitle() + ". Further expansion logic needed.");
                            Toast.makeText(activity, "Expanding IPTV Playlist: " + resultMovie.getTitle(), Toast.LENGTH_SHORT).show();
                            // Potentially trigger a new ViewModel method to load these specific sub-items
                            // viewModel.loadIptvSubGroup(resultMovie);
                        }
                        else {
                            Util.openVideoDetailsIntent(resultMovie, activity); // Default action
                        }
                    }
                    break;
                case ERROR:
                    Log.e(TAG, "Movie action error: " + resource.message);
                    Toast.makeText(activity, resource.message, Toast.LENGTH_LONG).show();
                    if (resource.data != null && resource.data.getState() == Movie.COOKIE_STATE) {
                        // Util.openBrowserIntent(resource.data, fragment, true, true); // Old call
                        if (getActivity() != null) {
                            Log.d(TAG, "Action outcome: ERROR but with COOKIE_STATE, opening browser for cookie fix (TV).");
                            Util.openBrowserIntent(getActivity(), resource.data, MainFragmentViewModel.REQUEST_CODE_FIX_COOKIE);
                        }
                    }
                    break;
            }
        });

        viewModel.getIptvPlaylistChannels().observe(getViewLifecycleOwner(), resource -> {
            if (rowsAdapter == null) return; // Ensure rowsAdapter is initialized

            // 1. Clear previous IPTV channel rows
            for (ListRow row : iptvChannelRows) {
                if (rowsAdapter.indexOf(row) >= 0) { // Check if row still exists
                   rowsAdapter.remove(row);
                }
            }
            iptvChannelRows.clear();
            // Note: totalHeadersCounter should be managed if these headers are dynamically added/removed often.
            // For simplicity, we might need a different way to generate unique header IDs if IPTV rows are mixed with server rows.

            if (resource.status == Resource.Status.LOADING) {
                // Optional: Add a single "Loading Channels..." row or use a Toast
                if (getActivity() != null) Toast.makeText(getActivity(), "Loading IPTV Channels...", Toast.LENGTH_SHORT).show();
            } else if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                int groupCounter = 0; // Counter for IPTV group header IDs to avoid conflicts with server rows
                for (Map.Entry<String, List<Movie>> entry : resource.data.entrySet()) {
                    String groupName = entry.getKey();
                    List<Movie> channels = entry.getValue();
                    if (channels == null || channels.isEmpty()) continue;

                    // Using a potentially large, offset ID for IPTV headers to minimize collision with server row header IDs
                    // A more robust solution would be a dedicated ID management or different adapter sections.
                    HeaderItem header = new HeaderItem(1000 + groupCounter++, groupName);
                    ArrayObjectAdapter channelAdapter = new ArrayObjectAdapter(new CardPresenter());
                    channelAdapter.addAll(0, channels);

                    ListRow listRow = new ListRow(header, channelAdapter);
                    rowsAdapter.add(listRow); // Add to the main rows adapter
                    iptvChannelRows.add(listRow);
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if (getActivity() != null) Toast.makeText(getActivity(), "Error loading IPTV channels: " + resource.message, Toast.LENGTH_LONG).show();
                // Optional: Add an error row or display message in UI
            }
        });
    }


    // This method might be simplified or removed if MainViewControl is fully phased out for row generation.
    // For now, it's called by the legacy MainViewControl reference if any part of it is still used.
    // If MainViewControl is fully removed, this method should be integrated into the ViewModel observation logic.
    // UPDATE: This method is no longer called by MainViewControl as MainViewControl is removed.
    // It could be a utility if needed by onActivityResult for example.
    private void extendMovieListOfHorizontalMovieAdapter(ArrayList<Movie> movies, ArrayObjectAdapter adapter) {
        Log.d(TAG, "extendMovieListOfHorizontalMovieAdapter: ");
        if (adapter == null) {
            Log.d(TAG, "extendMovieListOfHorizontalMovieAdapter: undefined adapter");
            return;
        }
        try {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: adapter> " + adapter.size());

                adapter.addAll(adapter.size(), movies);


            }
        });
        }catch (Exception e){
            Log.d(TAG, "extendMovieListOfHorizontalMovieAdapter: error: "+e.getMessage());
        }
    }

    private void updateRelatedMovieItem(ArrayObjectAdapter adapter, int clickedMovieIndex, Movie resultMovie) {
        if (adapter == null || resultMovie == null) {
            Log.d(TAG, "updateRelatedMovieItem: undefined adapter or movie");
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Log.d(TAG, "run: adapter> " + objectAdapter.size());
                adapter.replace(clickedMovieIndex, resultMovie);
            }
        });
    }

    // This method is problematic as it directly modifies rowsAdapter from a background thread
    // via MainViewControl. It needs to be refactored to be called from ViewModel observers on MainThread.
    // For now, its direct usage by MainViewControl might still occur if MainViewControl isn't fully removed.
    // However, the primary row generation should now happen in viewModel.serverConfigs.observe.
//    private ArrayObjectAdapter generateCategoryView(String title, ArrayList<Movie> movies, boolean isDefaultHeader) {
//        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter());
//        HeaderItem header = new HeaderItem(totalHeadersCounter++, title);
//
//        new Handler(Looper.getMainLooper()).post(() -> {
//            if (rowsAdapter != null) { // Check if rowsAdapter is initialized
//                 if (!movies.isEmpty()) {
//                    adapter.addAll(0, movies);
//                }
//                rowsAdapter.add(new ListRow(header, adapter));
//                if (isDefaultHeader) {
//                    defaultHeadersCounter++;
//                }
//            }
//        });
//        return adapter;
//    }


//    private void loadOmarServerHomepage(AbstractServer server) {
//
//
//        ExecutorService executor2 = Executors.newSingleThreadExecutor();
//        executor2.submit(() -> {
//            ArrayList<Movie> movies = server.getHomepageMovies(new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
//                @Override
//                public void onSuccess(ArrayList<Movie> result, String title) {
////                            generateCategory(title, result);
////                            if (server instanceof IptvServer) {
////                        //load history rows first
////                        loadHomepageHistoryRaws();
////
////                        loadMoviesRow(server, addRowToMainAdapter(server.getLabel()), null);
//////                            //channel list
//////                            ArrayList<Movie> channels = dbHelper.getIptvHomepageChannels();
//////                            if (channels.size() > 0) {
//////                                loadMoviesRow("tv", channels);
//////                            }
////                    } else {
//                    loadMoviesRow(server, addRowToMainAdapter(title), result);
////                    }
//                }
//
//                @Override
//                public void onInvalidCookie(ArrayList<Movie> result, String title) {
//                    Log.d(TAG, "onInvalidCookie: " + result);
////                            loadMoviesRow(server, addRowToMainAdapter(server.getLabel()), result);
//                    loadMoviesRow(server, addRowToMainAdapter(server.getLabel()), result);
//                }
//
//                @Override
//                public void onInvalidLink(ArrayList<Movie> result) {
//
//                }
//
//                @Override
//                public void onInvalidLink(String message) {
//
//                }
//            });
//
//        });
//        executor2.shutdown();
//
////        ExecutorService executor = Executors.newSingleThreadExecutor();
////        executor.submit(() -> {
////            if (activity != null) {
////                ArrayList<Movie> movies = server.getHomepageMovies(new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
////                    @Override
////                    public void onSuccess(ArrayList<Movie> result, String title) {
////                        Map<String, List<Movie>> moviesByCategory = result.stream()
////                                .flatMap(movie -> movie.getCategories().stream()
////                                        .map(category -> new AbstractMap.SimpleEntry<>(category, movie)))
////                                .collect(Collectors.groupingBy(
////                                        Map.Entry::getKey,
////                                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
////                                ));
////
////                        // Assuming you already have the 'moviesByCategory' map
////                        for (Map.Entry<String, List<Movie>> entry : moviesByCategory.entrySet()) {
////                            String category = entry.getKey();
////                            List<Movie> moviesInCategory = entry.getValue();
////                            Log.d(TAG, "loadOmarServerHomepage: " + category + ", " + moviesInCategory.size());
////                            loadMoviesRow(server, addRowToMainAdapter(category), (ArrayList) moviesInCategory);
////                        }
////                    }
////
////                    @Override
////                    public void onInvalidCookie(ArrayList<Movie> result) {
////
////                    }
////
////                    @Override
////                    public void onInvalidLink(ArrayList<Movie> result) {
////
////                    }
////
////                    @Override
////                    public void onInvalidLink(String message) {
////
////                    }
////                });
////
////            }
////
////        });
//    }

//    private void loadMoviesRow(AbstractServer server, ArrayObjectAdapter adapter, ArrayList<Movie> moviesList) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.submit(() -> {
//            if (activity == null) {
//                return;
//            }
//            final ArrayList<Movie> movies; // Declare as effectively final
////                if (moviesList == null && server != null) {
//            if (server == null) {
//                return;
//            }
//            Log.d(TAG, "loadMoviesRow: " + server.getServerId());
//            movies = server.getHomepageMovies(new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
//                @Override
//                public void onSuccess(ArrayList<Movie> result, String title) {
//                    Log.d(TAG, "loadMoviesRow: onSuccess: " + result + ", " + title);
//                    if (result.isEmpty()) {
//                        return;
//                    }
//                    Movie sampleMovie = result.get(0);
//                    if (sampleMovie != null && sampleMovie.getVideoUrl() != null) {
//                        ServerConfig config = ServerConfigManager.getConfig(server.getServerId());
//                        if (null != config) {
//                            updateDomain(sampleMovie.getVideoUrl(), config, dbHelper);
//                        }
//                    }
//
//                    if (adapter == null) {
//                        return;
//                    }
//
//                    mHandler.post(() -> {
//                        adapter.addAll(0, result);
//                        rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
////                            adapter.notifyItemRangeChanged(0, adapter.size());
//                    });
////                                adapter.notifyItemRangeInserted(0, result.size());
//
//                }
//
//                @Override
//                public void onInvalidCookie(ArrayList<Movie> result, String title) {
//                    Log.d(TAG, "loadMoviesRow: onInvalidCookie: " + result.size());
//                    if (result.isEmpty()) {
//                        return;
//                    }
//
//                    mHandler.post(() -> {
//                        // String newName = rowName + ("(" + finalMovieList.size() + ")");
//                        if (adapter != null) {
//                            adapter.addAll(0, result);
//                        }
//                    });
//                }
//
//                @Override
//                public void onInvalidLink(ArrayList<Movie> result) {
//                    Log.d(TAG, "loadMoviesRow: onInvalidLink: " + result);
//                }
//
//                @Override
//                public void onInvalidLink(String message) {
//                    Log.d(TAG, "loadMoviesRow: onInvalidLink: " + message);
//                }
//            });
//
//        });
//
//        executor.shutdown();
//    }

    //todo clarify and optimize
//    private void updateDomain(String movieLink, ServerConfig config, MovieDbHelper dbHelper) {
//        String newDomain = Util.extractDomain(movieLink, true, false);
//        boolean equal = config.getUrl().contains(newDomain);
//        Log.d(TAG, "updateDomain: old: " + config.getUrl() + ", new: " + newDomain + ", = " + (equal));
//        if (!equal) {
//            config.setUrl(newDomain);
//            config.setReferer(newDomain + "/");
////            ServerConfigManager.updateConfig(config);
//
////            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
////            Log.d(TAG, "addServerConfigsToDB: ");
////            Date date = null;
////            try {
////                date = format.parse("2024-02-22T12:30:00");
////            } catch (ParseException e) {
////                date = new Date();
////            }
////            dbHelper.saveServerConfigAsCookieDTO(config, date);
//            ServerConfigManager.updateConfig(config, dbHelper);
//        }
//    }

//    private void loadMoviesRow_2(String label, ArrayList<Movie> movies) {
//        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter());
//        HeaderItem header = new HeaderItem(HEADER_ROWS_COUNTER++, label);
//        rowsAdapter.add(new ListRow(header, adapter));
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.submit(() -> {
//            if (activity != null) {
//                activity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        // String newName = rowName + ("(" + finalMovieList.size() + ")");
//                        adapter.addAll(0, movies);
//                    }
//                });
//            }
//
//        });
//
//        executor.shutdown();
//    }


//    private void loadHomepageHistoryRaws() {
//
////        ExecutorService executor = Executors.newSingleThreadExecutor();
////        executor.submit(() -> {
//        try {
//            ArrayList<Movie> historyMovies = dbHelper.getAllHistoryMovies(false);
//            loadMoviesRow(null, addRowToMainAdapter("المحفوظات"), historyMovies);
//
//            ArrayList<Movie> iptvHistoryMovies = dbHelper.getAllHistoryMovies(true);
//            loadMoviesRow(null, addRowToMainAdapter("محفوظات القنوات"), iptvHistoryMovies);
//        } catch (Exception e) {
//            Log.d(TAG, "loadHomepageRaws: error loading historyRows: " + e.getMessage());
//        }
//
////        });
////        executor.shutdown();
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data); // Important for Fragment lifecycle
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == MainFragmentViewModel.REQUEST_CODE_FIX_COOKIE) {
            Movie movieFromBrowser = null;
            if (data != null && data.hasExtra(com.omerflex.view.DetailsActivity.MOVIE)) {
                movieFromBrowser = data.getParcelableExtra(com.omerflex.view.DetailsActivity.MOVIE);
                 if (movieFromBrowser != null) {
                    Log.d(TAG, "onActivityResult: Received movie from BrowserActivity: " + movieFromBrowser.getTitle());
                } else {
                    Log.d(TAG, "onActivityResult: movieFromBrowser is null despite data.hasExtra being true.");
                }
            } else {
                 Log.d(TAG, "onActivityResult: No movie data in intent from BrowserActivity for REQUEST_CODE_FIX_COOKIE.");
            }
            if (viewModel != null) { // viewModel should be initialized
                viewModel.processCookieFixResult(resultCode, movieFromBrowser);
            } else {
                Log.e(TAG, "onActivityResult: viewModel is null, cannot process cookie fix result.");
            }
        }
        // Other onActivityResult logic if any...
//
//        if (resultCode != Activity.RESULT_OK || data == null) {
//            Log.d(TAG, "onActivityResult:RESULT_NOT_OK ");
//            return;
//        }
//        if (clickedMovie == null) {
//            Log.d(TAG, "onActivityResult: clickedMovie is not identified");
//            return;
//        }
//        ArrayObjectAdapter objectAdapter = getServerAdapter(clickedMovie.getStudio());
////        AbstractServer server = ServerManager.determineServer(clickedMovie, objectAdapter, getActivity(), fragment);
//        AbstractServer server = ServerConfigManager.getServer(clickedMovie.getStudio());
//        if (server == null) {
//            Log.d(TAG, "onActivityResult: unknown server: " + clickedMovie.getStudio() + ", state: " + clickedMovie.getState());
//            return;
//        }
//        String result = data.getStringExtra("result");
//        ArrayList<Movie> resultMovieSublist = data.getParcelableArrayListExtra(DetailsActivity.MOVIE_SUBLIST);
//
//        if (requestCode == 0 || requestCode == Movie.REQUEST_CODE_MOVIE_LIST) {
//                    Thread t = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
////                            Movie movie = new Movie();
////                            movie.setTitle(query);
////                            movie.setStudio(result);
//
////                            Log.d(TAG, "run: after renewing the cookie:" + server.getHeaders());
//                            //List<Movie> movies = server.search(query);
////                            Gson gson = new Gson();
////                            Type movieListType = new TypeToken<List<Movie>>() {
////                            }.getType();
////                            List<Movie> movies = gson.fromJson(result, movieListType);
////                            if (movies == null){
////                                Log.d(TAG, "onActivityResult run: fail converting result to movies object ");
////                                return;
////                            }
//
//                            if (clickedMovie.getState() == Movie.COOKIE_STATE) {
//                                for (Movie mov : resultMovieSublist) {
//                                    //todo: check if still needed to redetect the movie state
//                                    mov.setState(server.detectMovieState(mov));
//                                    //sets main movie to it self same as search method as renewing cookie only search for movies
//                                    mov.setMainMovie(mov);
//                                    mov.setMainMovieTitle(mov.getTitle());
////                                    if (server.isSeries(mov)){
////                                        movies.get(movies.indexOf(mov)).setState(Movie.GROUP_OF_GROUP_STATE);
////                                    }else {
////                                        movies.get(movies.indexOf(mov)).setState(Movie.ITEM_STATE);
////                                    }
//                                }
//                            }
//
//                            if (!resultMovieSublist.isEmpty()) {
//                                getActivity().runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Log.d(TAG, "run: adapter> " + objectAdapter.size());
//                                        objectAdapter.addAll(1, resultMovieSublist);
//                                    }
//                                });
//                            } else {
//                                getActivity().runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Toast.makeText(getContext(), "لايوجد نتائج", Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            }
//
//                        }
//                    });
//                    t.start();
//                    Log.d(TAG, "onActivityResult: cookies:");
////
////                    Gson gson = new Gson();
////                    Type movieListType = new TypeToken<List<Movie>>(){}.getType();
////                    List<Movie> movies = gson.fromJson(result, movieListType);
////                    Log.d(TAG, "onActivityResult: movie:"+movies.toString());
//
//            return;
//
//        }
//            Thread t = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    List<Movie> movies = server.search(clickedMovie.getTitle(), new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
//                        @Override
//                        public void onSuccess(ArrayList<Movie> result, String title) {
//                            Log.d(TAG, "onSuccess: ");
//                            if (result == null || result.isEmpty()){
//                                Log.d(TAG, "onSuccess: onActivityResult empty result");
//                            return;
//                            }
//                            getActivity().runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Log.d(TAG, "run: adapter> " + objectAdapter.size());
//                                    objectAdapter.addAll(1, result);
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onInvalidCookie(ArrayList<Movie> result) {
//                            Log.d(TAG, "onInvalidCookie: ");
//                            if (result == null || result.isEmpty()){
//                                Log.d(TAG, "onInvalidCookie: onActivityResult empty result");
//                                return;
//                            }
//                            getActivity().runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Log.d(TAG, "run: adapter> " + objectAdapter.size());
//                                    objectAdapter.addAll(1, result);
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onInvalidLink(ArrayList<Movie> result) {
//
//                        }
//
//                        @Override
//                        public void onInvalidLink(String message) {
//                            Log.d(TAG, "onInvalidLink: "+message);
//                        }
//                    });
//                }
//            });
//            t.start();
//
        //hier



//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getContext(), "لايوجد نتائج", Toast.LENGTH_SHORT).show();
//            }
//        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateService.handleOnRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void test() {

//        loadHomepageRaws();


//        Movie mm = new Movie();
//    String url = "https://m.asd.quest/%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d9%84%d8%b9%d8%a8%d8%a9-%d8%ad%d8%a8-%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-39-%d8%a7%d9%84%d8%aa%d8%a7%d8%b3%d8%b9%d8%a9-%d9%88%d8%a7%d9%84%d8%ab%d9%84%d8%a7%d8%ab%d9%88/";
//    //String url = "https://main4.ci4u.co/%d9%81%d9%8a%d9%84%d9%85-your-christmas-or-mine-2022-%d9%85%d8%aa%d8%b1%d8%ac%d9%85-%d8%a7%d9%88%d9%86-%d9%84%d8%a7%d9%8a%d9%86/?wat=1";
////    String url = "https://cimatube.cc/embed1/29f1d50ae92194ed586eb34c47a41945";
////    String url = "https://www.faselhd.express/video_player?uid=0&vid=863adbd5b09c0b764128cec2dcb1d84f&img=https://img.scdns.io/thumb/863adbd5b09c0b764128cec2dcb1d84f/large.jpg&nativePlayer=true";
//    String url = "https://m.gamezone.cam/blastman/play/?playit=4e5467344e54497a&fgame=aHR0cHM6Ly9tLmFzZC5ob21lcw==HhZ35wNXMr,,A838GBww1vdTL2F9AJxvrJvWstQaumxqBnXP6SaihNYaPEZa11xkTUWadvZZ66NZ3k0wmtvG_SRztVSfOiWIdHXBlao1UsSSYbNheJsyjywQ2FZNcD9tWIwPhGXYVS9YVmrn1jprLEwl8WqV1art8jZh32Al1Kbp1_VPDYZa8x7om2IrH9aQa6wwVkF24Z8Cot7f9TbGbGlWsKCnMi7Na9bbrza5sBTH_xiFP2DCBkcLFDrkw2lfbcdoCzPTou4Rafy3zvgDpbHgCJ5emNfuKyC67g-cotgx-AGgE7pyBYIuRzIL4gjlves52m7YnmvY0aoimtxyQfTRHWFGSgvQvsSktMyrbe7teUkYL-kUySwqxv81HgvG6jASzQULbYAXYk3zRZ2176TtMyyqruvcvnadVR4c5kyPbLf873rXcpGJPmrScq48wdKPUEaQAhGw6IPxolMBUfi_eRQAzgylEkdqNe";
//    String url = "https://wecima.show/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-fox-spirit-matchmaker-love-in-pavilion-%d9%85%d9%88%d8%b3%d9%85-1-%d8%ad%d9%84%d9%82%d8%a9-10/";
//    String url = "https://www.google.com/search?sca_esv=792c36b2414f597c&sca_upv=1&sxsrf=ADLYWILq2GQVLfS6OtWBOZttfUz_gO2FpA:1716908543425&q=ss&uds=ADvngMgnp21tvUWATbVPNUHyrBahasC3_Xskxp-yVqIWaah14uWwZGyEs8SEQnojasvFy1klDAGiK1X000V1u8TMw8WbPd4mdOSf3Z-_WfznXIH3KMxOFX-WPTEBgiVxKEucOwT4nWYlEkxizOQdCtNhrw_D4bTD33sKjMmA9bKU2UEcPXFk20y77h4YYmxpfxmbKeQt9hRlNGWgX8Tf0bU6xIx3rtYCluTxCDf7XbrXNNJ2yGCiJpA&udm=2&prmd=ivnbz&sa=X&ved=2ahUKEwjIw97ezrCGAxW_A9sEHf-IDPkQtKgLegQIDRAB";
//    mm.setVideoUrl(url);
//    mm.setStudio(Movie.SERVER_ARAB_SEED);
//    mm.setState(Movie.ITEM_STATE);
//        Intent browse = new Intent(getActivity(), BrowserActivity.class);
//         browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//         getActivity().startActivity(browse);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

//            AbstractServer iptv = IptvServer.getInstance(activity, fragment);
//            ServerConfig iptvConfig = new ServerConfig();
//            iptvConfig.url = "https://cimanow.cc";
//            iptv.setConfig(iptvConfig);
//            ArrayList<Movie> movies = iptv.getHomepageMovies();
////            ArrayList<Movie> movies = iptv.search("الجزيرة");
//            Log.d(TAG, "test: movies:"+movies.size());
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(iptv.getLabel(), movies);
//            }

//           ControllableServer cimaNow = CimaNowController.getInstance(activity, fragment);
//            ServerConfig cimaNowConfig = new ServerConfig();
//            cimaNowConfig.url = "https://cimanow.cc";
//            cimaNow.setConfig(cimaNowConfig);
//            ArrayList<Movie> movies = cimaNow.getHomepageMovies();
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(cimaNow.getLabel(), movies);
//            }
//
//            ControllableServer arabseed = ArabSeedController.getInstance(fragment, activity);
//            ServerConfig arabseedConfig = new ServerConfig();
//            arabseedConfig.url = "https://arabseed.show";
//            arabseed.setConfig(arabseedConfig);
//            ArrayList<Movie> movies = arabseed.search("الخائن");
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(arabseed.getLabel(), movies);
//            }

//            ControllableServer cima = CimaClubController.getInstance(fragment, activity);
//            ServerConfig config = new ServerConfig();
//            config.url = "https://cimaclub.top";
//            cima.setConfig(config);
//            ArrayList<Movie> movies = cima.getHomepageMovies();
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(cima.getLabel(), movies);
//            }

//            ControllableServer fasel = FaselHdController.getInstance(fragment, activity);
//            ServerConfig faselConfig = new ServerConfig();
//            faselConfig.url = "https://www.faselhd.link";
//            fasel.setConfig(faselConfig);
//            ArrayList<Movie> faselMovies = fasel.getHomepageMovies();
//            if (faselMovies != null && faselMovies.size() > 0) {
//                loadMoviesRow(fasel.getLabel(), faselMovies);
//            }

//                        AbstractServer mycima = MyCimaServer.getInstance(activity, fragment);
//            ServerConfig mycimaConfig = new ServerConfig();
//            mycimaConfig.url = "https://wecima.show/";
//            mycima.setConfig(mycimaConfig);
//            ArrayList<Movie> mycimaMovies = mycima.getHomepageMovies();
//            if (mycimaMovies != null && mycimaMovies.size() > 0) {
//                loadMoviesRow(mycima, addRowToMainAdapter(mycima.getLabel()), mycimaMovies);
//            }
//
//
//        });
//        executor.shutdown();


//        ControllableServer fasel = FaselHdController.getInstance(fragment, activity);
//            ServerConfig faselConfig = new ServerConfig();
//            faselConfig.url = "https://www.faselhd.link";
//            fasel.setConfig(faselConfig);

//            Intent searchResultIntent = new Intent(getActivity(), SearchResultActivity.class);
//            searchResultIntent.putExtra("query", "doc");
//            startActivity(searchResultIntent);


//        searchResultIntent.putExtra("query", "الغريب");
//        hhhhhhh     searchResultIntent.putExtra("query", "bab");
            // setResult(Activity.RESULT_OK,returnIntent);
            //  finish();


            //hhhhhhh       Intent browse = new Intent(getActivity(), BrowserActivity.class);
            //hhhhhhh browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
            //hhhhhhh getActivity().startActivity(browse);

//    Locale locale = new Locale("en");
//    Locale.setDefault(locale);
//    Configuration config = new Configuration();
//    config.locale = locale;
//    getActivity().getBaseContext().getResources().updateConfiguration(config,
//            getActivity().getBaseContext().getResources().getDisplayMetrics());


            // dbHelper.cleanMovieList();

            //  loadServerRow("شاهد", Shahid4uController.getInstance(getActivity()), "game of thrones" );


            //            loadServerRow("أفلام",AkwamController.getInstance(getActivity()), "spider" );
            //loadServerRow("ماي سيما", MyCimaController.getInstance(), "https://wecima.actor/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2023-series-ramadan-2023/list/" );
            //loadServerRow("ماي سيما", MyCimaController.getInstance(), "https://mycima22.wecima.cam/seriestv/" );
//    loadServerRow("ماي سيما", MyCimaController.getInstance(), "https://mycima.uno/genre/%d9%83%d9%88%d9%85%d9%8a%d8%af%d9%8a%d8%a7-comedy/" );
            // loadServerRow("سيمافوريو", Cima4uController.getInstance(getActivity()), "https://cima4u.mx/netflix/" );
            //     loadServerRow("أفلام",new AkwamController(new ArrayObjectAdapter(new CardPresenter()), getActivity()), "https://akwam.co/movies" );


//    Movie mm = new Movie();
//    //String url = "https://ui.cima4u.bio/category/%D8%A7%D9%81%D9%84%D8%A7%D9%85-%D8%A7%D8%AC%D9%86%D8%A8%D9%8A/";
//    //String url = "https://main4.ci4u.co/%d9%81%d9%8a%d9%84%d9%85-your-christmas-or-mine-2022-%d9%85%d8%aa%d8%b1%d8%ac%d9%85-%d8%a7%d9%88%d9%86-%d9%84%d8%a7%d9%8a%d9%86/?wat=1";
//    String url = "https://cimatube.cc/embed1/29f1d50ae92194ed586eb34c47a41945";
//    String url = "http://194.164.53.40/movie/fetch/476.ts";
//    String url = "https://airmax.boats:443/auth/klAOuM1eGh8S_Lad-zHd3tRQQNJwPy1xC9daIKIlJuJa8zkzWpcOan7LNqE9MedsBm09PjbO3z6TCiFHsThjg12o-u_0Ww3o-KVci707p08j-Koinxoj9Y2raOyRkw_gr2ryf7Ym05pUaypNUlIacLXMdHBxaKoqnFMeCkcp71RgpYPmy4ufp1dvhmwFGpXt_XE6SzUrixQBykbQsx7juDx9NqP_jZboMASNoLFq9cTm2dnpEidnkvrWbV8yPlWAiBfBYKGgc-JzAlbEo_XaFeoyQPAjMzYke7_R0YbPF216F3UGKoxOz62NakIOPllj71AJFID2EOM-b00cRvcxTPmRrJV927RnWnhRhxw-zc58ubdxI6xeqEY8kPduZg3skgZTkEMUdHIhvxFZ4S6LxbdPz8LRYtCUtkzCDAMoWPGU2ZMHlOMRrQGjBHmVBlpwQWfbMsu8Dva3Djgg8z0MicYYB2bxGAgyRhc_oRTF3meVkzTickaaKmduESCg_QmBNzUjmKCPGwxg26OT2WxlnpxsbFELoqiaR0P8Lflm6poO0_u-ES4FHMX0oiikTLQTirIs3N9TcyYZMyp75kCrJQkHQ9yII3eLeRU322WzRv-9CiPzeijfs94mlzBBNZtbgLSD_6yjnSh-jW0pHYC3DA";
////    String url = "https://www.faselhd.express/video_player?uid=0&vid=863adbd5b09c0b764128cec2dcb1d84f&img=https://img.scdns.io/thumb/863adbd5b09c0b764128cec2dcb1d84f/large.jpg&nativePlayer=true";
//    String url = "https://www.faselhd.link/movies/%d9%81%d9%8a%d9%84%d9%85-sonic-hedgehog-2020-%d9%85%d8%aa%d8%b1%d8%ac%d9%85-ct";
//    String url = "https://tgb4.top15top.shop/0bboz4svw11q/Sonic_the_Hedgehog_2020_Bluray-1080p.Weciima.mp4.html?Key=ibPyARbr1aJuVz7ibn-kng&Expires=1728899987|referer=https://wecima.movie/";
//    String url = "https://deva-cpmav9sk6x33.cimanowtv.com/uploads/2024/10/17/_Cima-Now.CoM_%20El.Moass.Osman.S06E03.HD/[Cima-Now.CoM]%20El.Moass.Osman.S06E03.HD-480p.mp4|Referer=https://deva-cpmav9sk6x33.cimanowtv.com/e/2ss18e78qfgj";
//    String url = "https://deva-cpmav9sk6x35.cimanowtv.com/uploads/2024/10/18/_Cima-Now.CoM_%20Bagman.2024.HD/[Cima-Now.CoM]%20Bagman.2024.HD-480p.mp4|sec-ch-ua=\"Chromium\";v=\"130\", \"Android WebView\";v=\"130\", \"Not?A_Brand\";v=\"99\"&sec-ch-ua-mobile=?1&Accept=*/*&sec-ch-ua-platform=\"Android\"&User-Agent=Mozilla/5.0 (Linux; Android 14; SM-S908B Build/UP1A.231005.007; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.58 Mobile Safari/537.36&Referer=https://deva-cpmav9sk6x35.cimanowtv.com/e/525yih97u1vs&Accept-Encoding=identity;q=1, *;q=0&Range=bytes=0-";
//    String url = "blob:https://vk.com/d08eb16f-d03e-4743-8a27-8158f72c1c7b|Referer=https://vk.com/&User-Agent=Android 8";
//    mm.setVideoUrl(url);
//    mm.setStudio(Movie.SERVER_CimaNow);
//    mm.setState(Movie.VIDEO_STATE);
//    mm.setState(Movie.RESOLUTION_STATE);
//    mm.setState(Movie.BROWSER_STATE);
//
//            Map<String, String> headers = Util.extractHeaders(url);
//
//            headers.forEach((key, value) -> System.out.println(key + ": " + value));
//
//
//Util.openExoPlayer(mm, getActivity(), false);
//Util.openBrowserIntent(mm, fragment, false, false);
//Util.openExternalVideoPlayer(mm, getActivity());
//    Intent browse = new Intent(getActivity(), BrowserActivity.class);
//     browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//     getActivity().startActivity(browse);
//            String urlAuth = "http://airmaxtv.de/airmaxfree/IBO/auth";
//
//            HashMap<String, String> headers = new HashMap<>();
//            headers.put("User-Agent", "smart-tv");
//            headers.put("Content-Type", "application/json; charset=utf-8");
//            headers.put("Host", "airmaxtv.de");
//            headers.put("Connection", "Keep-Alive");
//            headers.put("Accept-Encoding", "gzip");
////            headers.put("Content-Length", "357");
//
//
//            try {
////              Document doc = Jsoup.connect(urlAuth)
//////                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
////                        .headers(headers)
//////                        .cookies(config.getMappedCookies())
//////                    .userAgent("Android 7")
//////                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
////                        .followRedirects(true)
////                        .ignoreHttpErrors(true)
////                        .ignoreContentType(true)
////                        .timeout(0)
////                        .get();
//String urlUp = "http://airmaxtv.de/airmaxfree/IBO/update";
//                Document doc2 = Jsoup.connect(urlUp)
////                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                        .headers(headers)
////                        .cookies(config.getMappedCookies())
////                    .userAgent("Android 7")
////                    .userAgent("Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86 Build/OSM1.180201.031; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 Mobile Safari/537.36")
//                        .followRedirects(true)
//                        .ignoreHttpErrors(true)
//                        .ignoreContentType(true)
//                        .timeout(0)
//                        .data("mimeType", "application/json; charset=utf-8")
//                        .data("text",  "{\"data\":\"eyJtYWNfYWRkcmVzcyI6IjNiOjZiOjUzOmJhOmU4OjA42zRmIiwicGxheWxpc3RfaWQiOiI5MCIsInBs\\nYXlsaXN0X25hbWUiOiJhaXJNQVggU2VydmVyIDEiLCJwbGF5bGlzdF91cmwiOiJodHRwOlwvXC9h\\naXJtYXhsaXZlLmJvYXRzXC9nZXQucGhwP3VzZXJuYW1lPWFpcm1heHR2JnBhc3N3b3JkPTU1NDg1\\nMzU0Jm91dHB1dD10cyZ0eXBlPW0zdV9wbHVzIiwicGxheWxpc3RfdHlwZSI6ImdlbmVyYWwiLCJh\\ncHBfdHlwZSI6ImFuZHJvaWQifQ==Qe\"}")
//                        .post();
//                Log.d(TAG, "test: "+doc2.outerHtml());
////            String docTitle = doc.title();
////            Log.d(TAG, "getSearchRequestDoc: " + docTitle);
////            if (docTitle.contains("Just a moment")) {
////                return fetchDocUsingWebView(url);
////            }
//
//            } catch (IOException e) {
//                //builder.append("Error : ").append(e.getMessage()).append("\n");
//                Log.i(TAG, "error: test: " + e.getMessage());
//            }


//            M3U8ContentFetcher mFetcher = new M3U8ContentFetcher();
//            String contents = "\n" +
//                    "#EXTM3U\n" +
//                    "#EXTINF:-1 tvg-id=\"EsTuTele.es\" tvg-logo=\"https://i.imgur.com/bsAbcmC.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",EsTuTele \n" +
//                    "#EXTVLCOPT:http-referrer=https://player.streamingconnect.com/\n" +
//                    "https://cloud.streamingconnect.tv/hls/estutele/estutele.m3u8" +
//                    "https://5d8d85cf2c308.streamlock.net:1936/CanalLuz/enDirecto/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalMalagaRTV.es\" tvg-logo=\"https://i.imgur.com/QSUOxtM.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",Canal Málaga RTV  \n" +
//                    "https://canalmalaga-tv-live.flumotion.com/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalParlamento.es\" tvg-logo=\"https://i.imgur.com/BUO0wH6.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",Canal Parlamento  \n" +
//                    "https://congresodirecto.akamaized.net/hls/live/2037973/canalparlamento/master.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalRed.es\" tvg-logo=\"https://i.imgur.com/6pExdIs.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",Canal Red \n" +
//                    "https://ythls.armelin.one/channel/UCky112obBMG68Nw5MrSNNPA.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalReusTV.es\" tvg-logo=\"https://i.imgur.com/HYRc9sJ.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\" user-agent=\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36 CrKey/1.44.191160\",Canal Reus TV \n" +
//                    "#EXTVLCOPT:http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36 CrKey/1.44.191160\n" +
//                    "https://liveingesta318.cdnmedia.tv/canalreustvlive/smil:live.smil/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalSanRoque.es\" tvg-logo=\"https://i.imgur.com/QBzvCJc.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",Canal San Roque  \n" +
//                    "https://cdnlivevlc.codev8.net/aytosanroquelive/smil:channel1.smil/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalSierradeCadiz.es\" tvg-logo=\"https://i.imgur.com/94rn7pb.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",Canal Sierra de Cádiz  \n" +
//                    "https://s.emisoras.tv:8081/sierradecadiz/index.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalSur2.es\" tvg-logo=\"https://i.imgur.com/VZjtGvm.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",Canal Sur Andalucía 2 \n" +
//                    "https://cdnlive.codev8.net/rtvalive/smil:channel22.smil/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalSurAndalucia.es\" tvg-logo=\"https://i.imgur.com/fJhkyZP.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\",Canal Sur Andalucía  \n" +
//                    "https://cdnlive.codev8.net/rtvalive/smil:channel1.smil/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalTaronjaAnoia.es\" tvg-logo=\"https://i.imgur.com/RP8VcqZ.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\" user-agent=\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36 CrKey/1.44.191160\",Canal Taronja Anoia  \n" +
//                    "#EXTVLCOPT:http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36 CrKey/1.44.191160\n" +
//                    "https://ingest1-video.streaming-pro.com/canaltaronja/anoia/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalTaronjaComarquesCentrals.es\" tvg-logo=\"https://i.imgur.com/RP8VcqZ.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\" user-agent=\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36 CrKey/1.44.191160\",Canal Taronja Comarques Centrals  \n" +
//                    "#EXTVLCOPT:http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36 CrKey/1.44.191160\n" +
//                    "https://ingest1-video.streaming-pro.com/canaltaronja/central/playlist.m3u8\n" +
//                    "#EXTINF:-1 tvg-id=\"CanalTaronjaOsonaiMoianes.es\" tvg-logo=\"https://i.imgur.com/RP8VcqZ.png\" group-title=\"\uD83C\uDDEA\uD83C\uDDF8 Spain\" user-agent=\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36 CrKey/1.44.191160\",Canal Taronja Osona i Moianés  " +
//                    "\n" +
//                    "\n" +
//                    "\n" +
//                    "#EXTM3U\n" +
//                    "#EXTINF:-1 tvg-name=\"Das Erste Ⓖ\" tvg-logo=\"https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Das_Erste_2014.svg/640px-Das_Erste_2014.svg.png\" tvg-id=\"DasErste.de\" group-title=\"\uD83C\uDDE9\uD83C\uDDEA Germany\",Das Erste Ⓖ\n" +
//                    "https://mcdn.daserste.de/daserste/de/master.m3u8\n" +
//                    "#EXTINF:1 tvg-id=\"RTLZweiDeutschland.de\" tvg-language=\"German\" tvg-logo=\"https://i.imgur.com/0dDMVLa.png\" group-title=\"\uD83C\uDDE9\uD83C\uDDEA Germany\",RTL Zwei\n" +
//                    "https://s6.hopslan.com/rtl2x1/tracks-v1a1/mono.m3u8\n" +
//                    "#EXTINF:1 tvg-id=\"RTLZweiDeutschland.de\" tvg-language=\"German\" tvg-logo=\"https://i.imgur.com/0dDMVLa.png\" group-title=\"\uD83C\uDDE9\uD83C\uDDEA Germany\",RTL Zwei+\n" +
//                    "http://178.219.128.68:64888/RTL2"
//                    ;
//

//            Movie iptvMovie = new Movie();
////            iptvMovie.setVideoUrl("https://raw.githubusercontent.com/airtech35/airtech35/airtech35-patch-1/allero");
//            iptvMovie.setVideoUrl("https://raw.githubusercontent.com/airtech35/airtech35/refs/heads/airtech35-patch-1/Serier2023");
////            List<Movie> movies = M3U8ContentFetcher.parseGroupNames(contents, "hash");
//            M3U8ContentFetcher.fetchAndStoreM3U8Content(iptvMovie, dbHelper, result -> {
//                // Update UI with the result
////                updateChannelGroups(result);
//                Log.d(TAG, "movies: "+ result.size());
//            });
//            HashMap<String, ArrayList<IptvSegmentDTO>> movies = M3U8ContentFetcher.parseContentWithStreaming(contents, "hash");



















//hhhhhhh
            //hhhhhhh       Movie mm = new Movie();
            //hhhhhhh         mm.setStudio(Movie.SERVER_CIMA_CLUB);
            //mm.setState(Movie.ITEM_STATE);
            //hhhhhhh          mm.setState(Movie.BROWSER_STATE);
            //hhhhhhh       mm.setTitle("test");
            //hhhhhhh        mm.setCardImageUrl("www.google.com");
//        mm.setVideoUrl("https://tv.cima4u.mx/Video/Sonic+the+Hedgehog+2+2022-50604.html");
//       // mm.setVideoUrl("https://www.faselhd.club/?p=194950");
            // mm.setVideoUrl("https://ciima-clup.quest/c135");
            //  mm.setVideoUrl("https://akwam.us/movie/9152/%D9%86%D8%A8%D9%8A%D9%84-%D8%A7%D9%84%D8%AC%D9%85%D9%8A%D9%84-%D8%A3%D8%AE%D8%B5%D8%A7%D8%A6%D9%8A-%D8%AA%D8%AC%D9%85%D9%8A%D9%84");
            //hhhhhhh         mm.setVideoUrl("https://akwam.us/download/149205/9152/%D9%86%D8%A8%D9%8A%D9%84-%D8%A7%D9%84%D8%AC%D9%85%D9%8A%D9%84-%D8%A3%D8%AE%D8%B5%D8%A7%D8%A6%D9%8A-%D8%AA%D8%AC%D9%85%D9%8A%D9%84");
            //      mm.setVideoUrl("#");
//hhh        mm.setVideoUrl("https://cimclllb.sbs/watch/مسلسل-the-wheel-of-time-الموسم-الثاني-الحلقة-5-الخامسة");
//      //  mm.setVideoUrl("https://www.faselhd.ac/?s=sonic");
            //mm.setVideoUrl("https://www.faselhd.club/seasons/%d9%85%d8%b3%d9%84%d8%b3%d9%84-sonic-prime");

////        Log.d(TAG, "loadRows: cookie:"+CookieManager.getInstance().getCookie(mm.getVideoUrl()));


            //hhhhhhh

//            Intent browse = new Intent(getActivity(), BBrowserActivity.class);
//    browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//        getActivity().startActivity(browse);
//        getActivity().startActivity(browse);


//        String newUrl=  "https://k301o.dood.video/u5kj6egmx3flsdgge7bf4osedb4eegnjgawimg7akxdvtb3wrznktqo5c5wq/qzk1uews13~tnLJC64N1E?token=3gyjneucz05go388u7xfnho4&expiry=1673037884992|Referer=https://dood.pm/";
////        String newUrl=  "https://k301o.dood.video/u5kj6egmx3flsdgge7bf4osedb4eegnjgawimg7akxdvtb3wrznjrmw5c5wq/u4sjgmzxhu~LpgSZlwKGZ|token=3gyjneucz05go388u7xfnho4&expiry=1673009930631";
//        String type = "video/*"; // It works for all video application
//        Uri uri = Uri.parse(newUrl);
//        Log.d("yessss2", uri + "");
//        Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
//        in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        //  in1.setPackage("org.videolan.vlc");
//        in1.setDataAndType(uri, type);
//        // view.stopLoading();
//        startActivity(in1);


            // loadServerRow("سيماكلوب", CimaClubController.getInstance(getActivity()), "game of thrones" );
            //     loadServerRow("ماي سيما", MyCimaController.getInstance(), "game of thrones" );
            //    loadServerRow("كورة",new KooraLiveController(new ArrayObjectAdapter(new CardPresenter()), getActivity()), "https://www.yallashoote.com" );
            //  loadServerRow("اكوام القديم", OldAkwamController.getInstance(getActivity()), "spider" );
            //    loadServerRow("شاهد",Shahid4uController.getInstance( getActivity()), "https://shahed4u.vip/netflix/" );
            //loadServerRow("شاهد",Shahid4uController.getInstance( getActivity()), "game of thrones" );


            //   HeaderItem faselHeader = new HeaderItem(ROWS_COUNTER++, "فاصل");
            //   rowsAdapter.add(new ListRow(faselHeader, faselAdapter));
            //kk  HeaderItem faselHeader = new HeaderItem(ROWS_COUNTER++, "فاصل");
            //kk rowsAdapter.add(new ListRow(faselHeader, faselAdapter));


            // ArrayObjectAdapter  akwamRowsAdapter = new ArrayObjectAdapter(new CardPresenter());
            //ControllableServer akwamS = ;


            //  HeaderItem header = new HeaderItem(0, "rowName");
            //increase row counter location
            //ROWS_COUNTER++;
            //add header name and position of the row
            // rowsAdapter.add(new ListRow(header, akwamRowsAdapter));
            // rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());


//
//    ExecutorService executor = Executors.newSingleThreadExecutor();
//    executor.submit(() -> {
//
//
        });

        executor.shutdown();


        //to set focus on the first row
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof String) {
                if (((String) item).contains(getString(R.string.error_fragment))) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (!(item instanceof Movie)) {
//                Toast.makeText(getActivity(), "handleItemClicked clicked item not is instanceof Movie ", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onItemClicked: handleItemClicked clicked item not is instanceof Movie ");
                return;
            }

            Movie movie = (Movie) item;

            clickedMovieAdapter = (ArrayObjectAdapter) ((ListRow) row).getAdapter();
            clickedMovieIndex = clickedMovieAdapter.indexOf(movie);

            // mainViewControl.handleMovieItemClick(movie, clickedMovieIndex, rowsAdapter, (ListRow) row, defaultHeadersCounter); // Removed

            Log.d(TAG, "onItemClicked: " + movie.getTitle() + ", state: " + movie.getState() + ", studio: " + movie.getStudio());

            if (movie.getState() == Movie.VIDEO_STATE ||
                (movie.getStudio().equals(Movie.SERVER_IPTV) && movie.getState() != Movie.PLAYLIST_STATE && movie.getVideoUrl() != null && !movie.getVideoUrl().isEmpty())) {
                Log.d(TAG, "Direct play condition met for: " + movie.getTitle());
                Util.openExoPlayer(movie, activity, movie.getStudio().equals(Movie.SERVER_IPTV));
                viewModel.markAsWatched(movie);
            } else if (movie.getState() == Movie.COOKIE_STATE ||
                       movie.getState() == Movie.BROWSER_STATE ||
                       movie.getState() == Movie.GROUP_OF_GROUP_STATE ||
                       movie.getState() == Movie.GROUP_STATE ||
                       movie.getState() == Movie.ITEM_STATE ||
                       movie.getState() == Movie.RESOLUTION_STATE) {
                 Log.d(TAG, "Complex action / data fetch needed for: " + movie.getTitle());
                viewModel.handleMovieClickAction(movie);
            } else if (movie.getStudio().equals(Movie.SERVER_IPTV) && movie.getState() == Movie.PLAYLIST_STATE) {
                Log.d(TAG, "IPTV Playlist item clicked: " + movie.getTitle());
                viewModel.loadIptvPlaylist(movie);
            }
            else {
                Log.d(TAG, "Defaulting to details intent for: " + movie.getTitle());
                Util.openVideoDetailsIntent(movie, activity); // For TV, DetailsActivity is usually VideoDetailsActivity
            }
//            handleItemClicked(itemViewHolder, item, row);
//            if (item instanceof Movie) {
//                Movie movie = (Movie) item;
//                Log.d(TAG, "Item: " + item.toString());
//                Intent intent = new Intent(getActivity(), DetailsActivity.class);
//                intent.putExtra(DetailsActivity.MOVIE, (Parcelable) movie);
//
//                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                                getActivity(),
//                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                                DetailsActivity.SHARED_ELEMENT_NAME)
//                        .toBundle();
//                getActivity().startActivity(intent, bundle);
//            } else if (item instanceof String) {
//                if (((String) item).contains(getString(R.string.error_fragment))) {
//                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
//                    startActivity(intent);
//                } else {
//                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
//                }
//            }
        }
    }

//    private void handleItemClicked(Presenter.ViewHolder itemViewHolder, Object item, Row row) {
//        if (item instanceof String) {
//            if (((String) item).contains(getString(R.string.error_fragment))) {
//                Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
//                startActivity(intent);
//            } else {
//                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
//            }
//            return;
//        }
//        if (!(item instanceof Movie)) {
//            Toast.makeText(getActivity(), "handleItemClicked clicked item not is instanceof Movie ", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        Movie movie = (Movie) item;
//
////        AbstractServer server = ServerConfigManager.getServer(movie.getStudio());
////        if (server == null){
////            Log.d(TAG, "handleItemClicked: server is not found: "+movie.getStudio());
////            return;
////        }
////        int nextAction = server.fetchNextAction(movie);
//
//
//        //''''''''''
//
//        Log.d(TAG, "onItemClicked: " + item.toString());
//        if (movie.getStudio().equals(Movie.SERVER_IPTV)) {
//            handleIptvClickedItem(movie);
//            //exist method after handling
//            return;
//        }
//
//        if (movie.getState() == Movie.NEXT_PAGE_STATE) {
//            //todo: add info to say if next already clicked, and handle the rest
//            handleNextPageMovieClick((ListRow) row, movie);
//            return;
//        }
//
//        Intent intent = Util.generateIntent(movie, new Intent(activity, DetailsActivity.class), true);
//
//        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        getActivity(),
//                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                        DetailsActivity.SHARED_ELEMENT_NAME)
//                .toBundle();
//        getActivity().startActivity(intent, bundle);
//    }

//    private void handleNextPageMovieClick(ListRow row, Movie movie) {
//        if (movie.getDescription().equals("0")) {
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            executor.submit(() -> {
////                            AbstractServer server = ServerManager.determineServer(movie, null, getActivity(), fragment);
//                AbstractServer server = ServerConfigManager.getServer(movie.getStudio());
//                if (server == null) {
//                    Log.d(TAG, "handleItemClicked NEXT_PAGE_STATE run: unknown server:" + movie.getStudio());
//                    return;
//                }
//                //server
//                ArrayObjectAdapter adapter = (ArrayObjectAdapter) row.getAdapter();
////                            Log.d(TAG, "onItemClicked: adapter :" + adapter.toString());
//
//                List<Movie> nextList = server.search(movie.getVideoUrl(), new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
//                    @Override
//                    public void onSuccess(ArrayList<Movie> result, String title) {
//                        Log.d(TAG, "handleItemClicked NEXT_PAGE_STATE onSuccess");
//                        if (result.isEmpty()) {
//                            return;
//                        }
//                        mHandler.post(() -> {
//                            adapter.addAll(adapter.size(), result);
//                            //flag that its already clicked
//                            movie.setDescription("1");
//                        });
//                    }
//
//                    @Override
//                    public void onInvalidCookie(ArrayList<Movie> result, String title) {
//                        Log.d(TAG, "handleItemClicked NEXT_PAGE_STATE onInvalidCookie");
//                        if (result.isEmpty()) {
//                            return;
//                        }
//                        mHandler.post(() -> {
//                            adapter.addAll(adapter.size(), result);
//                            //flag that its already clicked
//                            movie.setDescription("1");
//                        });
//                    }
//
//                    @Override
//                    public void onInvalidLink(ArrayList<Movie> result) {
//
//                    }
//
//                    @Override
//                    public void onInvalidLink(String message) {
//
//                    }
//                });
////                            Log.d(TAG, "handleItemClicked: nextPage:" + nextList.toString());
//
//            });
//
//            executor.shutdown();
//
//        }
//    }

//    private void handleIptvClickedItem(Movie movie) {
//        if (movie.getState() != Movie.PLAYLIST_STATE) {
//            Log.d(TAG, "handleIptvClickedItem: not PLAYLIST_STATE but: " + movie.getState());
//            Util.openExoPlayer(movie, getActivity(), true);
//            return;
//        }
//
//        int rowSize = rowsAdapter.size() - 1;
//        int defaultHeaders = rowSize - IPTV_HEADER_ROWS_COUNTER;
//        Log.d(TAG, "handleItemClicked: defaultHeaders:" + defaultHeaders);
//
////                        Log.d(TAG, "handleItemClicked: iptvStartIndex: "+iptvStartIndex);
//        if (rowSize >= IPTV_HEADER_ROWS_COUNTER) {
////            if (iptvList.isEmpty()) {
////                Log.d(TAG, "handleItemClicked: SERVER_IPTV PLAYLIST_STATE iptvList.isEmpty ");
////                return;
////            }
//            while (rowSize > defaultHeaders) {
////                                Log.d(TAG, "onItemClicked: remove row:" + iptvLastIndex);
//                try {
//                    rowsAdapter.remove(
//                            rowsAdapter.get((rowSize--)
//                            ));
//                } catch (Exception exception) {
//                    Log.d(TAG, "handleItemClicked: error deleting iptv header on main fragment: " + exception.getMessage());
//                }
//
//            }
//            iptvList.clear();
//            IPTV_HEADER_ROWS_COUNTER = 0;
//        }
//
//        try {
////           showProgressDialog();
////        todo
//            IptvServer iptvServer = (IptvServer) ServerConfigManager.getServer(Movie.SERVER_IPTV);
//            if (iptvServer == null) {
//                return;
//            }
//            CompletableFuture<Map<String, List<Movie>>> futureGroupedMovies = iptvServer.fetchAndGroupM3U8ContentAsync(movie, dbHelper);
//            Toast.makeText(getActivity(), "الرجاء الانتظار...", Toast.LENGTH_LONG).show();
//
//            futureGroupedMovies.thenAcceptAsync(groupedMovies -> {
//                for (Map.Entry<String, List<Movie>> entry : groupedMovies.entrySet()) {
//                    String group = entry.getKey();
//                    List<Movie> groupMovies = entry.getValue();
//                    // Creating a movie magic show with your UI update!
////                            getActivity().runOnUiThread(() -> {
//                    mHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
//                            iptvList.addAll(groupMovies);
//                            listRowAdapter.addAll(0, groupMovies);
//                            HeaderItem header = new HeaderItem(HEADER_ROWS_COUNTER++, group);
//                            IPTV_HEADER_ROWS_COUNTER++;
//                            rowsAdapter.add(new ListRow(header, listRowAdapter));
//                        }
//                    });
//                }
//            }).exceptionally(e -> {
//                // Handle any exceptions with grace (and maybe a touch of humor!)
//                Log.e(TAG, "Something went wrong: " + e.getMessage());
//                return null;
//            });
//
//
//// This line waits for the completion of the future
////          hideProgressDialog();
//        } catch (Exception e) {
//            Log.d(TAG, "handleIptvClickedItem: " + e.getMessage());
//        }
//    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            if (item instanceof Movie) {
                mBackgroundUri = ((Movie) item).getBackgroundImageUrl();
                startBackgroundTimer();
            }
        }
    }

    private void prepareBackgroundManager() {

        Activity currentActivity = getActivity();
        Context context = getContext();
        if (currentActivity == null || context == null) {
            return;
        }
        mBackgroundManager = BackgroundManager.getInstance(currentActivity);
        mBackgroundManager.attach(currentActivity.getWindow());

        mDefaultBackground = ContextCompat.getDrawable(context, R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        currentActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
//        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // The original behavior was to launch GetSearchQueryActivity.
                // To integrate ViewModel, the query should be obtained and then passed to ViewModel.
                // This might require onActivityResult handling if GetSearchQueryActivity returns the query.
                // For a direct search (if Leanback provided an input field):
                // String query = ... obtain query ...
                // String selectedServerId = viewModel.getSelectedServerIdLiveData().getValue(); // Or some other selection logic
                // if (selectedServerId != null && query != null && !query.isEmpty()) {
                //     viewModel.searchMovies(selectedServerId, query).observe(getViewLifecycleOwner(), resource -> {
                //         // Handle search results: update a specific search row or display in a new fragment/activity
                //         if (resource.status == Resource.Status.SUCCESS) {
                //             Log.d(TAG, "Search results: " + resource.data);
                //             // Example: display search results in a new row
                //             HeaderItem searchHeader = new HeaderItem("Search Results for \"" + query + "\"");
                //             ArrayObjectAdapter searchAdapter = new ArrayObjectAdapter(new CardPresenter());
                //             if (resource.data != null) {
                //                 searchAdapter.addAll(0, resource.data);
                //             }
                //             // Need to manage adding/updating this search row in rowsAdapter
                //             // This might involve removing previous search result row if any.
                //         }
                //     });
                // }
                // For now, keep original behavior of launching activity.
                // Actual search triggering using ViewModel would happen after query is received.
                Intent getQueryIntent = new Intent(getActivity(), GetSearchQueryActivity.class);
                startActivity(getQueryIntent); // The result of this should be handled in onActivityResult
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        // Check if the background manager is initialized
        if (getActivity() == null || mBackgroundManager == null) {
            return;
        }

        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(@NonNull Drawable drawable, @Nullable Transition<? super Drawable> transition) {
                        if (mBackgroundManager != null && mBackgroundManager.isAttached()) {
                            mHandler.post(() -> {
                                mBackgroundManager.setDrawable(drawable);
                            });
                        }
                    }
                });

        if (mBackgroundTimer != null) {
            mBackgroundTimer.cancel();
        }

    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBackgroundTimer != null) {
            mBackgroundTimer.cancel();
        }
        mHandler.removeCallbacksAndMessages(null); // Clear handler
    }


    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateBackground(mBackgroundUri);
                }
            });
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(
                    ContextCompat.getColor(getContext(), R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }
}