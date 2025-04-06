package com.omerflex.view;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

import com.omerflex.R;
import com.omerflex.entity.Movie;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {
    private static final int MAX_BODY_LINES = 3; // Set your desired line count
    String TAG = "DetailsDescriptionPresenter";

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;

        if (movie != null) {
            viewHolder.getTitle().setText(movie.getTitle());
            viewHolder.getSubtitle().setText(movie.getStudio());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                viewHolder.getBody().setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Large);
            }

            String history = "";
            if (movie.getMovieHistory() != null &&
                    (
                            movie.getState() == Movie.GROUP_OF_GROUP_STATE ||
                                    movie.getState() == Movie.GROUP_STATE
                    )
            ) {
                history = ((movie.getMovieHistory().getEpisode() != null) ?
                        (" | " + movie.getMovieHistory().getEpisode()) : "") +
                        ((movie.getMovieHistory().getSeason() != null) ? (" | " + movie.getMovieHistory().getSeason()) : "");
            }
//            Log.d(TAG, "onBindDescription: history: "+history);

            // Configure body text view
            TextView body = viewHolder.getBody();
            body.setMaxLines(MAX_BODY_LINES);
            body.setLines(MAX_BODY_LINES);
            body.setEllipsize(TextUtils.TruncateAt.END);
            body.setText(movie.getDescription() + history);
            body.setHeight(1); // Set fixed height (80dp example)

            viewHolder.getSubtitle().setHeight(2);
            viewHolder.getSubtitle().setLines(2);
        }
    }
}