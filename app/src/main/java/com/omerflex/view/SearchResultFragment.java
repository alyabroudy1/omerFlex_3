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
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;

import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.server.Util;
import com.omerflex.service.M3U8ContentFetcher;
import com.omerflex.view.viewConroller.MainFragmentController;
import com.omerflex.viewmodel.SharedViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.webkit.internal.ApiFeature;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * The SearchResultFragment is a lean view that sets up the UI and delegates
 * all business and UI logic to its controller.
 */
public class SearchResultFragment extends BrowseSupportFragment {
    private static final String TAG = "SearchResultFragment";

    private ArrayObjectAdapter mRowsAdapter;
    private MainFragmentController mController;
    private SharedViewModel sharedViewModel;
    private String query;


    // todo think about initialize and re initialize the activity

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        setupUIElements();
        prepareController();
        setupEventListeners();

        query = getActivity().getIntent().getStringExtra("query");
        if (query == null) {
            Log.d(TAG, "loadRows: fail to receive the query");
            return;
        }
        query = query.trim();

        mController.loadSearchData(query);
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
//        setTitle(getString(R.string.browse_title));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        // The fragment only handles simple UI events, delegating complex logic.
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent getQueryIntent = new Intent(getActivity(), GetSearchQueryActivity.class);
                startActivity(getQueryIntent);
            }
        });
    }
}