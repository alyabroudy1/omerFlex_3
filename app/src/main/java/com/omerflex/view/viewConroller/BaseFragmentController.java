package com.omerflex.view.viewConroller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.omerflex.entity.Movie;
import com.omerflex.server.Util;
import com.omerflex.view.CardPresenter;
import com.omerflex.view.listener.MovieItemViewClickedListener;

import com.omerflex.view.handler.ActivityResultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseFragmentController {

    private static final String TAG = "BaseFragmentController";
    private static final int BACKGROUND_UPDATE_DELAY = 300;

    protected BrowseSupportFragment mFragment;
    protected ArrayObjectAdapter mRowsAdapter;
    protected BackgroundManager mBackgroundManager;
    protected DisplayMetrics mMetrics;
    protected Drawable mDefaultBackground;
    protected final Handler mHandler = new Handler(Looper.myLooper());
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private ActivityResultHandler activityResultHandler;
    // A single, shared executor service for all background tasks
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    public BaseFragmentController(BrowseSupportFragment fragment, ArrayObjectAdapter rowsAdapter, Drawable defaultBackground) {
        this.mFragment = fragment;
        this.mRowsAdapter = rowsAdapter;
        this.mDefaultBackground = defaultBackground;
        this.mBackgroundManager = BackgroundManager.getInstance(fragment.getActivity());
        this.mMetrics = new DisplayMetrics();
        this.activityResultHandler = new ActivityResultHandler(fragment.getActivity());
        fragment.getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        this.mBackgroundManager.attach(fragment.getActivity().getWindow());
        setupEventListeners();
    }

    protected void setupEventListeners() {
        mFragment.setOnItemViewClickedListener(new MovieItemViewClickedListener(mFragment, mRowsAdapter));
        mFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    // Abstract method to be implemented by subclasses for specific data loading.
    public abstract void loadData();

    // Method to add a new row of movies to the adapter.
    public void addMovieRow(HeaderItem header, List<Movie> movies) {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter.addAll(0, movies);
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    // Method to update the background image.
    protected void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(mFragment.getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(@NonNull Drawable drawable, @Nullable Transition<? super Drawable> transition) {
                        mBackgroundManager.setDrawable(drawable);
                    }
                });
        if (mBackgroundTimer != null) {
            mBackgroundTimer.cancel();
        }
    }

    protected void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        activityResultHandler.handleResult(requestCode, resultCode, data, mRowsAdapter);
    }


    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Movie) {
                mBackgroundUri = ((Movie) item).getBackgroundImageUrl();
                startBackgroundTimer();
            }
        }
    }

    private class UpdateBackgroundTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(() -> updateBackground(mBackgroundUri));
        }
    }
}