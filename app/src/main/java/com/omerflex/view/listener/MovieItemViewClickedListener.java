package com.omerflex.view.listener;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.omerflex.OmerFlexApplication;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieFetchProcess;
import com.omerflex.entity.MovieRepository;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.ServerInterface;
import com.omerflex.server.Util;
import com.omerflex.server.config.ServerConfigRepository;
import com.omerflex.view.CardPresenter;
import com.omerflex.view.VideoDetailsFragment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieItemViewClickedListener implements OnItemViewClickedListener{

    private static final String TAG = "MovieClickedListener";

    public static final int ACTION_OPEN_DETAILS_ACTIVITY = 1;
    public static final int ACTION_OPEN_EXTERNAL_ACTIVITY = 2;
    public static final int ACTION_OPEN_NO_ACTIVITY = 3;

    private static final int ACTION_WATCH = 1;
    private static final int ACTION_WATCH_TRAILER = 2;

    private Integer selectedItemIndex = -1;
    private Integer selectedRowIndex = -1;
    Fragment mFragment;
    ArrayObjectAdapter mRowsAdapter;
    private final MovieRepository movieRepository;
    // A single, shared executor service for all background tasks
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public MovieItemViewClickedListener(Fragment fragment, ArrayObjectAdapter mRowsAdapter) {
        this.mFragment = fragment;
        this.mRowsAdapter = mRowsAdapter;
        this.movieRepository = MovieRepository.getInstance(fragment.getActivity(), ((OmerFlexApplication) fragment.getActivity().getApplication()).getDatabase().movieDao());
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (!(item instanceof Movie)) {
            // return to delegate handling
            return;
        }
        Movie movie = (Movie) item;
        if (movie.getStudio() == null) {
            Log.d(TAG, "handleMovieItemClick: Unknown movie or studio, returning.");
            Toast.makeText(mFragment.getActivity(), "Unknown movie or server: " + movie.getStudio(), Toast.LENGTH_SHORT).show();
            return;
        }
        selectedRowIndex = mRowsAdapter.indexOf(row);
        // Submit the heavy lifting to a background thread
        executorService.submit(() -> processMovieClick(movie, (ListRow) row));
    }

    /**
     * Handles the actual click processing logic on a background thread.
     */
    private void processMovieClick(Movie movie, ListRow clickedRow) {
        Log.d(TAG, "processMovieClick: "+ movieRepository.getSelectedMovie().getValue());
        if (movieRepository.getSelectedMovie().getValue() == null){
            // set mSelectedMovie in the repository
            // very important to monitor the selected movie in the repository
            movieRepository.setSelectedMovie(movie);
        }
        ArrayObjectAdapter adapter =(ArrayObjectAdapter) clickedRow.getAdapter();
        selectedItemIndex = adapter.indexOf(movie);
//        Log.d(TAG, "processMovieClick 2: "+ movieRepository.getSelectedMovie().getValue());
        Log.d(TAG, "processMovieClick: state: " + movie.getState());
        Log.d(TAG, "processMovieClick: selectedRowIndex: " + selectedRowIndex);
        Log.d(TAG, "processMovieClick: selectedItemIndex: " + selectedItemIndex);
        // Use a switch statement for cleaner state handling
        switch (movie.getState()) {
            case Movie.COOKIE_STATE:
                // Fetch cookie through browser
                handleCookieState(movie);
                break;
            case Movie.VIDEO_STATE:
                // play video
                handleVideoState(movie);
                break;
            case Movie.BROWSER_STATE:
                // open browser. todo analyze use case
                handleBrowserState(movie);
                break;
            case Movie.NEXT_PAGE_STATE:
                // fetch next page movies
                handleNextPageState(movie, clickedRow);
                break;
            case Movie.IPTV_PLAY_LIST_STATE:
                // fetch iptv categories
                handleIptvPlayListState(movie);
                break;
            default:
                // The default case handles all other states,
                // including opening the details activity or next page states
                handleServerFetchByItemClickCase(movie, clickedRow);
//                Util.openVideoDetailsIntent(movie, mFragment);
                break;
        }
    }

    private void handleIptvPlayListState(Movie movie) {
        Log.d(TAG, "handleIptvPlayListState: list size: "+ mRowsAdapter.size());
        // remove iptv created rows first
        removeIpTvRows();
        // fetch iptv movies
        for (int i = 0; i < 350; i++) {
            movieRepository.getHomepageMovies((category, movieList) -> {
                if (movieList != null) {
                    Log.d("Movie", "Fetched movie33: " + movieList.toString());
                    HeaderItem header = new HeaderItem(Movie.IPTV_PLAY_LIST_STATE, category);
                    addMovieRow(header, movieList);
                } else {
                    Log.d("Movie", "movieList not found.");
                }
            });
        }
    }

    private void removeIpTvRows() {
        // loop over listRows of the mRowsAdapter and delete rows of header id = Movie.IPTV_PLAY_LIST_STATE
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            ListRow row = (ListRow) mRowsAdapter.get(i);
            Log.d(TAG, "removeIpTvRows: " + row.getHeaderItem().getId() + ", " + row.getHeaderItem().getName());
            if (row.getHeaderItem().getId() == Movie.IPTV_PLAY_LIST_STATE) {
                Log.d(TAG, "removeIpTvRows: removing row: " + row.getHeaderItem().getName());
                mRowsAdapter.remove(row);
            }
        }
    }

    public void addMovieRow(HeaderItem header, List<Movie> movies) {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter.addAll(0, movies);
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }


    private void handleCookieState(Movie movie) {
        // to update the row with new result after fetching the cookie, setFetch to REQUEST_CODE_MOVIE_LIST
        movie.setFetch(Movie.REQUEST_CODE_EXTEND_MOVIE_SUB_LIST);
        if (mFragment != null) {
            Util.openBrowserIntent(movie, mFragment, true, true, true, selectedRowIndex, selectedItemIndex);
            return;
        }
        Util.openBrowserIntent(movie, mFragment.getActivity(), true, true, true, selectedRowIndex, selectedItemIndex);
    }

    private void handleVideoState(Movie movie) {
        Log.d(TAG, "handleMovieItemClick: VIDEO_STATE");
        Util.openExoPlayer(movie, mFragment.getActivity(), true);
    }

    private void handleBrowserState(Movie movie) {
        Log.d(TAG, "handleMovieItemClick: BROWSER_STATE");
        Util.openBrowserIntent(movie, mFragment, false, false, false);
    }

    protected void handleServerFetchByItemClickCase(Movie movie, ListRow clickedRow) {
        Log.d(TAG, "handleServerFetchByItemClickCase");
        AbstractServer server = ServerConfigRepository.getInstance().getServer(movie.getStudio());

        int nextAction = server.fetchNextAction(movie);
        switch (nextAction) {
            case VideoDetailsFragment.ACTION_OPEN_DETAILS_ACTIVITY:
                Log.d(TAG, "handleServerFetchByItemClickCase: nextAction: ACTION_OPEN_DETAILS_ACTIVITY");
                Util.openVideoDetailsIntent(movie, mFragment);
                break;
            case VideoDetailsFragment.ACTION_OPEN_EXTERNAL_ACTIVITY:
//                dbHelper.addMainMovieToHistory(movie);  todo
                Log.d(TAG, "handleServerFetchByItemClickCase: nextAction: ACTION_OPEN_EXTERNAL_ACTIVITY");
                Util.openExternalVideoPlayer(movie, mFragment.getActivity());
                break;
            case VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY:
//                dbHelper.addMainMovieToHistory(movie); todo
                Log.d(TAG, "handleServerFetchByItemClickCase: nextAction: ACTION_OPEN_NO_ACTIVITY");
                handleFetchMovieFromServer(movie, clickedRow, server);
                break;
            default:
                Log.d(TAG, "handleServerFetchByItemClickCase: unknown case");
        }

    }

    private void handleFetchMovieFromServer(Movie movie, ListRow clickedRow, AbstractServer server) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            ArrayObjectAdapter adapter =(ArrayObjectAdapter) clickedRow.getAdapter();
            int position = adapter.indexOf(movie);
            MovieFetchProcess process = server.fetch(movie, movie.getState(), new ServerInterface.ActivityCallback<Movie>() {
                @Override
                public void onSuccess(Movie result, String title) {
                    Util.openExternalVideoPlayer(result, mFragment.getActivity());
                    adapter.replace(position, result);
                }

                @Override
                public void onInvalidCookie(Movie result, String title) {
                    Log.d(TAG, "onInvalidCookie: " + result);
                    result.setFetch(Movie.REQUEST_CODE_EXTERNAL_PLAYER);
                    if (mFragment != null) {
                        Util.openBrowserIntent(result, mFragment, true, true, true, (int)clickedRow.getId(), position);
                        return;
                    }
                    Util.openBrowserIntent(result, mFragment.getActivity(), false, true, true,(int)clickedRow.getId(), position);
                }

                @Override
                public void onInvalidLink(Movie result) {

                }

                @Override
                public void onInvalidLink(String message) {

                }
            });
        });

        executor.shutdown();
    }

    /**
     * Refactored method to handle the next page state for a movie click.
     * This method now uses the class's shared ExecutorService and consolidates callback logic.
     * @param movie The movie that was clicked.
     * @param clickedRow The row that contains the clicked movie.
     */
    private void handleNextPageState(Movie movie, ListRow clickedRow) {
        // Use a more robust check to ensure we don't fetch multiple times.
        if (movie.getFetch() == Movie.NO_FETCH_MOVIE_AT_START) {
            Log.d(TAG, "handleNextPageState: This Next page is already fetched...");
            return;
        }

        // Call the MovieRepository to fetch the next page.
        movieRepository.fetchNextPage(movie.getVideoUrl(), (category, movieList) -> {
                if (movieList != null) {
                    Log.d("Movie", "fetchNextPage movie33: " + movieList.toString());
                    updateAdapterOnMainThread(movieList, clickedRow, movie);
                } else {
                    Log.d("Movie", "fetchNextPage movieList not found.");
                }
        });
    }

    /**
     * Helper method to update the adapter on the main thread and set a flag on the movie.
     * This avoids code duplication in the callback methods.
     * @param result The list of new movies to add.
     * @param clickedRow The row adapter to update.
     * @param movie The movie to update with a "clicked" flag.
     */
    private void updateAdapterOnMainThread(List<Movie> result, ListRow clickedRow, Movie movie) {
        if (result.isEmpty()) {
            Log.d(TAG, "onSuccess: empty result");
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            ArrayObjectAdapter adapter = (ArrayObjectAdapter) clickedRow.getAdapter();
            adapter.addAll(adapter.size(), result);
            movie.setFetch(Movie.NO_FETCH_MOVIE_AT_START); // Flag that it's already clicked
        });
    }

}
