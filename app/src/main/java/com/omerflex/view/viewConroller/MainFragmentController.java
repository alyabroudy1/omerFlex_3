package com.omerflex.view.viewConroller;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.omerflex.OmerFlexApplication;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieRepository;
import com.omerflex.service.UpdateService;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainFragmentController extends BaseFragmentController {

    private MovieRepository movieRepository;
    UpdateService updateService;
    public static String TAG = "MainFragmentController";
    private final AtomicBoolean subsequentCallsInitiated = new AtomicBoolean(false);

    public MainFragmentController(BrowseSupportFragment fragment, ArrayObjectAdapter rowsAdapter, Drawable defaultBackground) {
        super(fragment, rowsAdapter, defaultBackground);
        this.movieRepository = MovieRepository.getInstance(fragment.getActivity(), ((OmerFlexApplication) fragment.getActivity().getApplication()).getDatabase().movieDao());
        updateService = new UpdateService(mFragment);
    }

    @Override
    public void loadData() {
        Log.d(TAG, "loadData: ");
        subsequentCallsInitiated.set(false);
        movieRepository.getHomepageMovies(false, this::onHomepageMoviesLoaded);
    }

    private void onHomepageMoviesLoaded(String category, ArrayList<Movie> movieList) {
        if (movieList != null && !movieList.isEmpty()) {
            Log.d("Movie", "Fetched movie33: " + movieList.toString());
            HeaderItem header = new HeaderItem(1, category);
            addMovieRow(header, movieList);
        } else {
            Log.d("Movie", "movieList not found.");
        }

        if (subsequentCallsInitiated.compareAndSet(false, true)) {
            movieRepository.getWatchedMovies(this::onMoviesLoaded);
            movieRepository.getWatchedChannels(this::onMoviesLoaded);
            movieRepository.getHomepageChannels(this::onHomepageChannelsLoaded);
        }
    }

    private void onMoviesLoaded(String category, ArrayList<Movie> movieList) {
        if (movieList != null) {
            Log.d(TAG, "Fetched onMoviesLoaded: " + movieList.size());
            HeaderItem header = new HeaderItem(1, category);
            addMovieRow(header, movieList);
        } else {
            Log.d(TAG, "onMoviesLoaded: movieList not found.");
        }
    }

    private void onHomepageChannelsLoaded(String category, ArrayList<Movie> movieList) {
        if (movieList != null) {
            Log.d(TAG, "Fetched getHomepageChannels: " + movieList.size());
            HeaderItem header = new HeaderItem(1, category);
            addMovieRow(header, movieList);
        } else {
            Log.d(TAG, "getHomepageChannels: movieList not found.");
        }
    }


    @Override
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleActivityResult(requestCode, resultCode, data);
        updateService.handleOnActivityResult(requestCode, resultCode, data);
    }

    public void handleOnRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        updateService.handleOnRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void handleOnDestroy() {
        updateService.handleOnDestroy();
    }

    public void loadSearchData(String query) {
        Log.d(TAG, "loadSearchData: " + query);
        movieRepository.getSearchMovies(query, this::onMoviesLoaded);
    }
}