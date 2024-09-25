package com.omerflex.view.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.Util;
import com.omerflex.view.DetailsActivity;
import com.omerflex.view.mobile.view.CategoryAdapter;
import com.omerflex.view.mobile.view.HorizontalMovieAdapter;
import com.omerflex.view.mobile.view.OnMovieClickListener;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MobileMovieDetailActivity extends AppCompatActivity {
    public static final String TAG = "MobileDetailActivity";
    public static final String EXTRA_MOVIE_TITLE = "extra_movie_title";
    public static final String EXTRA_MOVIE_IMAGE_URL = "extra_movie_image_url";
    public static final String EXTRA_MOVIE_CATEGORY = "extra_movie_category";
    public static final String EXTRA_MOVIE_RATING = "extra_movie_rating";
    public static final String EXTRA_MOVIE_STUDIO = "extra_movie_studio";

    private TextView titleTextView;
    private ImageView imageView;
    private RecyclerView relatedMoviesRecyclerView;
    private HorizontalMovieAdapter relatedMoviesAdapter;
    private List<Movie> relatedMovies;
    private TextView ratingTextView;
    private TextView studioTextView;
    private Movie mSelectedMovie;
    private Handler handler = new Handler();
    Activity activity;
    AbstractServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mobile_movie_detail);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        activity = this;
        mSelectedMovie = Util.recieveSelectedMovie(activity);
        server = Util.determineServer(mSelectedMovie, null, activity, null);

        initializeView(mSelectedMovie);

        // Set up the related movies RecyclerView
        fetchRelatedMovies(mSelectedMovie);
    }

    private void initializeView(Movie mSelectedMovie) {
        titleTextView = findViewById(R.id.titleTextView);
        imageView = findViewById(R.id.imageView);
        relatedMoviesRecyclerView = findViewById(R.id.relatedMoviesRecyclerView);

        // Set the movie details
        titleTextView.setText(mSelectedMovie.getTitle());
        Glide.with(this).load(mSelectedMovie.getCardImageUrl()).into(imageView);

        ratingTextView = findViewById(R.id.ratingTextView);
        studioTextView = findViewById(R.id.studioTextView);

        // Set the rating and studio name
        ratingTextView.setText(mSelectedMovie.getRate());
        studioTextView.setText(mSelectedMovie.getStudio());

        relatedMoviesAdapter = new HorizontalMovieAdapter(this, new ArrayList<>(), new RelatedMovieItemClickListener(this));
        relatedMoviesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        relatedMoviesRecyclerView.setAdapter(relatedMoviesAdapter);
    }

    private void fetchRelatedMovies(Movie mSelectedMovie) {

        if (mSelectedMovie.getFetch() == Movie.NO_FETCH_MOVIE_AT_START && mSelectedMovie.getSubList() != null) {
            updateRelatedMovieAdapter(mSelectedMovie);
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            // In a real application, this could be a database query or API call to get related movies.
            // For simplicity, we're using dummy data here.
            List<Movie> actionMovies = new ArrayList<>();
            Movie m1 = new Movie();
            m1.setTitle("Movie 1");
            m1.setCardImageUrl("https://via.placeholder.com/120x160");

            Movie m2 = new Movie();
            m2.setTitle("Movie 2");
            m2.setCardImageUrl("https://via.placeholder.com/120x160");
            actionMovies.add(m1);
            actionMovies.add(m2);

            Movie movie = server.fetch(mSelectedMovie);

            if (movie == null || movie.getSubList() == null) {
                return;
            }
            updateRelatedMovieAdapter(movie);
        });

        executor.shutdown();
    }

    private Movie updateMovieOnActivityResult(Movie resultMovie, List<Movie> resultMovieSublist) {
        if (resultMovie == null) {
            resultMovie = mSelectedMovie;
        }
        if (resultMovieSublist == null) {
            resultMovieSublist = mSelectedMovie.getSubList();
        }
        Log.d(TAG, "onActivityResult: REQUEST_CODE_MOVIE_UPDATE: " + resultMovie);


//                    ArrayList<Movie> movieSublist = (ArrayList<Movie>) data.getSerializableExtra(DetailsActivity.MOVIE_SUBLIST);


        Log.d(TAG, "onActivityResult: subList:" + resultMovieSublist);
        if (resultMovieSublist != null && !resultMovieSublist.isEmpty()) {
            String desc = resultMovieSublist.get(0).getDescription();
            Log.d(TAG, "onActivityResult: desc: " + desc);
            resultMovie.setDescription(desc);
            resultMovie.setDescription(desc);
            resultMovie.setSubList(resultMovieSublist);

        }
        // update subMovies state
        if (resultMovie.getState() == Movie.RESULT_STATE && server != null && resultMovieSublist != null) {
            for (Movie mov : resultMovieSublist) {
                if (server.isSeries(mov)) {
                    resultMovieSublist.get(resultMovieSublist.indexOf(mov)).setState(Movie.GROUP_OF_GROUP_STATE);
                } else {
                    resultMovieSublist.get(resultMovieSublist.indexOf(mov)).setState(Movie.ITEM_STATE);
                }
            }
        }
        return resultMovie;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("TAG", "onActivityResult: ");

        // cases:
        // 1.Movie.REQUEST_CODE_MOVIE_UPDATE
        // 2.Movie.REQUEST_CODE_EXOPLAYER
        // 3.Movie.REQUEST_CODE_FETCH_HTML
        // 4.Movie.REQUEST_CODE_EXTERNAL_PLAYER
        // 5.Movie.REQUEST_CODE_MOVIE_LIST
        // should update:
        // 1.mSelectedMovie
        // 2.relatedMovieAdapter

        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.d(TAG, "onActivityResult:RESULT_NOT_OK ");
            return;
        }
        Gson gson = new Gson();

        Movie resultMovie = (Movie) data.getSerializableExtra(DetailsActivity.MOVIE);
        Type type = new TypeToken<List<Movie>>() {
        }.getType();
        String movieSublistString = data.getStringExtra(DetailsActivity.MOVIE_SUBLIST);

        List<Movie> resultMovieSublist = gson.fromJson(movieSublistString, type);

        String result = data.getStringExtra("result");


        Log.d(TAG, "onActivityResult:RESULT_OK ");

        //requestCode Movie.REQUEST_CODE_MOVIE_UPDATE is one movie object or 2 for a list of movies
        //this result is only to update the clicked movie of the sublist only and in some cases to update the description of mSelectedMovie
        //the id property of Movie object is used to identify the index of the clicked sublist movie
        switch (requestCode) {
            case Movie.REQUEST_CODE_MOVIE_UPDATE:
                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_MOVIE_UPDATE");
            case Movie.REQUEST_CODE_MOVIE_LIST:
                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_MOVIE_LIST");
                // returns null or movie
                mSelectedMovie = updateMovieOnActivityResult(resultMovie, resultMovieSublist);
                break;
            case Movie.REQUEST_CODE_FETCH_HTML:
                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_FETCH_HTML");
                mSelectedMovie = (Movie) server.handleOnActivityResultHtml(result, mSelectedMovie);
                break;
            case Movie.REQUEST_CODE_EXOPLAYER:
                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_EXOPLAYER");
                resultMovie.setSubList(resultMovieSublist);
                Util.openExoPlayer(resultMovie, activity);
                // todo: handle dbHelper
                // dbHelper.addMainMovieToHistory(mSelectedMovie);
                mSelectedMovie = resultMovie;
                break;
            case Movie.REQUEST_CODE_EXTERNAL_PLAYER:
                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_EXTERNAL_PLAYER");
                Util.openExternalVideoPlayer(resultMovie, activity);
                // todo: handle dbHelper
                // dbHelper.addMainMovieToHistory(mSelectedMovie);
                mSelectedMovie = resultMovie;
                break;
            default:
                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE unknown: "+ requestCode);
                mSelectedMovie = resultMovie;
        }

        updateRelatedMovieAdapter(mSelectedMovie);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateRelatedMovieAdapter(Movie mSelectedMovie) {
        Log.d("TAG", "updateRelatedMovieAdapter: "+ mSelectedMovie);
        handler.post(new Runnable() {
            @Override
            public void run() {
                relatedMoviesAdapter.getMovieList().addAll(mSelectedMovie.getSubList());
                relatedMoviesAdapter.notifyDataSetChanged();
            }
        });
    }

    public static class RelatedMovieItemClickListener implements OnMovieClickListener {
        Activity activity;
        public String TAG = "RelatedMovieItemClickListener";

        CategoryAdapter categoryAdapter;

        public RelatedMovieItemClickListener(Activity activity) {
            this.activity = activity;
        }

        public void onMovieClick(Movie movie, int position, HorizontalMovieAdapter horizontalMovieAdapter) {
// Check if the clicked movie matches the criteria to extend the category
//            Log.d(TAG, "onMovieClick: "+ categoryAdapter.getItemCount());

            // 1. case: open external movie player
            // 2. case open web browser
            // 3. case open details activity
            boolean externalPlayerCond = movie.getState() == Movie.VIDEO_STATE || movie.getState() == Movie.RESOLUTION_STATE;
            if (externalPlayerCond) {
                Util.openExternalVideoPlayer(movie, activity);
            } else if (movie.getState() == Movie.BROWSER_STATE || movie.getState() == Movie.COOKIE_STATE) {
                Util.openBrowserIntent(movie, activity);
            } else {
                Util.openDetailsIntent(movie, activity);
            }
        }
    }


}


