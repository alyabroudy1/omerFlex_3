// MainFragment.java
package com.omerflex.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;

import com.omerflex.R;
import com.omerflex.view.viewConroller.MainFragmentController;
import com.omerflex.viewmodel.SharedViewModel;

import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;

/**
 * The MainFragment is a lean view that sets up the UI and delegates
 * all business and UI logic to its controller.
 */
public class MainFragment extends BrowseSupportFragment {
    private static final String TAG = "MainFragment";

    private ArrayObjectAdapter mRowsAdapter;
    private MainFragmentController mController;
    private SharedViewModel sharedViewModel;


    // todo think about initialize and re initialize the activity

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        setupUIElements();
        prepareController();
        setupEventListeners();

        // The controller is now responsible for loading data.
        mController.loadData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mController != null) {
            // Delegate the activity result to the controller.
            mController.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mController != null) {
            // Delegate the activity result to the controller.
            mController.handleOnRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mController != null) {
            // Delegate the activity result to the controller.
            mController.handleOnDestroy();
        }
    }

    private void prepareController() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        Drawable defaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.default_background);
        mController = new MainFragmentController(this, mRowsAdapter, defaultBackground);
        setAdapter(mRowsAdapter);
    }

    private void setupUIElements() {
        setTitle(getString(R.string.browse_title));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        // The fragment only handles simple UI events, delegating complex logic.
        setOnSearchClickedListener(view ->
                Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG).show()
        );
    }
}
