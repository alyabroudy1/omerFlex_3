package com.omerflex.view.mobile;

import android.app.Activity;
import androidx.lifecycle.ViewModelProvider; // Added for ViewModel
import dagger.hilt.android.AndroidEntryPoint; // Added for Hilt
import com.omerflex.utils.Resource; // Added for ViewModel observations
import android.widget.Toast; // Added for messages
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.server.AbstractServer; // May become unused if all logic moves to ViewModel
import com.omerflex.server.ServerInterface; // May become unused
import com.omerflex.server.Util;
import com.omerflex.service.ServerConfigManager; // May become unused
// import com.omerflex.service.database.MovieDbHelper; // Removed
import com.omerflex.view.DetailsActivity;
// import com.omerflex.view.VideoDetailsFragment; // May become unused
import com.omerflex.view.mobile.view.CategoryAdapter;
import com.omerflex.view.mobile.view.HorizontalMovieAdapter;
import com.omerflex.view.mobile.view.OnMovieClickListener;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors; // May become unused

@AndroidEntryPoint // Added for Hilt
public class MobileMovieDetailActivity extends AppCompatActivity {
    public static final String TAG = "MobileDetailActivity";
    // Keys for intent extras should ideally match ViewModel's SavedStateHandle keys
    // public static final String EXTRA_MOVIE_TITLE = "extra_movie_title";
    public static final String EXTRA_MOVIE_IMAGE_URL = "extra_movie_image_url";
    public static final String EXTRA_MOVIE_CATEGORY = "extra_movie_category";
    public static final String EXTRA_MOVIE_RATING = "extra_movie_rating";
    public static final String EXTRA_MOVIE_STUDIO = "extra_movie_studio";

    private TextView titleTextView;
    private TextView categoryTextView;
    private ImageView imageView;
    private RecyclerView relatedMoviesRecyclerView;
    private HorizontalMovieAdapter relatedMoviesAdapter;
    private TextView ratingTextView;
    private TextView studioTextView;
    private TextView descriptionTextView;
    private TextView historyTextView;
    private Button watchButtonView;
    // private Movie mSelectedMovie; // ViewModel will hold the movie state
    private Handler handler = new Handler(); // Keep for UI updates if needed, but prefer LiveData
    Activity activity;
    // static AbstractServer server; // ViewModel will handle server logic via Repository
    // public MovieDbHelper dbHelper; // Removed
    // private static int clickedMovieIndex = 0; // Manage in adapter or pass data directly

    private MobileMovieDetailActivityViewModel viewModel;
    private HorizontalMovieAdapter relatedMoviesAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mobile_movie_detail);
        activity = this;

        // Initialize ViewModel
        // The Movie object is passed to ViewModel's SavedStateHandle via Intent extra
        // (Key: MobileMovieDetailActivityViewModel.MOVIE_ARG_KEY)
        viewModel = new ViewModelProvider(this).get(MobileMovieDetailActivityViewModel.class);

        initializeUIElements();
        observeViewModel();
        setupUIListeners(); // Setup listeners after UI elements and ViewModel are ready
    }

    private void initializeUIElements() {
        titleTextView = findViewById(R.id.titleTextView);
        categoryTextView = findViewById(R.id.categoryTextView); // This might display e.g. "Episodes" or "Related"
        imageView = findViewById(R.id.imageView); // Should be detail_movie_image based on new layout draft
        relatedMoviesRecyclerView = findViewById(R.id.relatedMoviesRecyclerView);

        ratingTextView = findViewById(R.id.ratingTextView);
        studioTextView = findViewById(R.id.studioTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        historyTextView = findViewById(R.id.historyTextView); // To display episode/season from MovieHistory
        watchButtonView = findViewById(R.id.watch_button);

        // Setup RecyclerView for related movies/episodes
        relatedMoviesAdapter = new HorizontalMovieAdapter(this, new ArrayList<>(), new RelatedMovieItemClickListener(this));
        relatedMoviesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        relatedMoviesRecyclerView.setAdapter(relatedMoviesAdapter);
    }

    private void setupUIListeners() {
        watchButtonView.setOnClickListener(view -> {
            Log.d(TAG, "onClick: watch button");
            Resource<Movie> movieResource = viewModel.movieDetails.getValue();
            if (movieResource != null && movieResource.data != null) {
                // For the main watch button, usually play the primary content or first episode
                Movie movieToPlay = movieResource.data;
                if (movieToPlay.getSubList() != null && !movieToPlay.getSubList().isEmpty() &&
                    (movieToPlay.getState() == Movie.GROUP_STATE || movieToPlay.getState() == Movie.GROUP_OF_GROUP_STATE)) {
                    // If it's a series and has sub-items, play the first sub-item (episode)
                    // This logic might need refinement based on how "primary content" vs "episodes" are structured.
                    // For now, assume if sublist exists, it's what the main watch button targets.
                    // Or, the watch button should be disabled and users click on sublist items.
                    // Let's assume for now the watch button tries to play the main movie if it's playable,
                    // otherwise user clicks on sublist.
                     Log.d(TAG, "Watch button clicked for main movie: " + movieToPlay.getTitle());
                     handleMoviePlayback(movieToPlay);
                } else if (movieToPlay.getVideoUrl() != null && !movieToPlay.getVideoUrl().isEmpty()){
                     Log.d(TAG, "Watch button clicked for direct play: " + movieToPlay.getTitle());
                     handleMoviePlayback(movieToPlay);
                } else {
                    Toast.makeText(activity, "No playable content for main watch button.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Movie details not loaded yet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.movieDetails.observe(this, resource -> {
            if (resource == null) {
                Log.e(TAG, "Movie details resource is null");
                Toast.makeText(this, "Error: Movie details could not be loaded.", Toast.LENGTH_LONG).show();
                return;
            }

            Movie movie = resource.data;

            switch (resource.status) {
                case LOADING:
                    Log.d(TAG, "Loading movie details...");
                    Toast.makeText(this, "Loading details...", Toast.LENGTH_SHORT).show();
                    if (movie != null) populateUI(movie); // Show potentially stale/initial data
                    watchButtonView.setVisibility(View.GONE); // Hide watch button while loading
                    break;
                case SUCCESS:
                    if (movie != null) {
                        Log.d(TAG, "Successfully loaded movie details for: " + movie.getTitle());
                        populateUI(movie);
                        if (movie.getSubList() != null && !movie.getSubList().isEmpty()) {
                            relatedMoviesAdapter.setMovies(movie.getSubList());
                            categoryTextView.setText(movie.getSubList().get(0).getGroup() != null ? movie.getSubList().get(0).getGroup() : "Related Content");
                        } else {
                            relatedMoviesAdapter.setMovies(new ArrayList<>()); // Clear related movies
                            categoryTextView.setText("No related content");
                        }
                        // Determine watch button visibility based on loaded movie
                        boolean canPlayMainMovie = movie.getVideoUrl() != null && !movie.getVideoUrl().isEmpty() &&
                                                  (movie.getState() == Movie.ITEM_STATE || movie.getState() == Movie.VIDEO_STATE || movie.getState() == Movie.RESOLUTION_STATE);
                        boolean hasSublist = movie.getSubList() != null && !movie.getSubList().isEmpty();

                        // Show watch button if main movie is directly playable or if there's a sublist (implying episodes)
                        // This logic might need refinement. For now, show if main is playable.
                        watchButtonView.setVisibility(canPlayMainMovie ? View.VISIBLE : View.GONE);


                    } else {
                        Log.e(TAG, "Movie details success but data is null.");
                        Toast.makeText(this, "Failed to load movie details.", Toast.LENGTH_LONG).show();
                        watchButtonView.setVisibility(View.GONE);
                    }
                    break;
                case ERROR:
                    Log.e(TAG, "Error loading movie details: " + resource.message);
                    Toast.makeText(this, "Error: " + resource.message, Toast.LENGTH_LONG).show();
                    if (movie != null) populateUI(movie); // Show potentially stale/initial data
                    watchButtonView.setVisibility(View.GONE);
                    break;
            }
        });
    }

    private void populateUI(Movie movie) {
        if (movie == null) return;

        titleTextView.setText(movie.getTitle());
        descriptionTextView.setText(movie.getDescription());
        studioTextView.setText(movie.getStudio()); // Studio might be server name
        ratingTextView.setText(movie.getRate());

        String imageUrl = movie.getCardImageUrl() != null ? movie.getCardImageUrl() : movie.getBackgroundImageUrl();
        Glide.with(this)
             .load(imageUrl)
             .error(R.drawable.default_background) // Ensure this drawable exists
             .into(imageView);

        // History text (e.g. Last Watched Episode)
        // This would typically come from a MovieHistory object associated with the series.
        // For now, if mSelectedMovie has MovieHistory, display it.
        if (movie.getMovieHistory() != null) {
            String historyStr = "Last: S" + movie.getMovieHistory().getSeason() + " E" + movie.getMovieHistory().getEpisode();
            historyTextView.setText(historyStr);
            historyTextView.setVisibility(View.VISIBLE);
        } else {
            historyTextView.setVisibility(View.GONE);
        }

        // Category for sublist (e.g. "Episodes")
        // This is dynamic based on sublist content, set in SUCCESS block of observer.
        // Here we just ensure it's visible if there will be content.
         categoryTextView.setVisibility(movie.getSubList() != null && !movie.getSubList().isEmpty() ? View.VISIBLE : View.GONE);
    }


    // Removed fetchRelatedMovies, fetchCookie, evaluateWatchButton, updateCurrentMovieView
    // Their logic is now handled by ViewModel or within observers.

//    private Movie updateMovieOnActivityResult(Movie resultMovie, List<Movie> resultMovieSublist) {
//        if (resultMovie == null) {
//            resultMovie = mSelectedMovie;
//        }
//        if (resultMovieSublist == null) {
//            resultMovieSublist = mSelectedMovie.getSubList();
//        }
//        Log.d(TAG, "onActivityResult: REQUEST_CODE_MOVIE_UPDATE: " + resultMovie);
//
//
////                    ArrayList<Movie> movieSublist = (ArrayList<Movie>) data.getSerializableExtra(DetailsActivity.MOVIE_SUBLIST);
//
//
//        Log.d(TAG, "onActivityResult: subList:" + resultMovieSublist);
//        if (resultMovieSublist != null && !resultMovieSublist.isEmpty()) {
//            String desc = resultMovieSublist.get(0).getDescription();
//            Log.d(TAG, "onActivityResult: desc: " + desc);
//            resultMovie.setDescription(desc);
//            resultMovie.setSubList(resultMovieSublist);
//
//        }
//        // update subMovies state
//        if (resultMovie.getState() == Movie.RESULT_STATE && server != null && resultMovieSublist != null) {
//            for (Movie mov : resultMovieSublist) {
//                //todo optimize
////                if (server.isSeries(mov)) {
////                    resultMovieSublist.get(resultMovieSublist.indexOf(mov)).setState(Movie.GROUP_OF_GROUP_STATE);
////                } else {
////                    resultMovieSublist.get(resultMovieSublist.indexOf(mov)).setState(Movie.ITEM_STATE);
////                }
//                mov.setState(server.detectMovieState(mov));
//            }
//        }
//        updateCurrentMovieView(resultMovie);
//        return resultMovie;
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        // TODO: Implement onActivityResult logic, potentially delegating to ViewModel
        // For example, if BrowserActivity returns a result for a cookie:
        // if (requestCode == DetailsActivity.REQUEST_CODE_BROWSER_LOGIN && resultCode == Activity.RESULT_OK && data != null) {
        //     Movie returnedMovie = data.getParcelableExtra(MobileMovieDetailActivityViewModel.MOVIE_ARG_KEY);
        //     boolean success = data.getBooleanExtra("success", false); // Assuming BrowserActivity sets this
        //     if (returnedMovie != null && success) {
        //         viewModel.loadMovieDetails(returnedMovie); // Refresh or retry with new cookie state via repository
        //     } else {
        //         Toast.makeText(this, "Cookie operation may have failed.", Toast.LENGTH_SHORT).show();
        //     }
        // }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Removed updateRelatedMovieItem, updateRelatedMovieAdapter, updateCurrentMovieView.
    // UI updates are now driven by observing viewModel.movieDetails.

    public class RelatedMovieItemClickListener implements OnMovieClickListener {
        Activity activity;
        // MovieDbHelper dbHelper; // Removed

        public RelatedMovieItemClickListener(Activity activity /*, MovieDbHelper dbHelper removed */) {
            this.activity = activity;
            // this.dbHelper = dbHelper;
        }

        public void onMovieClick(Movie movie, int position, HorizontalMovieAdapter horizontalMovieAdapter) {
            Log.d(TAG, "Related movie/episode clicked: " + movie.getTitle());
            // For sub-items like episodes, we usually want to play them directly.
            // The `playMovieLink` method in ViewModel can handle if it needs further resolution.
            handleMoviePlayback(movie);
        }
    }

    private void handleMoviePlayback(Movie movieToPlay) {
        viewModel.playMovieLink(movieToPlay).observe(this, resource -> {
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                Movie playableMovie = resource.data;
                Log.d(TAG, "Attempting to play: " + playableMovie.getTitle() + " URL: " + playableMovie.getVideoUrl());
                if (playableMovie.getVideoUrl() != null && !playableMovie.getVideoUrl().isEmpty()) {
                    Util.openExoPlayer(playableMovie, this, playableMovie.getStudio().equals(Movie.SERVER_IPTV));
                    viewModel.markAsWatched(playableMovie);
                } else {
                    Toast.makeText(this, "No playable URL found for " + playableMovie.getTitle(), Toast.LENGTH_SHORT).show();
                }
            } else if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(this, "Error preparing link: " + resource.message, Toast.LENGTH_LONG).show();
            } else if (resource.status == Resource.Status.LOADING) {
                Toast.makeText(this, "Preparing link...", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Standard back button behavior for up navigation
        return true;
    }
}


