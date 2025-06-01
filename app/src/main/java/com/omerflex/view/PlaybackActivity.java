package com.omerflex.view;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.omerflex.OmerFlexApplication;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;

/**
 * Loads {@link PlaybackVideoFragment}.
 */
public class PlaybackActivity extends FragmentActivity {
    private static final String TAG = "PlaybackActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "onCreate: Starting PlaybackActivity");

        try {
            if (savedInstanceState == null) {
                Logger.d(TAG, "onCreate: Loading PlaybackVideoFragment");

                try {
                    FragmentTransaction transaction = getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, new PlaybackVideoFragment());
                    transaction.commit();
                } catch (Exception e) {
                    // Handle fragment transaction errors
                    ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                            "Failed to load PlaybackVideoFragment", e);
                }
            } else {
                Logger.d(TAG, "onCreate: Activity restored from saved state");
            }
        } catch (Exception e) {
            // Handle any errors during activity creation
            ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                    "Error initializing PlaybackActivity", e);
        }
    }
}
