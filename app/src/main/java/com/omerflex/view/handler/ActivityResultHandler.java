package com.omerflex.view.handler;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRow;
import com.omerflex.entity.Movie;
import com.omerflex.server.Util;
import java.util.ArrayList;

public class ActivityResultHandler {

    private static final String TAG = "ActivityResultHandler";
    private Activity activity;

    public ActivityResultHandler(Activity activity) {
        this.activity = activity;
    }

    public void handleResult(int requestCode, int resultCode, Intent data, ArrayObjectAdapter rowsAdapter) {
        Log.d(TAG, "handleActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.d(TAG, "onActivityResult:RESULT_NOT_OK ");
            return;
        }

        Movie resultMovie = Util.recieveSelectedMovie(data);
        int clickedMovieIndex = data.getIntExtra(Movie.KEY_CLICKED_MOVIE_INDEX, -1);
        int clickedRowId = data.getIntExtra(Movie.KEY_CLICKED_ROW_ID, -1);
        Log.d(TAG, "handleActivityResult: clickedRowId: "+ clickedRowId);
        if (clickedRowId == -1) {
            Log.d(TAG, "handleActivityResult: clickedRowId == -1");
            Toast.makeText(activity, "clickedRowId not defined", Toast.LENGTH_SHORT).show();
            return;
        }
        if (clickedMovieIndex == -1) {
            Log.d(TAG, "handleActivityResult: clickedMovieIndex == -1");
            Toast.makeText(activity, "clickedMovieIndex not defined", Toast.LENGTH_SHORT).show();
            return;
        }
        ListRow clickedRow = (ListRow) rowsAdapter.get(clickedRowId);
        ArrayObjectAdapter clickedAdapter = (ArrayObjectAdapter) clickedRow.getAdapter();

        // we have to updated the clicked movie
        updateClickedMovieItem(clickedAdapter, clickedMovieIndex, resultMovie);

        switch (requestCode) {
            case Movie.REQUEST_CODE_EXOPLAYER:
                Log.d(TAG, "onActivityResult: REQUEST_CODE_EXOPLAYER");
                Util.openExoPlayer(resultMovie, activity, true);
                break;
            case Movie.REQUEST_CODE_EXTERNAL_PLAYER:
                Log.d(TAG, "onActivityResult: REQUEST_CODE_EXTERNAL_PLAYER: " + resultMovie);
                Util.openExoPlayer(resultMovie, activity, true);
                break;
            case Movie.REQUEST_CODE_MOVIE_LIST:
                // todo analyze it's use case
                if (resultMovie != null) {
                    extendClickedRow(clickedAdapter, (ArrayList<Movie>) (resultMovie.getSubList()));
                }
                break;
        }
    }

    private void extendClickedRow(ArrayObjectAdapter clickedAdapter, ArrayList<Movie> movieList) {
        if (movieList == null || movieList.isEmpty()) {
            Log.d(TAG, "extendClickedRow: empty list");
            return;
        }
        clickedAdapter.addAll(clickedAdapter.size(), movieList);
    }

    private void updateClickedMovieItem(ArrayObjectAdapter clickedAdapter, int clickedMovieIndex, Movie resultMovie) {
        if (resultMovie != null) {
            clickedAdapter.replace(clickedMovieIndex, resultMovie);
        }
    }
}