package com.omerflex.view;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;

import com.omerflex.service.PlayerManager;

public class PlayerUiController {

    private final AppCompatActivity activity;
    private final PlayerView playerView;
    private final PlayerManager playerManager;

    private long backPressedTime;
    private float initialX = 0;

    private static final long SEEK_DURATION_MS = 1000;
    private static final float MIN_SEEK_DISTANCE = 100;

    public PlayerUiController(AppCompatActivity activity, PlayerView playerView, PlayerManager playerManager) {
        this.activity = activity;
        this.playerView = playerView;
        this.playerManager = playerManager;
    }

    public View.OnTouchListener getOnTouchListener() {
        return (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getX();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    handleTouchMove(event);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!playerView.isControllerFullyVisible()) {
                        playerView.showController();
                    }
                    return true;
            }
            return false;
        };
    }

    private void handleTouchMove(MotionEvent event) {
        Player player = playerManager.getCurrentPlayer();
        if (player == null) return;

        float deltaX = event.getX() - initialX;
        if (Math.abs(deltaX) > MIN_SEEK_DISTANCE) {
            if (deltaX > 0) {
                player.seekTo(player.getCurrentPosition() + SEEK_DURATION_MS);
            } else {
                player.seekTo(player.getCurrentPosition() - SEEK_DURATION_MS);
            }
            initialX = event.getX();
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (playerView.dispatchKeyEvent(event)) {
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Player player = playerManager.getCurrentPlayer();
            if (player == null) return false;

            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    player.seekTo(player.getCurrentPosition() + 15000);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    player.seekTo(player.getCurrentPosition() - 15000);
                    return true;
            }
        }
        return false;
    }

    public void handleBackPressed() {
        if (playerView.isControllerFullyVisible()) {
            playerView.hideController();
        } else {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                activity.finish();
            } else {
                Toast.makeText(activity, "Press back again to exit", Toast.LENGTH_SHORT).show();
            }
            backPressedTime = System.currentTimeMillis();
        }
    }
}
