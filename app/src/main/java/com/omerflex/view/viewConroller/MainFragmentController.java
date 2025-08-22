package com.omerflex.view.viewConroller;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.omerflex.entity.MovieRepository;

public class MainFragmentController extends BaseFragmentController {

    private MovieRepository movieRepository;
    public static String TAG = "MainFragmentController";

    public MainFragmentController(BrowseSupportFragment fragment, ArrayObjectAdapter rowsAdapter, Drawable defaultBackground) {
        super(fragment, rowsAdapter, defaultBackground);
        this.movieRepository = MovieRepository.getInstance();
    }

    @Override
    public void loadData() {
        Log.d(TAG, "loadData: ");
        movieRepository.getHomepageMovies( movieList -> {
            if (movieList != null) {
                Log.d("Movie", "Fetched movie33: " + movieList.toString());
                // todo let the id be the studio name
                HeaderItem header = new HeaderItem(1, "Featured Movies");
                addMovieRow(header, movieList);
            } else {
                Log.d("Movie", "movieList not found.");
            }
        });

        movieRepository.getHomepageMovies( movieList -> {
            if (movieList != null) {
                Log.d("Movie", "Fetched movie33: " + movieList.toString());
                HeaderItem header = new HeaderItem(2, "Featured Movies 22");
                addMovieRow(header, movieList);
            } else {
                Log.d("Movie", "movieList not found.");
            }
        });
    }
}