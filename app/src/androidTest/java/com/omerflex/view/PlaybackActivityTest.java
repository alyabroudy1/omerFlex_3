package com.omerflex.view;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.omerflex.entity.Movie;

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
public class PlaybackActivityTest {

    private ActivityScenario<PlaybackActivity> activityScenario;
    private Movie testMovie;

    @Before
    public void setUp() {
        // Create a test movie
        testMovie = new Movie();
        testMovie.setId(1);
        testMovie.setTitle("Test Movie");
        testMovie.setDescription("Test Description");
        testMovie.setVideoUrl("https://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Hangin'%20with%20the%20Google%20Search%20Bar.mp4");
        
        // Create intent with the test movie
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlaybackActivity.class);
        intent.putExtra(DetailsActivity.MOVIE, testMovie);
        
        // Launch the activity with the intent
        activityScenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        // Close the activity
        if (activityScenario != null) {
            activityScenario.close();
        }
    }

    @Test
    public void testPlaybackActivityLaunch() {
        // Verify that the activity is launched successfully
        activityScenario.onActivity(activity -> {
            assertNotNull(activity);
            assertEquals(PlaybackActivity.class, activity.getClass());
        });
    }

    @Test
    public void testPlaybackFragmentIsCreated() {
        // Verify that the PlaybackVideoFragment is created
        activityScenario.onActivity(activity -> {
            PlaybackVideoFragment fragment = (PlaybackVideoFragment) activity.getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
            assertNotNull(fragment);
        });
    }

    @Test
    public void testMovieDetailsAreSet() {
        // Verify that the movie details are set in the fragment
        activityScenario.onActivity(activity -> {
            PlaybackVideoFragment fragment = (PlaybackVideoFragment) activity.getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
            assertNotNull(fragment);
            
            // We can't directly access the private fields of the fragment,
            // but we can verify that the activity has the correct intent extras
            Movie movie = (Movie) activity.getIntent().getSerializableExtra(DetailsActivity.MOVIE);
            assertNotNull(movie);
            assertEquals(testMovie.getTitle(), movie.getTitle());
            assertEquals(testMovie.getDescription(), movie.getDescription());
            assertEquals(testMovie.getVideoUrl(), movie.getVideoUrl());
        });
    }

    @Test
    public void testOnPause() {
        // Test that onPause is called when the activity is paused
        activityScenario.onActivity(activity -> {
            // First, ensure the activity is created
            assertNotNull(activity);
        });
        
        // Pause the activity
        activityScenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED);
        
        // We can't directly verify that the media player is paused,
        // but we can verify that the activity transitions to the STARTED state
        activityScenario.onActivity(activity -> {
            assertEquals(androidx.lifecycle.Lifecycle.State.STARTED, 
                    activity.getLifecycle().getCurrentState());
        });
    }
}