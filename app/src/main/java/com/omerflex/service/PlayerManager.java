package com.omerflex.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;

import androidx.annotation.Nullable;

import com.google.android.gms.cast.framework.CastContext;

@androidx.media3.common.util.UnstableApi
public class PlayerManager implements SessionAvailabilityListener {

    private static final String TAG = "PlayerManager";

    private final Context context;
    private final PlayerView playerView;

    private ExoPlayer localPlayer;
    @Nullable private CastPlayer castPlayer;
    private Player currentPlayer;

    public PlayerManager(Context context, @Nullable CastContext castContext, PlayerView playerView) {
        this.context = context;
        this.playerView = playerView;

        // 1. Always create local ExoPlayer
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder().build();
        localPlayer = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build();

        // 2. Only create CastPlayer if context is available
        if (castContext != null) {
            castPlayer = new CastPlayer(castContext);
            castPlayer.setSessionAvailabilityListener(this);
            castPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "CastPlayer error: " + error.getMessage(), error);
                    Toast.makeText(context, "Cast playback failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        // 3. Set initial player (defaults to local player)
        if (castPlayer != null && castPlayer.isCastSessionAvailable()) {
            this.currentPlayer = castPlayer;
        } else {
            this.currentPlayer = localPlayer;
        }
        playerView.setPlayer(this.currentPlayer);
    }

    public ExoPlayer getLocalPlayer() {
        return localPlayer;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void prepare(MediaSource mediaSource) {
        if (currentPlayer instanceof ExoPlayer) {
            ((ExoPlayer) currentPlayer).setMediaSource(mediaSource);
        } else if (currentPlayer != null) {
            currentPlayer.setMediaItem(mediaSource.getMediaItem());
        }
        if (currentPlayer != null) {
            currentPlayer.prepare();
        }
    }

    private void switchPlayer() {
        // Cannot switch if cast player was never created
        if (castPlayer == null) return;

        Player newPlayer = castPlayer.isCastSessionAvailable() ? castPlayer : localPlayer;
        if (currentPlayer == newPlayer) {
            return; // No switch needed
        }

        long playbackPositionMs = C.TIME_UNSET;
        boolean playWhenReady = false;
        MediaItem currentMediaItem = null;

        if (currentPlayer != null) {
            playbackPositionMs = currentPlayer.getCurrentPosition();
            playWhenReady = currentPlayer.getPlayWhenReady();
            currentMediaItem = currentPlayer.getCurrentMediaItem();
            currentPlayer.stop();
        }

        currentPlayer = newPlayer;
        playerView.setPlayer(currentPlayer);

        Log.d(TAG, "Switched to " + (currentPlayer == castPlayer ? "CastPlayer" : "LocalPlayer"));

        if (currentMediaItem != null) {
            currentPlayer.setMediaItem(currentMediaItem, playbackPositionMs);
        }
        currentPlayer.setPlayWhenReady(playWhenReady);
        currentPlayer.prepare();
    }

    @Override
    public void onCastSessionAvailable() {
        if (castPlayer == null) return;
        Log.d(TAG, "Cast session is available");
        switchPlayer();
    }

    @Override
    public void onCastSessionUnavailable() {
        if (castPlayer == null) return;
        Log.d(TAG, "Cast session is unavailable");
        switchPlayer();
    }

    public void release() {
        if (localPlayer != null) {
            localPlayer.release();
            localPlayer = null;
        }
        if (castPlayer != null) {
            castPlayer.setSessionAvailabilityListener(null);
            castPlayer.release();
            castPlayer = null;
        }
        currentPlayer = null;
    }
}
