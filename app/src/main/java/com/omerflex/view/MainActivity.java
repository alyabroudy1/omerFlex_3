package com.omerflex.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.omerflex.R;


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
        Log.d(TAG, "onCreate: Starting MainActivity");

        try {
            setContentView(R.layout.activity_main);

            if (savedInstanceState == null) {
                Log.d(TAG, "onCreate: Loading MainFragment");

                try {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_browse_fragment, new MainFragment());
                    transaction.commitNow();
                } catch (Exception e) {
                    // Handle fragment transaction errors
                    Log.d(TAG, "onCreate: Error loading MainFragment", e);
                }
            } else {
                Log.d(TAG, "onCreate: Activity restored from saved state");
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //check if waiting time between the second click of back button is greater less than 2 seconds so we finish the app
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finish();
        } else {
            Toast.makeText(this, "Press back 2 time to exit", Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}