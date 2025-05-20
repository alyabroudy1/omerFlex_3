package com.omerflex.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.omerflex.R;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    //variable for back button confirmation
    private long backPressedTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "onCreate: Starting MainActivity");

        try {
            setContentView(R.layout.activity_main);

            if (savedInstanceState == null) {
                Logger.d(TAG, "onCreate: Loading MainFragment");

                try {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_browse_fragment, new MainFragment());
                    transaction.commitNow();
                } catch (Exception e) {
                    // Handle fragment transaction errors
                    ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                            "Failed to load MainFragment", e);
                }
            } else {
                Logger.d(TAG, "onCreate: Activity restored from saved state");
            }
        } catch (Exception e) {
            // Handle any errors during activity creation
            ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                    "Error initializing MainActivity", e);
        }
    }

    @Override
    public void onBackPressed() {
        Logger.d(TAG, "onBackPressed: Back button pressed");

        try {
            //check if waiting time between the second click of back button is less than 2 seconds so we finish the app
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                Logger.i(TAG, "onBackPressed: Second back press detected, finishing activity");
                finish();
            } else {
                Logger.d(TAG, "onBackPressed: First back press, showing exit message");
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            }
            backPressedTime = System.currentTimeMillis();
        } catch (Exception e) {
            // Handle any errors during back press handling
            ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                    "Error handling back button press", e);
            // Ensure we still update the timestamp even if there's an error
            backPressedTime = System.currentTimeMillis();
        }
    }
}
