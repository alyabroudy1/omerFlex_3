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
import com.omerflex.view.viewConroller.MainFragmentController;
import com.omerflex.viewmodel.SharedViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.webkit.internal.ApiFeature;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

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


//
//        try {
//
//                    Movie movie = new Movie();
//        movie.setState(Movie.BROWSER_STATE);
//        movie.setStudio(Movie.SERVER_CimaNow);
//            String url = " https://cimanow.cc/%d9%85%d8%b3%d9%84%d8%b3%d9%84-dexter-resurrection-%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-10-%d8%a7%d9%84%d8%b9%d8%a7%d8%b4%d8%b1%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85%d8%a9/watching/";
//            url = "https://rm.freex2line.online/loadon/?link=aHR0cHM6Ly9jaW1hbm93LmNjLyVkOSU4NSVkOCViMyVkOSU4NCVkOCViMyVkOSU4NC1kZXh0ZXItcmVzdXJyZWN0aW9uLSVkOCVhNyVkOSU4NCVkOCVhZCVkOSU4NCVkOSU4MiVkOCVhOS0xMC0lZDglYTclZDklODQlZDglYjklZDglYTclZDglYjQlZDglYjElZDglYTktJWQ5JTg1JWQ4JWFhJWQ4JWIxJWQ4JWFjJWQ5JTg1JWQ4JWE5L3dhdGNoaW5nLw==";
//            String referer = "https://www.freex2line.online/2020/02/blog-post.html/";
////         referer = "https://cimanow.cc/";
//        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
//         userAgent = "Android 6";
//        String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";
//        String acceptEncoding = "zip, deflate, br, zstd";
//        String acceptLanguage = "en-US,en;q=0.9";
//        String priority = "u=0";
////        String secChUa = "Not(A:Brand\";v=\"99\", \"Android WebView\";v=\"133\", \"Chromium\";v=\"133\"";
//        String secChUaMobile = "?1";
//        String secChUaPlatform = "Android";
//        String secFetchDest = "document";
//        String secFetchMode = "navigate";
//        String secFetchSite = "cross-site";
//        String secFetchUser = "?1";
//        String secGpc = "1";
//        String upgradeInsecureRequests = "1";
//
//
//
//        movie.setVideoUrl(url + "|Referer=" + referer + "&User-Agent=" + userAgent + "&Accept=" + accept + "&Accept-Encoding=" + acceptEncoding + "&Accept-Language=" + acceptLanguage + "&priority=" + priority + "&sec-ch-ua-mobile=" + secChUaMobile + "&sec-ch-ua-platform=" + secChUaPlatform + "&sec-fetch-dest=" + secFetchDest + "&sec-fetch-mode=" + secFetchMode + "&sec-fetch-site=" + secFetchSite + "&sec-fetch-user=" + secFetchUser + "&sec-gpc=" + secGpc + "&upgrade-insecure-requests=" + upgradeInsecureRequests);
//        Util.openBrowserIntent(movie, getActivity(), false, false,false);
            // Make a request and keep the response
//            Connection.Response response = Jsoup.connect(url)
//                    .method(Connection.Method.GET)
//                    .execute();
//
//            // Get the document if needed
//            Document doc = response.parse();
//            Log.d(TAG, "onActivityCreated: doc: "+ doc.title());
//
//            // Extract cookies from the response
//            Map<String, String> cookies = response.cookies();
//String cookieString = "";
//            for (Map.Entry<String, String> entry : cookies.entrySet()) {
//                Log.d("JSOUP", "Cookie: " + entry.getKey() + " = " + entry.getValue());
//                cookieString += entry.getKey() + "=" + entry.getValue() + ";";
//            }
//            CookieManager cookieManager = CookieManager.getInstance();
//            cookieManager.setAcceptCookie(true);
//            cookieManager.setCookie("https://www.freex2line.online/", cookieString);
//            url = " https://cimanow.cc/%d9%85%d8%b3%d9%84%d8%b3%d9%84-dexter-resurrection-%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-10-%d8%a7%d9%84%d8%b9%d8%a7%d8%b4%d8%b1%d8%a9-%d9%85%d8%aa%d8%b1%d8%ac%d9%85%d8%a9/watching/";
//            movie.setVideoUrl(url + "|Referer=" + referer + "&Cookie=" + cookieString);
//            Util.openBrowserIntent(movie, getActivity(), false, false,false);
            // If you want to reuse cookies for another request:
//            Document doc2 = Jsoup.connect(url)
//                    .header("Referer", referer)
//                    .cookies(cookies)
//                    .get();

//            Log.d(TAG, "onActivityCreated: doc2: "+ doc2.outerHtml());
//            Log.d(TAG, "onActivityCreated: doc2: "+ doc2.title());

//        } catch (IOException e) {
//            Log.d(TAG, "onActivityCreated: error:"+ e.getMessage());
//            e.printStackTrace();
//        }


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
