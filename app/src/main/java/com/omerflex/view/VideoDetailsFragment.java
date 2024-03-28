package com.omerflex.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.app.DetailsSupportFragmentBackgroundController;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.omerflex.entity.Movie;
import com.omerflex.R;
import com.omerflex.entity.MovieHistory;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.Util;
import com.omerflex.service.ServerManager;
import com.omerflex.service.database.MovieDbHelper;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class VideoDetailsFragment extends DetailsSupportFragment {
    private static final String TAG = "VideoDetailsFragment";

    public static final int ACTION_OPEN_DETAILS_ACTIVITY = 1;
    public static final int ACTION_OPEN_EXTERNAL_ACTIVITY = 2;
    public static final int ACTION_OPEN_NO_ACTIVITY = 3;

    private static final int ACTION_WATCH = 1;
    private static final int ACTION_WATCH_TRAILER = 2;
    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int NUM_COLS = 10;

    private Movie mSelectedMovie;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private DetailsSupportFragmentBackgroundController mDetailsBackground;

    // ********
    private ProgressDialog mProgressDialog;

    public MovieDbHelper dbHelper;
    Handler movieHandler;


    AbstractServer server;
    DetailsDescriptionPresenter detailsDescriptionPresenter;
    ArrayObjectAdapter listRowAdapter;
    FullWidthDetailsOverviewRowPresenter detailsPresenter;


    DetailsOverviewRow row;
    Fragment fragment = this;

    // ********

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mSelectedMovie = (Movie) Objects.requireNonNull(getActivity()).getIntent().getSerializableExtra(DetailsActivity.MOVIE);
        Movie mSelectedMovieMainMovie = (Movie) Objects.requireNonNull(getActivity()).getIntent().getSerializableExtra(DetailsActivity.MAIN_MOVIE);

        if (mSelectedMovie != null) {
            mSelectedMovie.setMainMovie(mSelectedMovieMainMovie);

            //very important to initialize all required
            initializeThings();
            if (mSelectedMovie.getMovieHistory() == null && mSelectedMovie.getMainMovie() != null) {
                mSelectedMovie.setMovieHistory(dbHelper.getMovieHistoryByMainMovie(Util.getUrlPathOnly(mSelectedMovie.getMainMovie().getVideoUrl())));
            }



            setupRowsAndServer();
        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        //   Log.d(TAG, "onStart::: " + mSelectedMovie.getStudio());
        //fetch server and load description and bg image
//        server = ServerManager.determineServer(mSelectedMovie, listRowAdapter, getActivity(), this);
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Objects.requireNonNull(getView()).requestFocus();
    }

    private void initializeThings() {
        dbHelper = MovieDbHelper.getInstance(getActivity());

        mDetailsBackground = new DetailsSupportFragmentBackgroundController(this);
        detailsDescriptionPresenter = new DetailsDescriptionPresenter();
        listRowAdapter = new ArrayObjectAdapter(new CardPresenter());


        String movieJson = Objects.requireNonNull(getActivity()).getIntent().getStringExtra(DetailsActivity.MOVIE_SUBLIST);
        Gson gson = new Gson();
        Type type = new TypeToken<List<Movie>>() {
        }.getType();
        List<Movie> movieSublist = gson.fromJson(movieJson, type);
        Log.d(TAG, "onCreate: subList:" + movieSublist);
        if (movieSublist != null) {
            mSelectedMovie.setSubList(movieSublist);
        }

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setTitle("جاري التحميل...");
        mProgressDialog.setMessage("الرجاء الانتظار...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);

        server = ServerManager.determineServer(mSelectedMovie, listRowAdapter, getActivity(), fragment);

        mPresenterSelector = new ClassPresenterSelector();
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);

        setupDetailsOverviewRow();
        setupDetailsOverviewRowPresenter();
        setupRelatedMovieListRow();
        setAdapter(mAdapter);
        initializeBackground(mSelectedMovie);

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }


    //fetch and update current movie
    private void setupRowsAndServer() {

        //todo: 1. at start init and fetch current movie and update the current movie accordingly
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // showProgressDialog(false);
        executor.submit(() -> {
            mSelectedMovie = (Movie) server.fetch(mSelectedMovie);
            Log.d(TAG, "setupRowsAndServer: mSelectedMovie after first fetch:" + mSelectedMovie);
            if (mSelectedMovie.getSubList() != null) {
                listRowAdapter.addAll(0, mSelectedMovie.getSubList());
                Movie firstSubMovie = mSelectedMovie.getSubList().get(0);
                if (firstSubMovie != null) {
                    boolean watchCond = firstSubMovie.getState() == Movie.RESOLUTION_STATE || firstSubMovie.getState() == Movie.VIDEO_STATE ;
                    boolean watchOmarCond = mSelectedMovie.getStudio().equals(Movie.SERVER_OMAR) && firstSubMovie.getState() > Movie.ITEM_STATE;
                    if (watchCond || watchOmarCond) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadActionRow();
                            }
                        });
                    }
                }
            }
            mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(row), mAdapter.size());
            mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(listRowAdapter), mAdapter.size());
            setSelectedPosition(getAdapter().size()-1, true);
            Log.d(TAG, "setupRowsAndServer: mSelectedMovie sublist after first fetch:" + mSelectedMovie.getSubList().toString());

            hideProgressDialog(true);
        });

        executor.shutdown();

//        movieHandler = new Handler() {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                loadActionRow();
//                Log.d(TAG, "handle what: " + msg.what);
//                if (mSelectedMovie.getSubList() != null) {
//                    listRowAdapter.addAll(0, mSelectedMovie.getSubList());
//                    Log.d(TAG, "handleMessage: sublist Added: " + mSelectedMovie.getSubList());
//                }
//                mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(row), mAdapter.size());
//                mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(listRowAdapter), mAdapter.size());
//                super.handleMessage(msg);
//            }
//        };


//        Log.d(TAG, "setupRowsAndServer: " + mSelectedMovie.toString());
//
//
//        if (mSelectedMovie.getFetch() != 0) {
//            Log.d(TAG, "setupRowsAndServer: getSubList:" + mSelectedMovie.getSubList());
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//
//            executor.submit(() -> {
//                Log.d(TAG, "run: save movie 1");
//                if (mSelectedMovie.getStudio().equals(Movie.SERVER_SHAHID4U)
//                        || mSelectedMovie.getStudio().equals(Movie.SERVER_CIMA4U)
//                        || mSelectedMovie.getStudio().equals(Movie.SERVER_CIMA_CLUB)
//                        || (mSelectedMovie.getStudio().equals(Movie.SERVER_FASELHD)
//                        && mSelectedMovie.getState() == Movie.RESULT_STATE)
//                ) {
//                    mSelectedMovie.setIsHistory(1);
//                }
//                mSelectedMovie.save(dbHelper);
//                Movie fetchedMovie = (Movie) Objects.requireNonNull(server).fetch(mSelectedMovie);
//                if (fetchedMovie != null) {
//                    mSelectedMovie = fetchedMovie;
//                }
//
//                hideProgressDialog();
//                movieHandler.sendEmptyMessage(0);
//            });
//
//            executor.shutdown();
//        } else {
//            if (mSelectedMovie.getSubList() != null) {
//                listRowAdapter.addAll(0, mSelectedMovie.getSubList());
//            }
//            loadActionRow();
//            hideProgressDialog();
//        }

    }


    private void loadActionRow() {
        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();
        //boolean watchActionCond = mSelectedMovie.getVideoUrl() != null && ( !(mSelectedMovie.getStudio().equals(Movie.SERVER_CIMA_CLUB) || mSelectedMovie.getStudio().equals(Movie.SERVER_SHAHID4U) || mSelectedMovie.getStudio().equals(Movie.SERVER_CIMA4U)) && (mSelectedMovie.getState() == Movie.ITEM_STATE || mSelectedMovie.getState() == Movie.RESOLUTION_STATE || mSelectedMovie.getState() == Movie.VIDEO_STATE));
        int state = mSelectedMovie.getState();
        String studio = mSelectedMovie.getStudio();
        boolean watchFaselCond = state == Movie.VIDEO_STATE;
        boolean watchCond = state == Movie.ITEM_STATE && (studio.equals(Movie.SERVER_AKWAM) || studio.equals(Movie.SERVER_MyCima));
        //   Log.d(TAG, "loadActionRow: watchCond:"+watchActionCond);
//        if (watchFaselCond || watchCond) {
//            // if (watchActionCond) {
//            actionAdapter.add(
//                    new Action(
//                            ACTION_WATCH,
//                            getResources().getString(R.string.watch)));
//            setSelectedPosition(0);
//        } else {
//            setSelectedPosition(1);
//        }
        actionAdapter.add(
                new Action(
                        ACTION_WATCH,
                        getResources().getString(R.string.watch)));
        actionAdapter.notifyArrayItemRangeChanged(0, actionAdapter.size() - 1);
        setSelectedPosition(0, true);
        if (mSelectedMovie.getTrailerUrl() != null) {
            if (mSelectedMovie.getTrailerUrl().length() > 2) {
                actionAdapter.add(
                        new Action(
                                ACTION_WATCH_TRAILER,
                                getResources().getString(R.string.watch_trailer_1)));
            }
        }
        row.setActionsAdapter(actionAdapter);
    }




    private void initializeBackground(Movie data) {
        mDetailsBackground.enableParallax();
            server = ServerManager.determineServer(mSelectedMovie, null, getActivity(), this);
            if (server.getHeaders() != null) {
                String cookies = server.getCookies();
                if (cookies == null) {
                    cookies = "";
                }

                LazyHeaders.Builder builder = new LazyHeaders.Builder()
                        .addHeader("Cookie", cookies);

                for (Map.Entry<String, String> entry : server.getHeaders().entrySet()) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
//            GlideUrl glideUrl = new GlideUrl(data.getBackgroundImageUrl(), builder.build());
//            if (mSelectedMovie.getStudio().equals(Movie.SERVER_FASELHD)){
//                glideUrl = new GlideUrl("https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png", builder.build());
//            }

                if (data.getBackgroundImageUrl() == null || data.getBackgroundImageUrl().equals("")) {
                    data.setBackgroundImageUrl(data.getCardImageUrl());
                }

                Glide.with(getActivity())
                        .asBitmap()
                        //.fitCenter()
                        .centerCrop()
                        //  .load(glideUrl)
                        .load(data.getBackgroundImageUrl())
                        .error(R.drawable.default_background_2)
                        .fallback(R.drawable.default_background_2)
                        .placeholder(R.drawable.default_background_2)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap bitmap,
                                                        @Nullable Transition<? super Bitmap> transition) {
                                mDetailsBackground.setCoverBitmap(bitmap);
                                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                            }
                        });

            }
            else {
            Glide.with(getActivity())
                .asBitmap()
                .centerCrop()
                .error(R.drawable.default_background_2)
                    .fallback(R.drawable.default_background_2)
                    .placeholder(R.drawable.default_background_2)
                .load(data.getBackgroundImageUrl())
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap,
                                                @Nullable Transition<? super Bitmap> transition) {
                        mDetailsBackground.setCoverBitmap(bitmap);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });
            }
    }

    private void setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());
        row = new DetailsOverviewRow(mSelectedMovie);
        row.setImageDrawable(
                ContextCompat.getDrawable(getContext(), R.drawable.default_background_2));
        int width = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);
        Glide.with(getActivity())
                .load(mSelectedMovie.getCardImageUrl())
//                .centerCrop()
                .fitCenter()
                .error(R.drawable.default_background_2)
                .fallback(R.drawable.default_background_2)
                .placeholder(R.drawable.default_background_2)
                .into(new SimpleTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(@NonNull Drawable drawable,
                                                @Nullable Transition<? super Drawable> transition) {
                        Log.d(TAG, "details overview card image url ready: " + drawable);
                        row.setImageDrawable(drawable);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });

        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {
        Handler movieResHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.d(TAG, "handle what: " + msg.what);
                mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(row), mAdapter.size());
                super.handleMessage(msg);
            }
        };

        // Set detail background.
        detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getContext(), R.color.selected_background));

        // Hook up transition element.
        FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                new FullWidthDetailsOverviewSharedElementHelper();
        sharedElementHelper.setSharedElementEnterTransition(
                getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(sharedElementHelper);
        detailsPresenter.setParticipatingEntranceTransition(true);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                handleActionClick(action);
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private void handleActionClick(Action action) {
        Intent intent;
        boolean condTrailer = action.getId() == ACTION_WATCH_TRAILER;
        boolean condWatch1 = action.getId() == ACTION_WATCH;

        if (condTrailer) {
            intent = new Intent(getActivity(), BrowserActivity.class);
            Movie trailer = new Movie();
            trailer.setVideoUrl(mSelectedMovie.getTrailerUrl());
            trailer.setState(Movie.BROWSER_STATE);
            trailer.setTitle(mSelectedMovie.getTitle());
            trailer.setStudio(mSelectedMovie.getStudio());
            intent.putExtra(DetailsActivity.MOVIE, (Serializable) trailer);
            Log.d(TAG, "onActionClicked: Trailer: " + trailer.getTitle());
            startActivity(intent);
        } else if (condWatch1) {
            boolean condWatch2 = listRowAdapter.size() > 0;
            if (condWatch2) {

                showProgressDialog(false);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                showProgressDialog(false);
                Toast.makeText(getActivity(), "الرجاء الانتظار...", Toast.LENGTH_SHORT).show();
                executor.submit(() -> {
                    // mSelectedMovie = Objects.requireNonNull(server).fetchItem(mSelectedMovie);
                    Movie movie = (Movie) listRowAdapter.get(0);
                    movie.setRowIndex(0); // very important to update the correct item of the row
                    Log.d(TAG, "run: condWatch2 " + movie);
                    Movie res = server.fetchToWatchLocally(movie);
                    if (res != null && res.getVideoUrl() != null) {
                        String type = "video/*";
                        // Uri uri = Uri.parse(res.getSubList().get(0).getVideoUrl());
                        Log.d(TAG, "onActionClicked: Resolutions " + res);
                        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Intent intent = new Intent(activity, PlaybackActivity.class);
                                Intent exoIntent = new Intent(getActivity(), ExoplayerMediaPlayer.class);
                                exoIntent.putExtra(DetailsActivity.MOVIE, (Serializable) res);
                                exoIntent.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) res.getMainMovie());
                                //intent.putExtra(DetailsActivity.MOVIE, (Serializable) res.getSubList().get(0));
                                Objects.requireNonNull(getActivity()).startActivity(exoIntent);
                                //dbHelper.addMainMovieToHistory(res.getMainMovieTitle(), null);
                                updateItemFromActivityResult(res);
                                dbHelper.addMainMovieToHistory(mSelectedMovie);
                            }
                        });

                    } else {
                        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "الرجاء الانتظار...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    hideProgressDialog(true);
                });

                executor.shutdown();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.watch_trailer_2), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.watch_trailer_2), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRelatedMovieListRow() {
        String headerTitle = getResources().getString(R.string.related_movie_row_header_resolutions);
        if (mSelectedMovie.getState() == Movie.GROUP_OF_GROUP_STATE) {
            headerTitle = getResources().getString(R.string.related_movie_row_header_session);
        } else if (mSelectedMovie.getState() == Movie.GROUP_STATE) {
            headerTitle = getResources().getString(R.string.related_movie_row_header_series);
        }

        HeaderItem header = new HeaderItem(0, headerTitle);
        mAdapter.add(new ListRow(header, listRowAdapter));
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
    }

    private int convertDpToPixel(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {

            handleItemClick(itemViewHolder, item, rowViewHolder, row);
        }
    }
    private void handleItemClick(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder,
                                 Row row) {
        if (item instanceof Movie) {
            Log.d(TAG, "Item: " + item.toString());
            int nextAction = server.fetchNextAction((Movie) item);

            generateMovieHistory(mSelectedMovie, (Movie) item);
            //find the adapter to get the item index in order to identify and update the correct clicked item on returned result
            ArrayObjectAdapter adapter = (ArrayObjectAdapter) ((ListRow) row).getAdapter();
            int itemIndexInRow = adapter.indexOf(item);
            Log.d(TAG, "handleItemClick: itemIndex:" + itemIndexInRow);
            ((Movie) item).setRowIndex(itemIndexInRow);
            Log.d(TAG, "handleItemClick: id: " + ((Movie) item).getRowIndex());

            if (nextAction == ACTION_OPEN_DETAILS_ACTIVITY) {
                Log.d(TAG, "onItemClicked: new Activity");
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(getResources().getString(R.string.movie), (Serializable) (Movie) item);
                intent.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) (Movie) ((Movie) item).getMainMovie());
                Bundle bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        getActivity(),
                                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                        DetailsActivity.SHARED_ELEMENT_NAME)
                                .toBundle();
                getActivity().startActivity(intent, bundle);
            }
            else if (nextAction == ACTION_OPEN_EXTERNAL_ACTIVITY) { // means to run movie with external video player usually when movie in Video state is
                Log.d(TAG, "onItemClicked: fetch only");

                showProgressDialog(false);
                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.submit(() -> {
                    Movie movie = server.fetch((Movie) item);

                    Log.d(TAG, "run: fetch only save movie 2");

                    if (movie != null && movie.getVideoUrl() != null) {
                        //dbHelper.addMainMovieToHistory(movie.getMainMovieTitle(), null);
                        getActivity().runOnUiThread(() -> {
                            String type = "video/*"; // It works for all video application
                            Uri uri = Uri.parse(movie.getVideoUrl());
                            Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
                            in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            //  in1.setPackage("org.videolan.vlc");
                            in1.setDataAndType(uri, type);
                            // movie.getMainMovie().save(dbHelper);
                            hideProgressDialog(true);

                            getActivity().startActivity(in1);
                            updateItemFromActivityResult(movie);
                            dbHelper.addMainMovieToHistory(mSelectedMovie);
                        });
                    }
                });

                executor.shutdown();

            }
            else {
                if (!((Movie) item).getVideoUrl().equals("")){
                    Toast.makeText(getActivity(), "الرجاء الانتظار...", Toast.LENGTH_LONG).show();
                }
                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.submit(() -> {
                    //dbHelper.addMainMovieToHistory(((Movie) item).getMainMovieTitle(), null);
                    //fetch movie and the server is responsible for playing the video internal or external
                    Movie movie = server.fetch((Movie) item);
                    Log.d(TAG, "handleItemClick: open No Activity: " + movie);
                    if (movie != null) {
                        getActivity().runOnUiThread(() -> {
                            String type = "video/*"; // It works for all video application
                            Uri uri = Uri.parse(movie.getVideoUrl());
                            Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
                            in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            //  in1.setPackage("org.videolan.vlc");
                            in1.setDataAndType(uri, type);
                            hideProgressDialog(true);

                            getActivity().startActivity(in1);
                            updateItemFromActivityResult(movie);
                            dbHelper.addMainMovieToHistory(mSelectedMovie);

                        });
                    }
                });

                executor.shutdown();

            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + requestCode + ", " + data);
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "onActivityResult:RESULT_OK ");
            if (data != null) {
                Gson gson = new Gson();
                //requestCode Movie.REQUEST_CODE_MOVIE_UPDATE is one movie object or 2 for a list of movies
                //this result is only to update the clicked movie of the sublist only and in some cases to update the description of mSelectedMovie
                //the id property of Movie object is used to identify the index of the clicked sublist movie
                if (requestCode == Movie.REQUEST_CODE_MOVIE_UPDATE) {
                    Movie resultMovie = (Movie) data.getSerializableExtra(DetailsActivity.MOVIE);
                    Log.d(TAG, "onActivityResult: REQUEST_CODE_MOVIE_UPDATE: " + resultMovie);
                    updateItemFromActivityResult(resultMovie);
                } else if (requestCode == Movie.REQUEST_CODE_EXOPLAYER) {
                    Movie resultMovie = (Movie) data.getSerializableExtra(DetailsActivity.MOVIE);
                    Log.d(TAG, "onActivityResult: REQUEST_CODE_EXOPLAYER: " + resultMovie);
                    updateItemFromActivityResult(resultMovie);

                    Intent exoIntent = new Intent(getActivity(), ExoplayerMediaPlayer.class);
                    exoIntent.putExtra(DetailsActivity.MOVIE, (Serializable) resultMovie);
                    exoIntent.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) (resultMovie != null ? resultMovie.getMainMovie() : null));
                    //intent.putExtra(DetailsActivity.MOVIE, (Serializable) res.getSubList().get(0));
                    Objects.requireNonNull(getActivity()).startActivity(exoIntent);
                    dbHelper.addMainMovieToHistory(mSelectedMovie);

                } else if (requestCode == Movie.REQUEST_CODE_EXTERNAL_PLAYER) {
                    Movie resultMovie = (Movie) data.getSerializableExtra(DetailsActivity.MOVIE);
                    Log.d(TAG, "onActivityResult: REQUEST_CODE_EXTERNAL_PLAYER: " + resultMovie);
                    updateItemFromActivityResult(resultMovie);

                    String type = "video/*";
                    Uri uri = Uri.parse(resultMovie.getVideoUrl());
                    Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
                    in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //  in1.setPackage("org.videolan.vlc");
                    in1.setDataAndType(uri, type);
                    getActivity().startActivity(in1);
                    dbHelper.addMainMovieToHistory(mSelectedMovie);

                } else if (requestCode == Movie.REQUEST_CODE_MOVIE_LIST) {
                    //list of movies
                    String result = data.getStringExtra("result");
                    Log.d(TAG, "onActivityResult: resultString:" + result);
                    if (result != null) {
                        Type movieListType = new TypeToken<List<Movie>>() {
                        }.getType();
                        List<Movie> movies = gson.fromJson(result, movieListType);
                        if (movies != null) {
                            if (mSelectedMovie.getState() == Movie.RESULT_STATE) {
                                for (Movie mov : movies) {
                                    if (server.isSeries(mov)) {
                                        movies.get(movies.indexOf(mov)).setState(Movie.GROUP_OF_GROUP_STATE);
                                    } else {
                                        movies.get(movies.indexOf(mov)).setState(Movie.ITEM_STATE);
                                    }
                                }
                            }
                            Log.d(TAG, "onActivityResult: movie before:" + mSelectedMovie.getSubList());

                            if (mSelectedMovie.getSubList() == null){
                                mSelectedMovie.setSubList(new ArrayList<>());
                            }
                            mSelectedMovie.setSubList(movies);

//                            mSelectedMovie = Movie.clone(mSelectedMovie);
//                            mSelectedMovie.setSubList(movies);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (Movie movie: movies) {
                                        Log.d(TAG, "run: movie: "+movie);
                                        listRowAdapter.add(movie);
                                    }
                                    mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(row), mAdapter.size());
                                    mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(listRowAdapter), mAdapter.size());

                                    if (movies.size() > 0 && movies.get(0) != null){
                                        int state = movies.get(0).getState();
                                        if (state == Movie.RESOLUTION_STATE || state == Movie.VIDEO_STATE){
                                            loadActionRow();
                                        }
                                    }
                                }
                            });

//                            Movie mm = new Movie();
//                            mm.setStudio(mSelectedMovie.getStudio());
//                            mm.setState(Movie.GROUP_OF_GROUP_STATE);
//                            mm.setDescription(mSelectedMovie.getDescription());
//                            Intent intent = new Intent(getActivity(), DetailsActivity.class);
//                            if (movies != null){
//                                //Convert the movie object to a JSON string
//                                String movieJson = gson.toJson(movies);
//                                intent.putExtra(DetailsActivity.MOVIE_SUBLIST, (Serializable) movieJson);
//                            }
//                            intent.putExtra(getResources().getString(R.string.movie), (Serializable) (Movie) mm);
//                            getActivity().startActivity(intent);


                            Log.d(TAG, "onActivityResult: listRowAdapter:" + listRowAdapter.size());
                            Log.d(TAG, "onActivityResult: movies:" + mSelectedMovie.getSubList().toString());
                        }

                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
        // Objects.requireNonNull(getView()).requestFocus();
    }

    private void updateItemFromActivityResult(Movie resultMovie) {
        if (resultMovie != null && resultMovie.getVideoUrl() != null) {
            //find the correct clicked movie to be updated
            Movie targetMovie = mSelectedMovie.getSubList().get(resultMovie.getRowIndex());
            resultMovie.setTitle(mSelectedMovie.getTitle() + " | " + targetMovie.getTitle());
            targetMovie = resultMovie;

            mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(row), mAdapter.size());
            mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(listRowAdapter), mAdapter.size());
            listRowAdapter.replace(resultMovie.getRowIndex(), resultMovie);
        }
    }

    private void generateMovieHistory(Movie movie, Movie subMovie) {
        MovieHistory history = new MovieHistory();
//        String mainMovieUrl = movie.getVideoUrl();
//        String title = movie.getTitle();
//        Movie mainMovie = movie.getMainMovie();
//        Log.d(TAG, "generateMovieHistory: main0:"+movie.getTitle());
//        switch (movie.getState()){
//            case Movie.GROUP_OF_GROUP_STATE:
//                mainMovieUrl = movie.getVideoUrl();
//            case Movie.GROUP_STATE:
//                mainMovie = movie.getMainMovie();
//                if (mainMovie != null){
//                    mainMovieUrl = mainMovie.getVideoUrl();
//                    title = title + " | "+ mainMovie.getTitle();
//                }
//
//                mainMovieUrl = (mainMovie != null) ? mainMovie.getMainMovie() : mainMovie.getVideoUrl();
//            case Movie.GROUP_OF_GROUP_STATE:
//                mainMovie = movie.getVideoUrl();
//        }
//        if(mainMovie != null ) {
//            Log.d(TAG, "generateMovieHistory: main1:"+mainMovie.getTitle());
//            mainMovieUrl = mainMovie.getVideoUrl();
//            title = mainMovie.getTitle();
//            Movie mainMovie2 = mainMovie.getMainMovie();
//            if(mainMovie2 != null ) {
//                Log.d(TAG, "generateMovieHistory: main2:"+mainMovie2.getTitle());
//                mainMovieUrl = mainMovie2.getVideoUrl();
//                title = title + " | " + mainMovie2.getTitle();
//                Movie mainMovie3 = mainMovie2.getMainMovie();
//                if(mainMovie3.getMainMovie() != null ) {
//                    Log.d(TAG, "generateMovieHistory: main3:"+mainMovie3.getTitle());
//                    mainMovieUrl = mainMovie3.getVideoUrl();
//                    title = title + " | " + mainMovie3.getTitle();
//                }
//            }
//        }

        if (movie.getState() > Movie.ITEM_STATE){
            return;
        }
        Movie mainMovie = movie.getMainMovie();

        if (movie.getState() == Movie.GROUP_STATE){
            history.setSeason(movie.getTitle());
        }

        if (movie.getState() == Movie.ITEM_STATE){
            history.setEpisode(movie.getTitle());
        }

        if (subMovie.getState() == Movie.GROUP_STATE){
            history.setSeason(subMovie.getTitle());
        }

        if (subMovie.getState() == Movie.ITEM_STATE){
            history.setEpisode(subMovie.getTitle());
        }

//        history.setSeason(subMovie.getTitle() + " | " + mainMovie.getTitle());
//        history.setEpisode(removeDomain(movie.getVideoUrl()));
        history.setMainMovieUrl(Util.getUrlPathOnly(mainMovie.getVideoUrl()));
        Log.d(TAG, "generateMovieHistory: "+movie.getMainMovie());
//        subMovie.setMovieHistory(history);
        dbHelper.saveMovieHistory(history);
    }



    // Show the ProgressDialog
    private void showProgressDialog(boolean uiThread) {
        if (uiThread) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        mProgressDialog.show();
                    }
                }
            });
        } else {
            if (mProgressDialog != null) {
                mProgressDialog.show();
            }
        }
    }

    // Hide the ProgressDialog
    private void hideProgressDialog(boolean uiThread) {
        if (uiThread) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                }
            });
        } else {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }

    }
}