package com.omerflex.view;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.omerflex.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class GetDocActivity extends AppCompatActivity {

    private static final String TAG = "GetDocActivity";
    public static CompletableFuture<String> resultFuture;
    private WebView webView;
    private String url;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());

    @SuppressLint({"SetJavaScriptEnabled", "WebViewApiAvailability"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_doc);

        url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            completeExceptionally(new IllegalArgumentException("URL is missing from intent"));
            return;
        }

        if (resultFuture == null || resultFuture.isDone()) {
            Log.w(TAG, "ResultFuture is not ready. Finishing activity.");
            finish();
            return;
        }

        webView = findViewById(R.id.webView);
        configureWebView();

        Log.d(TAG, "Loading URL in WebView: " + url);
        webView.loadUrl(url);

        timeoutHandler.postDelayed(this::timeout, 90000); // 90-second timeout
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUserAgentString("Android 7");
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setAllowContentAccess(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }

        webView.setWebViewClient(new WebViewClient() {
            private boolean challengeResolved = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onPageFinished for URL: " + url);
                view.evaluateJavascript("document.title", title -> {
                    Log.d(TAG, "Page title: " + title);
                    if (title != null && (title.contains("Just a moment...") || title.contains("Checking your browser"))) {
                        Log.d(TAG, "Challenge page detected. WebView will continue loading...");
                        challengeResolved = false;
                    } else {
                        if (!challengeResolved) {
                            challengeResolved = true;
                            Log.d(TAG, "Challenge resolved or not present. Extracting HTML.");
                            extractHtml(view);
                        }
                    }
                });
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.w(TAG, "SSL Error: " + error.toString() + " Proceeding anyway.");
                handler.proceed(); // Bypass SSL errors
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.w(TAG, "JS Alert suppressed: " + message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Log.w(TAG, "JS Confirm suppressed: " + message);
                result.confirm();
                return true;
            }
        });
    }


    private void extractHtml(WebView view) {
        view.evaluateJavascript("document.documentElement.outerHTML",
                html -> {
                    if (html != null && !html.equals("null") && html.length() > 2) {
                        try {
                            JSONArray jsonArray = new JSONArray("[" + html + "]");
                            String unescapedHtml = jsonArray.getString(0);
                            completeSuccessfully(unescapedHtml);
                        } catch (JSONException e) {
                            completeExceptionally(new Exception("Failed to parse HTML from WebView", e));
                        }
                    } else {
                        completeExceptionally(new Exception("Failed to extract HTML from WebView"));
                    }
                });
    }

    private void timeout() {
        completeExceptionally(new TimeoutException("WebView timed out after 90 seconds"));
    }

    private void completeSuccessfully(String html) {
        if (resultFuture != null && !resultFuture.isDone()) {
            Log.d(TAG, "Completing future successfully.");
            resultFuture.complete(html);
            cleanup();
        }
    }

    private void completeExceptionally(Exception e) {
        if (resultFuture != null && !resultFuture.isDone()) {
            Log.e(TAG, "Completing future exceptionally", e);
            resultFuture.completeExceptionally(e);
            cleanup();
        }
    }

    private void cleanup() {
        timeoutHandler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.destroy();
            webView = null;
        }
        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
    }
}
