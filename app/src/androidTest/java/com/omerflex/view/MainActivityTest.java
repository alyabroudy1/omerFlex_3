package com.omerflex.view;

import android.content.Intent;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.omerflex.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private ActivityScenario<MainActivity> activityScenario;

    @Before
    public void setUp() {
        // Launch the activity
        activityScenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        // Close the activity
        if (activityScenario != null) {
            activityScenario.close();
        }
    }

    @Test
    public void testMainActivityLaunch() {
        // Verify that the activity is launched successfully
        activityScenario.onActivity(activity -> {
            assertNotNull(activity);
            assertEquals(MainActivity.class, activity.getClass());
        });
    }

    @Test
    public void testMainFragmentIsDisplayed() {
        // Verify that the main fragment is displayed
        onView(withId(R.id.main_browse_fragment)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonBehavior() {
        // Test the back button behavior
        activityScenario.onActivity(activity -> {
            // First press should show toast
            activity.onBackPressed();
            
            // Wait for toast to appear
            SystemClock.sleep(1000);
            
            // Second press within 2 seconds should finish the activity
            activity.onBackPressed();
            
            // Verify that the activity is finishing
            assertEquals(true, activity.isFinishing());
        });
    }

    @Test
    public void testSavedInstanceState() {
        // Test that the activity handles configuration changes correctly
        activityScenario.onActivity(activity -> {
            // Get the current fragment
            MainFragment fragment = (MainFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.main_browse_fragment);
            assertNotNull(fragment);
        });
        
        // Recreate the activity (simulates configuration change)
        activityScenario.recreate();
        
        // Verify that the fragment is still there after recreation
        activityScenario.onActivity(activity -> {
            MainFragment fragment = (MainFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.main_browse_fragment);
            assertNotNull(fragment);
        });
    }
}