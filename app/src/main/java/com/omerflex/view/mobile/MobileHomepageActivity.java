package com.omerflex.view.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.AkwamServer;
import com.omerflex.server.ArabSeedServer;
import com.omerflex.server.CimaClubServer;
import com.omerflex.server.IptvServer;
import com.omerflex.server.MyCimaServer;
import com.omerflex.server.OldAkwamServer;
import com.omerflex.server.Util;
import com.omerflex.service.ServerManager;
import com.omerflex.view.DetailsActivity;
import com.omerflex.view.mobile.entity.Category;
import com.omerflex.view.mobile.view.CategoryAdapter;
import com.omerflex.view.mobile.view.HorizontalMovieAdapter;
import com.omerflex.view.mobile.view.OnMovieClickListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MobileHomepageActivity extends AppCompatActivity {

    private SearchView searchView;
    public static String TAG = "MobileHomepageActivity";
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;
    ServerManager serverManager;
    Activity activity;
    HorizontalMovieAdapter clickedHorizontalMovieAdapter;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mobile_homepage);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        activity = this;
        serverManager = new ServerManager(activity, null);
        serverManager.updateServers();
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);

//        // Set up the vertical RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(this, categoryList, new MovieItemClickListener(this));


        recyclerView.setAdapter(categoryAdapter);

        // Handle the submit button click
        searchView.setOnQueryTextListener(getSearchViewListener());

        // Load categories in the background
        loadCategoriesInBackground();

        // todo: handle expired activity or device changed orientation
    }

    @NonNull
    private static SearchView.OnQueryTextListener getSearchViewListener() {
        return new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                // Handle the search query here (e.g., filter the category list)
                // For simplicity, this example doesn't implement filtering
                Log.d(TAG, "onQueryTextSubmit: " + s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        };
    }

    private void loadCategoriesInBackground() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
//            Log.d(TAG, "loadHomepageRaws a ");
            for (AbstractServer server : serverManager.getServers()) {

//                if (server == null || server.getConfig() == null|| !server.getConfig().isActive()){
//                    continue;
//                }
                if (
                        server instanceof OldAkwamServer ||
                                server instanceof CimaClubServer ||
//                                    server instanceof FaselHdController ||
                                    server instanceof AkwamServer ||
                                    server instanceof ArabSeedServer ||
                                    server instanceof IptvServer ||
                                    server instanceof MyCimaServer
                ) {
                    continue;
                }
                // Update the RecyclerView on the main thread
                ArrayList<Movie> movies = server.getHomepageMovies();

                if (movies == null || movies.isEmpty()) {
                    continue;
                }

                Category category = new Category(server.getLabel(), movies);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        categoryList.add(category);
                        categoryAdapter.notifyItemInserted(categoryList.size() - 1);
                    }
                });
            }
        });
        executor.shutdown();
    }


    //                Intent intent = new Intent(context, MobileMovieDetailActivity.class);
    //                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_TITLE, movie.getTitle());
    //                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_IMAGE_URL, movie.getCardImageUrl());
    //                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_CATEGORY, "Action"); // Replace "Action" with the actual category
    //                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_RATING, movie.getRate());
    //                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_STUDIO, movie.getStudio());
    //                context.startActivity(intent);


    public class MovieItemClickListener implements OnMovieClickListener {
        Activity activity;

        CategoryAdapter categoryAdapter;

        public MovieItemClickListener(Activity activity){
            this.activity = activity;
        }
        public void onMovieClick(Movie movie, int position, HorizontalMovieAdapter horizontalMovieAdapter) {
// Check if the clicked movie matches the criteria to extend the category
//            Log.d(TAG, "onMovieClick: "+ categoryAdapter.getItemCount());
            clickedHorizontalMovieAdapter = horizontalMovieAdapter;
            if (movie.getState() == Movie.COOKIE_STATE){
                Log.d(TAG, "onMovieClick: COOKIE_STATE");
                fetchCookie(movie);
                return;
            }

            if (shouldExtendCategory(movie)) {
                extendMovieListForCategory(movie, position, categoryAdapter, horizontalMovieAdapter);
            } else {
                // Handle normal click event (e.g., open detail activity)
                Intent intent = new Intent(activity, MobileMovieDetailActivity.class);
//                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_TITLE, movie.getTitle());
//                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_IMAGE_URL, movie.getCardImageUrl());
//                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_RATING, movie.getRate());
//                intent.putExtra(MobileMovieDetailActivity.EXTRA_MOVIE_STUDIO, movie.getStudio());
                intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
                startActivity(intent);
            }
        }

        private void fetchCookie(Movie movie) {
            AbstractServer server = Util.determineServer(movie, null, activity, null);
            if (server == null){
                return;
            }
            server.fetch(movie);
        }

        public CategoryAdapter getCategoryAdapter() {
            return categoryAdapter;
        }

        public void setCategoryAdapter(CategoryAdapter categoryAdapter) {
            this.categoryAdapter = categoryAdapter;
        }

        private void extendMovieListForCategory(Movie movie, int position, CategoryAdapter categoryAdapter, HorizontalMovieAdapter horizontalMovieAdapter)
        {
          Log.d(TAG, "extendMovieListForCategory: p:"+ position+ ", s:"+horizontalMovieAdapter.getItemCount());
          Movie movie1 = horizontalMovieAdapter.getMovieList().get(0);
            horizontalMovieAdapter.getMovieList().add(movie1);

            handler.post(new Runnable() {
                @Override
                public void run() {
//                    categoryList.add(category);
//                    categoryAdapter.notifyItemInserted(categoryList.size() - 1);
//                    horizontalMovieAdapter.notifyItemInserted(categoryList.size() - 1);
                    horizontalMovieAdapter.notifyDataSetChanged();
                }
            });
        }

        private boolean shouldExtendCategory(Movie movie) {
            return movie.getState() == Movie.NEXT_PAGE_STATE;
//            return true;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("TAG", "onActivityResult: adapter item count ");
//        Log.d("TAG", "onActivityResult: adapter item count "+ clickedHorizontalMovieAdapter);

        // cases:
        // 5.Movie.REQUEST_CODE_MOVIE_LIST to extend the movie list in row
        // should update:
        // 1.movie list
//
//        if (resultCode != Activity.RESULT_OK || data == null) {
//            Log.d(TAG, "onActivityResult:RESULT_NOT_OK ");
//            return;
//        }
//        Gson gson = new Gson();
//
//        Movie resultMovie = (Movie) data.getSerializableExtra(DetailsActivity.MOVIE);
//        Type type = new TypeToken<List<Movie>>() {
//        }.getType();
//        String movieSublistString = data.getStringExtra(DetailsActivity.MOVIE_SUBLIST);
//
//        List<Movie> resultMovieSublist = gson.fromJson(movieSublistString, type);
//
//        String result = data.getStringExtra("result");
//
//
//        Log.d(TAG, "onActivityResult:RESULT_OK ");
//
//        //requestCode Movie.REQUEST_CODE_MOVIE_UPDATE is one movie object or 2 for a list of movies
//        //this result is only to update the clicked movie of the sublist only and in some cases to update the description of mSelectedMovie
//        //the id property of Movie object is used to identify the index of the clicked sublist movie
////        switch (requestCode) {
////            case Movie.REQUEST_CODE_MOVIE_UPDATE:
////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_MOVIE_UPDATE");
////            case Movie.REQUEST_CODE_MOVIE_LIST:
////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_MOVIE_LIST");
////                // returns null or movie
////                mSelectedMovie = updateMovieOnActivityResult(resultMovie, resultMovieSublist);
////                break;
////            case Movie.REQUEST_CODE_FETCH_HTML:
////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_FETCH_HTML");
////                mSelectedMovie = (Movie) server.handleOnActivityResultHtml(result, mSelectedMovie);
////                break;
////            case Movie.REQUEST_CODE_EXOPLAYER:
////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_EXOPLAYER");
////                resultMovie.setSubList(resultMovieSublist);
////                Util.openExoPlayer(resultMovie, activity);
////                // todo: handle dbHelper
////                // dbHelper.addMainMovieToHistory(mSelectedMovie);
////                mSelectedMovie = resultMovie;
////                break;
////            case Movie.REQUEST_CODE_EXTERNAL_PLAYER:
////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE_EXTERNAL_PLAYER");
////                Util.openExternalVideoPlayer(resultMovie, activity);
////                // todo: handle dbHelper
////                // dbHelper.addMainMovieToHistory(mSelectedMovie);
////                mSelectedMovie = resultMovie;
////                break;
////            default:
////                Log.d(TAG, "handleOnDetailsActivityResult: REQUEST_CODE unknown: "+ requestCode);
////                mSelectedMovie = resultMovie;
////        }
//
////        updateRelatedMovieAdapter(mSelectedMovie);
        super.onActivityResult(requestCode, resultCode, data);
    }
}