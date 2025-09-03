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
import com.omerflex.server.Base64Util;
import com.omerflex.service.UpdateService;
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

        String text = "{\n" +
                "  \"output\": \"u003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-30/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 30 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-29/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 29 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-28/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 28 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-27/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 27 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-26/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 26 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-25/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 25 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-24/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 24u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-23/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 23 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-22/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 22 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-21/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 21 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-20/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 20u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-19/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 19 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-18/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 18 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-17/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 17 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-16/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 16 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-15/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 15 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-14/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 14u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-13/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 13u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-12/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 12 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-11/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 11 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-10/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 10 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-9/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 9 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-8/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 8 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-7/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 7 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-6/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 6 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-5/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 5 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-4/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 4 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-3/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 3 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-2/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 2 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca class=\"hoverable activable\" href=\"https://wecima.video/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-%d8%a7%d8%b3%d8%b1-%d8%ad%d9%84%d9%82%d8%a9-1/\"u003Eu003Cdiv class=\"Thumb\"u003Eu003Cspanu003Eu003Ci class=\"fa fa-play\"u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 1u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003E\"\n" +
                "}";
        String text2 = "u003Eu003C/iu003Eu003C/spanu003Eu003C/divu003Eu003CepisodeAreau003Eu003CepisodeTitleu003Eالحلقة 30 u003C/episodeTitleu003Eu003C/episodeAreau003Eu003C/au003Eu003Ca";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "onActivityCreated: "+ Base64Util.decode(text));
            Log.d(TAG, "onActivityCreated2: "+ Base64Util.decode(text2));
        }


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
