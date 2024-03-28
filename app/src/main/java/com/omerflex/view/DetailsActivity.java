package com.omerflex.view;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.omerflex.R;

/*
 * Details activity class that loads LeanbackDetailsFragment class
 */
public class DetailsActivity extends FragmentActivity {
    public static final String SHARED_ELEMENT_NAME = "hero";
    public static final String MOVIE = "Movie";
    public static final String MAIN_MOVIE = "Main_Movie";
    public static final String MOVIE_SUBLIST = "Movie_sub";
    public static final String DETAILS_FRAGMENT_TAG = "details_fragment_tag";


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.details_fragment, new VideoDetailsFragment())
                    .commitNow();
        }
    }

}