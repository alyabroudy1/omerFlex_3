package com.omerflex.view.mobile;

import android.app.Activity;
import dagger.hilt.android.AndroidEntryPoint; // Added for Hilt
import javax.inject.Inject; // Added for Hilt
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // Added for ViewModel
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig; // Added for ViewModel
import com.omerflex.server.Util;
import com.omerflex.service.ServerManager;
// import com.omerflex.service.database.MovieDbHelper; // Removed for Hilt
import com.omerflex.service.database.DatabaseManager; // Added for Hilt
import com.omerflex.utils.Resource; // Added for ViewModel
// import com.omerflex.view.MainViewControl; // Removing MainViewControl
import com.omerflex.view.mobile.entity.Category;
import com.omerflex.view.mobile.view.CategoryAdapter;
import android.widget.Toast; // Added for displaying messages
import com.omerflex.view.mobile.view.HorizontalMovieAdapter;
import com.omerflex.view.mobile.view.OnMovieClickListener;

import java.util.ArrayList;
import java.util.HashMap; // Added for serverAdapterMap
import java.util.Map; // Added for serverAdapterMap

@AndroidEntryPoint // Added for Hilt
public class MobileHomepageActivity extends AppCompatActivity {

    @Inject public DatabaseManager databaseManager; // Added for Hilt
    // @Inject public ServerManager serverManager; // For later if ServerManager is also Hilt managed

    private MobileHomepageViewModel viewModel; // Added for ViewModel
    private Map<String, HorizontalMovieAdapter> serverAdapterMap = new HashMap<>(); // Added for mapping serverId to its adapter

    private SearchView searchView;
    public static String TAG = "MobileHomepageActivity";
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
//    private List<HorizontalMovieAdapter> categoryList;
    ServerManager serverManager;
    Activity activity;
    HorizontalMovieAdapter clickedHorizontalMovieAdapter;
    int clickedMovieIndex = 0;

    int defaultHeadersCounter = 0;
    int totalHeadersCounter = 0;
    Category clickedMovieAdapter;

    private Handler handler = new Handler();
    // public MovieDbHelper dbHelper; // Removed for Hilt
    // private MainViewControl mainViewControl; // Removing MainViewControl field


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mobile_homepage);

        viewModel = new ViewModelProvider(this).get(MobileHomepageViewModel.class); // Added for ViewModel

        activity = this;

        // TODO: Initialize serverManager with injected databaseManager if needed, or inject serverManager directly
        // For now, to ensure MainViewControl compiles, we might need to pass databaseManager
        // or a null dbHelper if MainViewControl's constructor is updated.
        // This part depends on how ServerManager and MainViewControl are refactored for Hilt.
        // For this specific subtask, we focus on DatabaseManager injection.
        // Let's assume ServerManager is initialized later or also injected.
        // serverManager = new ServerManager(activity, null, null); // Placeholder, will be handled by Hilt or further refactor

        // todo: serverManager.updateServers();
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);

//        // Set up the vertical RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(this, new MovieItemClickListener(this));


        // MainViewControl constructor needs to be addressed.
        // If it strictly needs MovieDbHelper, it needs refactoring.
        // If it can take DatabaseManager or DAOs, that's better.
        // For now, to make it compile within this step, I'll pass null for dbHelper,
        // assuming it will be refactored or can handle null dbHelper temporarily.
        // A proper fix would involve refactoring MainViewControl to accept DatabaseManager or DAOs.
        // MainViewControl might need to be refactored to take ViewModel or specific LiveData
        // MainViewControl instantiation and its anonymous class are removed.
        // mainViewControl = new MainViewControl(activity, null, null /*dbHelper replaced with null*/) { ... };

        recyclerView.setAdapter(categoryAdapter);

        // Handle the submit button click
        searchView.setOnQueryTextListener(getSearchViewListener());

        // Observe LiveData from ViewModel
        observeViewModel();

        // Load categories in the background
//        loadCategoriesInBackground();
//        String url = "https://airmax.boats/airmaxtv963/airmaxtv369/309.ts|User-Agent=airmaxtv&Accept-Encoding=identity&Host=airmax.boats&Connection=Keep-Alive&Icy-MetaData=1";
//        Movie mov = new Movie();
//        mov.setVideoUrl(url);
//        Util.openExoPlayer(mov, activity, false);

        // todo: handle expired activity or device changed orientation
    }

    private void generateCategoryViewAsync(String title, ArrayList<Movie> movies, boolean isDefaultHeader) {
//        Category category = new Category(title, movies);
        Log.d(TAG, "generateCategoryViewAsync: " + movies);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                categoryAdapter.addCategory(title, movies);
//        adapter.notifyItemInserted(categoryAdapter.size() - 1);
                categoryAdapter.notifyItemInserted(categoryAdapter.size() - 1);
                if (isDefaultHeader) {
                    defaultHeadersCounter++;
                }
            }
        });
    }

    private HorizontalMovieAdapter generateCategoryView(String title, ArrayList<Movie> movies, boolean isDefaultHeader) {
//        Category category = new Category(title, movies);
        if (title == null){
            title = "default";
        }
        Log.d(TAG, "generateCategoryView: " + title+", movies: "+movies);
        HorizontalMovieAdapter adapter = null;
        try {
            adapter = categoryAdapter.addCategory(title, movies);
        }catch (Exception e){
            Log.d(TAG, "run: error notifying adapter: "+e.getMessage());
            return null;
        }

        if (isDefaultHeader) {
            defaultHeadersCounter++;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    categoryAdapter.notifyItemInserted(categoryAdapter.getItemCount());
                }catch (Exception e){
                    Log.d(TAG, "run: error notifying adapter: "+e.getMessage());
                }
            }
        });
//        adapter.notifyItemInserted(categoryAdapter.size() - 1);
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                categoryAdapter.notifyItemInserted(categoryAdapter.size() - 1);
//                if (isDefaultHeader) {
//                    defaultHeadersCounter++;
//                }
//            }
//        });
        return adapter;
    }

    @NonNull
    private SearchView.OnQueryTextListener getSearchViewListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // TODO: Need a way to get current/selected serverId for search
                // For now, using a placeholder or the first server if available.
                // This part of UI logic (selecting a server for search context) needs to be designed.
                List<ServerConfig> configs = viewModel.getServerConfigs().getValue();
                String selectedServerId = (configs != null && !configs.isEmpty()) ? configs.get(0).getName() : null;

                if (selectedServerId != null && query != null && !query.isEmpty()) {
                    Log.d(TAG, "Searching for: " + query + " on server: " + selectedServerId);
                    viewModel.searchMovies(selectedServerId, query).observe(MobileHomepageActivity.this, resource -> {
                        if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                            Log.d(TAG, "Search results: " + resource.data.size());
                            // TODO: Update UI with search results. This might involve:
                            // - Clearing existing categories in categoryAdapter
                            // - Adding a new category "Search Results" with resource.data
                            // - Or navigating to a dedicated search results activity/fragment
                            // For now, just logging. A simple approach:
                            categoryAdapter.clearCategories(); // Clear existing
                            categoryAdapter.addCategory("Search Results for \"" + query + "\"", (ArrayList<Movie>) resource.data);

                        } else if (resource.status == Resource.Status.ERROR) {
                            Log.e(TAG, "Search error: " + resource.message);
                            // TODO: Show error to user
                        } else if (resource.status == Resource.Status.LOADING) {
                            Log.d(TAG, "Search loading...");
                            // TODO: Show loading indicator
                        }
                    });
                } else {
                    Log.d(TAG, "Server ID not selected or query is empty for search.");
                    if (query != null && !query.isEmpty()){
                         Util.openSearchResultActivity(query, activity); // Fallback to old search if no server selected
                    }
                }
                return true; // Indicate the query has been handled
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        };
    }

    private void observeViewModel() {
        viewModel.getServerConfigs().observe(this, serverConfigs -> {
            if (serverConfigs != null && !serverConfigs.isEmpty()) {
                Log.d(TAG, "Server configs loaded: " + serverConfigs.size());
                categoryAdapter.clearCategories(); // Clear existing categories
                defaultHeadersCounter = 0; // Reset counter

                for (ServerConfig config : serverConfigs) {
                    // Add a category for each server. Movies will be loaded when the category is displayed
                    // or explicitly triggered. For now, just adding the category title.
                    // The actual movie loading for each category needs to be handled.
                    // One approach: when a category (server) becomes visible or is clicked,
                    // call viewModel.loadMoviesForServer(config.getName())
                    // and then observe viewModel.getMoviesForCategory().
                    // This is a simplified representation for now.
                    Log.d(TAG, "Adding category for server: " + config.getLabel() + " with ID: " + config.getName());
                    // The generateCategoryView method adds to adapter and notifies.
                    // We pass an empty list of movies initially, they'll load on demand.
                    HorizontalMovieAdapter adapter = generateCategoryView(config.getLabel(), new ArrayList<>(), true);
                    if (adapter != null) {
                        serverAdapterMap.put(config.getName(), adapter);
                    }
                }

                // Auto-load movies for the first server if list is not empty
                if (!serverConfigs.isEmpty()) {
                    viewModel.setSelectedServerId(serverConfigs.get(0).getName());
                }
            } else {
                Log.d(TAG, "No server configs found or list is empty.");
                categoryAdapter.clearCategories();
                serverAdapterMap.clear();
            }
        });

        viewModel.moviesForSelectedServer.observe(this, resource -> {
            String currentServerId = viewModel.getSelectedServerId().getValue();
            if (currentServerId == null) {
                Log.w(TAG, "moviesForSelectedServer updated, but currentServerId is null. Ignoring.");
                return;
            }

            HorizontalMovieAdapter adapter = serverAdapterMap.get(currentServerId);
            if (adapter == null) {
                Log.e(TAG, "No adapter found for serverId: " + currentServerId);
                return;
            }

            if (resource != null) {
                switch (resource.status) {
                    case LOADING:
                        Log.d(TAG, "Loading movies for server: " + currentServerId);
                        adapter.showLoading(true);
                        break;
                    case SUCCESS:
                        adapter.showLoading(false);
                        if (resource.data != null) {
                            Log.d(TAG, "Successfully loaded " + resource.data.size() + " movies for server: " + currentServerId);
                            adapter.setMovies(resource.data);
                        } else {
                            Log.d(TAG, "No movies found for server: " + currentServerId);
                            adapter.setMovies(new ArrayList<>()); // Set empty list
                        }
                        break;
                    case ERROR:
                        adapter.showLoading(false);
                        Log.e(TAG, "Error loading movies for server: " + currentServerId + ". Message: " + resource.message);
                        adapter.showError(resource.message != null ? resource.message : "Unknown error");
                        // Optionally set empty list on error or keep old data
                        // adapter.setMovies(new ArrayList<>());
                        break;
                }
            }
        });

        viewModel.getMovieActionOutcome().observe(this, resource -> {
            if (resource == null) return;

            // TODO: Potentially show/hide a global loading indicator based on resource.status
            // For now, using Toasts for feedback.

            switch (resource.status) {
                case LOADING:
                    Log.d(TAG, "Movie action loading for: " + (resource.data != null ? resource.data.getTitle() : "Unknown"));
                    Toast.makeText(activity, "Processing action for " + (resource.data != null ? resource.data.getTitle() : "..."), Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    Movie resultMovie = resource.data;
                    if (resultMovie != null) {
                        Log.d(TAG, "Movie action success: " + resultMovie.getTitle() + " with state: " + resultMovie.getState());
                        if (resultMovie.getState() == Movie.COOKIE_STATE) {
                            Log.d(TAG, "Action outcome: COOKIE_STATE, opening browser.");
                            Util.openBrowserIntent(resultMovie, activity, true, true);
                        } else if (resultMovie.getState() == Movie.VIDEO_STATE && resultMovie.getVideoUrl() != null && !resultMovie.getVideoUrl().isEmpty()) {
                            Log.d(TAG, "Action outcome: VIDEO_STATE, playing video.");
                            Util.openExoPlayer(resultMovie, activity, resultMovie.getStudio().equals(Movie.SERVER_IPTV));
                            viewModel.markAsWatched(resultMovie);
                        } else if (resultMovie.getState() == Movie.BROWSER_STATE && resultMovie.getVideoUrl() != null && !resultMovie.getVideoUrl().isEmpty()) {
                            Log.d(TAG, "Action outcome: BROWSER_STATE, opening browser.");
                            Util.openBrowserIntent(resultMovie, activity, false, false); // Assuming non-force for generic browser state
                        }
                        // Add other specific state handling if needed, e.g., for RESOLUTION_STATE if it implies further action
                        else {
                             Log.d(TAG, "Action outcome: Fallback to details view for " + resultMovie.getTitle());
                             Util.openMobileDetailsIntent(resultMovie, activity, true); // Default action
                        }
                    } else {
                        Log.d(TAG, "Movie action success but no movie data returned.");
                    }
                    break;
                case ERROR:
                    Log.e(TAG, "Movie action error: " + resource.message);
                    Toast.makeText(activity, resource.message, Toast.LENGTH_LONG).show();
                    // If error includes movie data (e.g. for retrying cookie state), handle it
                    if (resource.data != null && resource.data.getState() == Movie.COOKIE_STATE) {
                        Util.openBrowserIntent(resource.data, activity, true, true);
                    }
                    break;
            }
        });
    }

//    private void loadCategoriesInBackground() {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.submit(() -> {
////            AbstractServer server = new MyCimaServer(activity, null);
////            ArrayList<Movie> movies = server.getHomepageMovies();
////
////            Category category = new Category(server.getLabel(), movies);
////            handler.post(new Runnable() {
////                @Override
////                public void run() {
////                    categoryList.add(category);
////                    categoryAdapter.notifyItemInserted(categoryList.size() - 1);
////                }
////            });
//
//
//            ArrayList<AbstractServer> serversList= ServerConfigManager.getServers(dbHelper);
////            ArrayList<AbstractServer> serversList= new ArrayList<>();
//
//            Log.d(TAG, "loadHomepageRaws a "+serversList);
//            for (AbstractServer server : serversList) {
//
//                Log.d(TAG, "loadHomepageRaws server: " + server.getServerId());
////                if (server == null || server.getConfig() == null|| !server.getConfig().isActive()){
////                    continue;
////                }
////                if (
////                        server instanceof OldAkwamServer ||
////                                server instanceof CimaClubServer ||
//////                                    server instanceof FaselHdController ||
////                                    server instanceof AkwamServer ||
////                                    server instanceof ArabSeedServer ||
////                                    server instanceof IptvServer ||
////                                    server instanceof MyCimaServer
////                ) {
////                    continue;
////                }
//                // Update the RecyclerView on the main thread
//                ArrayList<Movie> movies = server.getHomepageMovies(new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
//                    @Override
//                    public void onSuccess(ArrayList<Movie> result, String title) {
//                        generateCategory(title, result);
//                    }
//
//                    @Override
//                    public void onInvalidCookie(ArrayList<Movie> result, String title) {
//                        generateCategory(server.getLabel(), result);
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
//
////                if (movies == null || movies.isEmpty()) {
////                    continue;
////                }
//
//
//            }
//        });
//        executor.shutdown();
//    }

//    private void generateCategory(String title, ArrayList<Movie> movies) {
////        Category category = new Category(title, movies);
//        HorizontalMovieAdapter adapter = categoryAdapter.addCategory(title, movies);
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
////                categoryList.add(category);
//                categoryAdapter.notifyItemInserted(categoryList.size() - 1);
//            }
//        });
//        return adapter;
//    }

    public class MovieItemClickListener implements OnMovieClickListener {
        Activity activity;

        CategoryAdapter categoryAdapter;

        public MovieItemClickListener(Activity activity) {
            this.activity = activity;
        }

        public void onMovieClick(Movie movie, int position, HorizontalMovieAdapter horizontalMovieAdapter) {
            // Check if the clicked movie matches the criteria to extend the category
            //            Log.d(TAG, "onMovieClick: "+ categoryAdapter.getItemCount());
            clickedHorizontalMovieAdapter = horizontalMovieAdapter; // May still be useful for specific UI non-data updates
            clickedMovieIndex = position; // May still be useful

            Log.d(TAG, "onMovieClick: " + movie.getTitle() + ", state: " + movie.getState() + ", studio: " + movie.getStudio());

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
                       movie.getState() == Movie.RESOLUTION_STATE ||
                       (movie.getStudio().equals(Movie.SERVER_IPTV) && movie.getState() == Movie.PLAYLIST_STATE)) {
                Log.d(TAG, "Complex action / data fetch needed for: " + movie.getTitle());
                viewModel.handleMovieClickAction(movie);
            } else {
                Log.d(TAG, "Defaulting to details intent for: " + movie.getTitle());
                Util.openMobileDetailsIntent(movie, activity, true);
            }
        }

        private void handleIptvClickedItem(Movie movie, int position, HorizontalMovieAdapter horizontalMovieAdapter) {
            // This method's specific logic for IPTV playlist expansion vs direct play
            // is now integrated into the main onMovieClick method by checking PLAYLIST_STATE.
            // If PLAYLIST_STATE, it goes to viewModel.handleMovieClickAction.
            // If direct play IPTV, it's handled by the first condition in onMovieClick.
            // This method can be removed if not called from elsewhere.
            // For now, let's assume it's effectively replaced.
            Log.d(TAG, "handleIptvClickedItem called (likely now redundant): " + movie.getTitle());
            // Original logic:
            // if (movie.getState() == Movie.PLAYLIST_STATE) {
            //     // Todo: generateCategories for a specific iptv list -> Now viewModel.handleMovieClickAction
            // } else {
            //     Util.openExoPlayer(movie, activity, true);
            //     viewModel.markAsWatched(movie);
            // }
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            executor.submit(() -> {
////                AbstractServer server = ServerManager.determineServer(movie, null, activity, null);
////                AbstractServer server = ServerConfigManager.getServer(movie.getStudio());
////
////                if (movie.getState() == Movie.COOKIE_STATE){
////                    Log.d(TAG, "onMovieClick: COOKIE_STATE-0");
////                    MovieFetchProcess fetchProcess = server.fetch(
////                            movie,
////                            movie.getState(),
////                            new ServerInterface.ActivityCallback<Movie>() {
////                                @Override
////                                public void onSuccess(Movie result, String title) {
////                                    // todo: analyse case
////                                    Log.d(TAG, "onMovieClick: COOKIE_STATE onSuccess");
////                                    Util.openBrowserIntent(result, activity, false, true);
////                                }
////
////                                @Override
////                                public void onInvalidCookie(Movie result, String title) {
////                                    Log.d(TAG, "onMovieClick: COOKIE_STATE onInvalidCookie: ");
////                                    result.setFetch(Movie.REQUEST_CODE_MOVIE_UPDATE);
////                                    Util.openBrowserIntent(result, activity, true, true);
////                                }
////
////                                @Override
////                                public void onInvalidLink(Movie result) {
////                                    Log.d(TAG, "onMovieClick: COOKIE_STATE onInvalidLink: ");
////                                }
////
////                                @Override
////                                public void onInvalidLink(String message) {
////
////                                }
////                            }
////                    );
//////                    Log.d(TAG, "onMovieClick: COOKIE_STATE");
//////                    if (fetchProcess.stateCode == MovieFetchProcess.FETCH_PROCESS_COOKE_REQUIRE) {
//////                        Util.openBrowserIntent(movie, activity, false, true);
//////                        return;
//////                    }
////                }
////                else if (movie.getStudio().equals(Movie.SERVER_IPTV)){
////                    Log.d(TAG, "onMovieClick: Movie.SERVER_IPTV");
////                    handleIptvClickedItem(movie, position, horizontalMovieAdapter);
////                }else if (movie.getState() == Movie.VIDEO_STATE){
////                    Log.d(TAG, "onMovieClick: Movie.SERVER_IPTV");
////                    Util.openExoPlayer(movie, activity, false);
////                }
////                else {
////                    Log.d(TAG, "onMovieClick: openMobileDetailsIntent");
////                    // Handle normal click event (e.g., open detail activity)
////                    Util.openMobileDetailsIntent(movie, activity, true);
////                }
////hiier
////                if (shouldExtendCategory(movie)) {
////                    Log.d(TAG, "onMovieClick: shouldExtendCategory");
////                    extendMovieListForCategory(movie, position, categoryAdapter, horizontalMovieAdapter);
////                } else {
////                    Log.d(TAG, "onMovieClick: openMobileDetailsIntent");
////                    // Handle normal click event (e.g., open detail activity)
////                    Util.openMobileDetailsIntent(movie, activity, true);
////                }
//            });
//            executor.shutdown();
        }

        private void handleIptvClickedItem(Movie movie, int position, HorizontalMovieAdapter horizontalMovieAdapter) {
            if (movie.getState() == Movie.PLAYLIST_STATE) {
                // Todo: generateCategories for a specific iptv list
            } else {
                Util.openExoPlayer(movie, activity, true);
            }
        }

//        private void fetchCookie(Movie movie) {
//            AbstractServer server = ServerManager.determineServer(movie, null, activity, null);
//            if (server == null) {
//                return;
//            }
//            server.fetch(movie, movie.getState());
//        }

        public CategoryAdapter getCategoryAdapter() {
            return categoryAdapter;
        }

        public void setCategoryAdapter(CategoryAdapter categoryAdapter) {
            this.categoryAdapter = categoryAdapter;
        }

        private boolean shouldExtendCategory(Movie movie) {
            return movie.getState() == Movie.NEXT_PAGE_STATE;
//            return true;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        // mainViewControl.onActivityResult(requestCode, resultCode, data, clickedHorizontalMovieAdapter, clickedMovieIndex); // Removed

        // TODO: Re-implement onActivityResult logic based on ViewModel.
        // This is crucial for handling results from BrowserActivity (for cookies) or other activities.
        // Example:
        // if (requestCode == DetailsActivity.REQUEST_CODE_BROWSER_LOGIN && resultCode == RESULT_OK && data != null) {
        //     Movie returnedMovie = data.getParcelableExtra(DetailsActivity.MOVIE);
        //     boolean success = data.getBooleanExtra("success", false);
        //     if (returnedMovie != null && success) {
        //         // Potentially re-trigger the action that required the cookie, or refresh data.
        //         viewModel.handleMovieClickAction(returnedMovie); // Or a more specific method
        //     } else if (returnedMovie != null) {
        //          Toast.makeText(this, "Cookie fetch might have failed for " + returnedMovie.getTitle(), Toast.LENGTH_LONG).show();
        //     }
        // }
        // For other request codes like REQUEST_CODE_EXOPLAYER, update UI or history as needed,
        // though much of this might be handled by direct LiveData observation now.

        super.onActivityResult(requestCode, resultCode, data);


//        if (resultCode != Activity.RESULT_OK || data == null) {
//            Log.d(TAG, "onActivityResult:RESULT_NOT_OK ");
//            return;
//        }
//
//        if (requestCode == Movie.REQUEST_CODE_EXOPLAYER){
//            Movie resultMovie = (Movie) data.getParcelableExtra(DetailsActivity.MOVIE);
//            if (resultMovie == null){
//                return;
//            }
//            Util.openExoPlayer(resultMovie, activity, true);
//            // todo: handle dbHelper
//            updateRelatedMovieItem(clickedHorizontalMovieAdapter, clickedMovieIndex, resultMovie);
//            dbHelper.addMainMovieToHistory(clickedHorizontalMovieAdapter.getMovieList().get(clickedMovieIndex));
//            return;
//        }
//
//
//
//
//        ArrayList<Movie> resultMovieSublist = data.getParcelableArrayListExtra(DetailsActivity.MOVIE_SUBLIST);
//        if (resultMovieSublist != null && !resultMovieSublist.isEmpty()){
//            updateMovieListOfHorizontalMovieAdapter(resultMovieSublist);
//        }
////        Gson gson = new Gson();
////
////        Movie resultMovie = (Movie) data.getSerializableExtra(DetailsActivity.MOVIE);
////        Type type = new TypeToken<List<Movie>>() {
////        }.getType();
////        String movieSublistString = data.getStringExtra(DetailsActivity.MOVIE_SUBLIST);
////
////        List<Movie> resultMovieSublist = gson.fromJson(movieSublistString, type);
////
////        String result = data.getStringExtra("result");
////
////
////        Log.d(TAG, "onActivityResult:RESULT_OK ");
////
////        //requestCode Movie.REQUEST_CODE_MOVIE_UPDATE is one movie object or 2 for a list of movies
////        //this result is only to update the clicked movie of the sublist only and in some cases to update the description of mSelectedMovie
////        //the id property of Movie object is used to identify the index of the clicked sublist movie
//////        switch (requestCode) {
//////            case Movie.REQUEST_CODE_MOVIE_UPDATE:
//////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_MOVIE_UPDATE");
//////            case Movie.REQUEST_CODE_MOVIE_LIST:
//////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_MOVIE_LIST");
//////                // returns null or movie
//////                mSelectedMovie = updateMovieOnActivityResult(resultMovie, resultMovieSublist);
//////                break;
//////            case Movie.REQUEST_CODE_FETCH_HTML:
//////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_FETCH_HTML");
//////                mSelectedMovie = (Movie) server.handleOnActivityResultHtml(result, mSelectedMovie);
//////                break;
//////            case Movie.REQUEST_CODE_EXOPLAYER:
//////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_EXOPLAYER");
//////                resultMovie.setSubList(resultMovieSublist);
//////                Util.openExoPlayer(resultMovie, activity);
//////                // todo: handle dbHelper
//////                // dbHelper.addMainMovieToHistory(mSelectedMovie);
//////                mSelectedMovie = resultMovie;
//////                break;
//////            case Movie.REQUEST_CODE_EXTERNAL_PLAYER:
//////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_EXTERNAL_PLAYER");
//////                Util.openExternalVideoPlayer(resultMovie, activity);
//////                // todo: handle dbHelper
//////                // dbHelper.addMainMovieToHistory(mSelectedMovie);
//////                mSelectedMovie = resultMovie;
//////                break;
//////            default:
//////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE unknown: "+ requestCode);
//////                mSelectedMovie = resultMovie;
//////        }
////
//////        updateRelatedMovieAdapter(mSelectedMovie);
//        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateRelatedMovieItem(HorizontalMovieAdapter horizontalMovieAdapter, int clickedMovieIndex, Movie resultMovie) {
        horizontalMovieAdapter.getMovieList().set(clickedMovieIndex, resultMovie);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                horizontalMovieAdapter.notifyItemChanged(clickedMovieIndex);
            }
        });
    }

    private void updateMovieListOfHorizontalMovieAdapter(ArrayList<Movie> resultMovieSublist) {
        if (clickedHorizontalMovieAdapter != null) {
            extendMovieListOfHorizontalMovieAdapter(resultMovieSublist, clickedHorizontalMovieAdapter);
        }
    }

    private void extendMovieListOfHorizontalMovieAdapter(ArrayList<Movie> resultMovieSublist, HorizontalMovieAdapter horizontalMovieAdapter) {
        Log.d(TAG, "extendMovieListOfHorizontalMovieAdapter: p:" + ", s:" + horizontalMovieAdapter.getItemCount());
        horizontalMovieAdapter.getMovieList().addAll(resultMovieSublist);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
//                    categoryList.add(category);
//                    categoryAdapter.notifyItemInserted(categoryList.size() - 1);
//                    horizontalMovieAdapter.notifyItemInserted(categoryList.size() - 1);
                horizontalMovieAdapter.notifyDataSetChanged();
            }
        });
    }

    private void extendMovieListOfHorizontalMovieAdapter(ArrayList<Movie> resultMovieSublist, Category category) {
        Log.d(TAG, "Category: p:" + ", s:" + category.getMovieAdapter());
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
//                    categoryList.add(category);
//                    categoryAdapter.notifyItemInserted(categoryList.size() - 1);
//                    horizontalMovieAdapter.notifyItemInserted(categoryList.size() - 1);
//                if (categoryList.contains(category)) {
//                    int cateIndex = categoryList.indexOf(category);
//                    categoryList.get(cateIndex).getMovieList().addAll(resultMovieSublist);
//                    categoryAdapter.notifyItemInserted(categoryList.size() - 1);
//                }

            }
        });
    }
}