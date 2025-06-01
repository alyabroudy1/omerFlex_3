package com.omerflex.view;

import android.net.Uri;
import android.os.Bundle;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.PlaybackControlsRow;

import com.omerflex.OmerFlexApplication;
import com.omerflex.entity.Movie;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;

/**
 * Handles video playback with media controls.
 */
public class PlaybackVideoFragment extends VideoSupportFragment {
    private static final String TAG = "PlaybackVideoFragment";

    private PlaybackTransportControlGlue<MediaPlayerAdapter> mTransportControlGlue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "onCreate: Initializing playback");

        try {
            // Get the movie from the intent
            final Movie movie = getMovieFromIntent();
            if (movie == null) {
                ErrorHandler.handleError(getContext(), ErrorHandler.GENERAL_ERROR, 
                        "No movie data found in intent", null);
                return;
            }

            Logger.d(TAG, "onCreate: Setting up playback for movie: " + movie.getTitle());

            // Set up the media player
            setupMediaPlayer(movie);
        } catch (Exception e) {
            ErrorHandler.handleError(getContext(), ErrorHandler.PLAYBACK_ERROR, 
                    "Error initializing playback", e);
        }
    }

    private Movie getMovieFromIntent() {
        try {
            if (getActivity() == null || getActivity().getIntent() == null) {
                Logger.e(TAG, "getMovieFromIntent: Activity or intent is null");
                return null;
            }
            return (Movie) getActivity().getIntent().getSerializableExtra(DetailsActivity.MOVIE);
        } catch (Exception e) {
            ErrorHandler.handleError(getContext(), ErrorHandler.GENERAL_ERROR, 
                    "Error retrieving movie from intent", e);
            return null;
        }
    }

    private void setupMediaPlayer(Movie movie) {
        try {
            // Create the glue host
            VideoSupportFragmentGlueHost glueHost = new VideoSupportFragmentGlueHost(this);

            // Create the player adapter
            MediaPlayerAdapter playerAdapter = new MediaPlayerAdapter(getContext());
            playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE);

            // Create the transport control glue
            mTransportControlGlue = new PlaybackTransportControlGlue<>(getContext(), playerAdapter);
            mTransportControlGlue.setHost(glueHost);
            mTransportControlGlue.setTitle(movie.getTitle());
            mTransportControlGlue.setSubtitle(movie.getDescription());

            // Start playback
            mTransportControlGlue.playWhenPrepared();

            // Set the data source
            String videoUrl = movie.getVideoUrl();
            Logger.d(TAG, "setupMediaPlayer: Setting data source: " + videoUrl);
            playerAdapter.setDataSource(Uri.parse(videoUrl));
        } catch (Exception e) {
            ErrorHandler.handleError(getContext(), ErrorHandler.PLAYBACK_ERROR, 
                    "Error setting up media player", e);
        }
    }

    @Override
    public void onPause() {
        Logger.d(TAG, "onPause: Pausing playback");
        try {
            super.onPause();
            if (mTransportControlGlue != null) {
                mTransportControlGlue.pause();
            }
        } catch (Exception e) {
            ErrorHandler.handleError(getContext(), ErrorHandler.PLAYBACK_ERROR, 
                    "Error pausing playback", e);
        }
    }
}
