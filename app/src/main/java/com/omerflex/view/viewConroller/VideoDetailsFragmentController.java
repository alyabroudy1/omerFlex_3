package com.omerflex.view.viewConroller;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.ListRow;

import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.MovieRepository;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.ServerInterface;
import com.omerflex.server.Util;
import com.omerflex.server.config.ServerConfigRepository;
import com.omerflex.view.DetailsActivity;
import com.omerflex.view.VideoDetailsFragment;
import com.omerflex.view.handler.ActivityResultHandler;
import com.omerflex.viewmodel.SharedViewModel;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoDetailsFragmentController {
    private static final String TAG = "VideoDetailsController";

    private static final int ACTION_WATCH = 1;
    private static final int ACTION_WATCH_TRAILER = 2;
    private VideoDetailsFragment fragment;
    private ArrayObjectAdapter mAdapter;
    private DetailsOverviewRow row;
    private Movie mSelectedMovie;
    private MovieRepository movieRepository;
    private ArrayObjectAdapter listRowAdapter;
    private ActivityResultHandler activityResultHandler;
    private SharedViewModel sharedViewModel;

    public VideoDetailsFragmentController(VideoDetailsFragment fragment, ArrayObjectAdapter adapter, DetailsOverviewRow row, Movie movie, ArrayObjectAdapter listRowAdapter, SharedViewModel sharedViewModel) {
        this.fragment = fragment;
        this.mAdapter = adapter;
        this.row = row;
        this.mSelectedMovie = movie;
        this.movieRepository = MovieRepository.getInstance(fragment.getActivity());
        this.listRowAdapter = listRowAdapter;
        this.activityResultHandler = new ActivityResultHandler(fragment.getActivity());
        this.sharedViewModel = sharedViewModel;
    }

    public void fetchDetails() {
        fragment.showProgressDialog(true);
        Log.d(TAG, "fetchDetails: ");
        movieRepository.fetchMovieDetails( mSelectedMovie, fetchedMovie -> {
                sharedViewModel.updateMovie(fetchedMovie);
                listRowAdapter.addAll(0, fetchedMovie.getSubList());

            fragment.hideProgressDialog(true, null);
            evaluateWatchAction();
        });
    }

    private void updateOverview(Movie movie) {
        mSelectedMovie.setTitle(movie.getTitle());
        mSelectedMovie.setDescription(movie.getDescription());
        mSelectedMovie.setBackgroundImageUrl(movie.getBackgroundImageUrl());

        if (fragment != null) {
            fragment.updateOverviewUI(mSelectedMovie);
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        activityResultHandler.handleResult(requestCode, resultCode, data, mAdapter);
    }

    public void evaluateWatchAction() {
        if (mSelectedMovie == null) return;
        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();
        actionAdapter.add(new Action(ACTION_WATCH, fragment.getResources().getString(R.string.watch)));
        if (mSelectedMovie.getTrailerUrl() != null && mSelectedMovie.getTrailerUrl().length() > 2) {
            actionAdapter.add(new Action(ACTION_WATCH_TRAILER, fragment.getResources().getString(R.string.watch_trailer_1)));
        }
        row.setActionsAdapter(actionAdapter);
        mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(row), mAdapter.size());
    }
    public void handleActionClick(Movie movie, Action action, ArrayObjectAdapter clickedAdapter) {
        if (movie == null || action == null || clickedAdapter == null) {
            Log.e(TAG, "handleActionClick: Invalid input parameters");
            return;
        }

        long actionId = action.getId();
        if (actionId == ACTION_WATCH_TRAILER) {
            Log.d(TAG, "onActionClicked: Trailer: " + movie.getTitle());
            Util.openBrowserIntent(movie, fragment, true, false, false, 11);
            return;
        }

        if (actionId != ACTION_WATCH) {
            return;
        }

        Movie watchMovie = (clickedAdapter.size() > 0) ? (Movie) clickedAdapter.get(0) : null;
        if (watchMovie == null) {
            Log.e(TAG, "handleActionClick: Empty sublist");
            return;
        }
        watchMovie.setRowIndex(0); // Update the correct item of the row

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> Toast.makeText(fragment.getActivity(), "الرجاء الانتظار...", Toast.LENGTH_SHORT).show());

        executor.submit(() -> {
            try {
                AbstractServer server = ServerConfigRepository.getInstance().getServer(movie.getStudio());
                if (server == null) {
                    Log.e(TAG, "handleActionClick: Undefined server");
                    return;
                }

                if (watchMovie.getState() == Movie.VIDEO_STATE) {
                    Log.d(TAG, "handleActionClick: Already video");
                    Util.openExoPlayer(watchMovie, fragment.getActivity(), true);
                    return;
                }

                server.fetch(watchMovie, Movie.ACTION_WATCH_LOCALLY, new ServerInterface.ActivityCallback<Movie>() {
                    @Override
                    public void onSuccess(Movie result, String title) {
                        if (result == null || result.getVideoUrl() == null) {
                            Log.e(TAG, "onSuccess: Empty result");
                            return;
                        }
                        Log.d(TAG, "onActionClicked: Resolutions " + result);
                        mainHandler.post(() -> {
                            Util.openExoPlayer(result, fragment.getActivity(), true);
                            clickedAdapter.replace(0, result);
                            updateOverview(result);
                        });
                    }

                    @Override
                    public void onInvalidCookie(Movie result, String title) {
                        Log.e(TAG, "onInvalidCookie: " + (result != null ? result.getVideoUrl() : "null"));
                        if (result == null){
                            Log.d(TAG, "onInvalidCookie: empty result");
                            return;
                        }
                        result.setFetch(Movie.REQUEST_CODE_EXOPLAYER);
                        if (fragment != null){
                            Util.openBrowserIntent(result, fragment, false, true, true,0);
                            return;
                        }
                        Util.openBrowserIntent(result, fragment.getActivity(), false, true, true,0);
                    }

                    @Override
                    public void onInvalidLink(Movie result) {
                        Log.e(TAG, "onInvalidLink: " + (result != null ? result.getVideoUrl() : "null"));
                    }

                    @Override
                    public void onInvalidLink(String message) {
                        Log.e(TAG, "onInvalidLink: " + message);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "handleActionClick: Error processing request", e);
            } finally {
                executor.shutdown();
            }
        });
    }
}
