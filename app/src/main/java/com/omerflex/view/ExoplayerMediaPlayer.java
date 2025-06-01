package com.omerflex.view;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.omerflex.OmerFlexApplication;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.database.DatabaseManager;
import com.omerflex.service.logging.ErrorHandler;
import com.omerflex.service.logging.Logger;
import com.omerflex.service.network.HttpClientManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.leanback.LeanbackPlayerAdapter;

import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.service.database.MovieDbHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@androidx.media3.common.util.UnstableApi
public class ExoplayerMediaPlayer extends AppCompatActivity {

    private ExoPlayer player;
    private static final String TAG = "ExoplayerMediaPlayer";

    private MovieDbHelper dbHelper;
    private PlayerView playerView;
    private long backPressedTime;
    private Movie movie;

    // Managers from OmerFlexApplication
    private ThreadPoolManager threadPoolManager;
    private HttpClientManager httpClientManager;
    private DatabaseManager databaseManager;

    // Player configuration
    public static final long MAX_SEEK_DURATION_MS = 60000; // 60 seconds
    private static final long MAX_VIDEO_DURATION = 7200000; // 2 hours in ms
    public static final long SEEK_DURATION_MS = 15000; // 15 seconds
    public static final int CONNECTION_TIMEOUT = 60000; // 60 seconds
    private static final float MIN_SEEK_DISTANCE = 100; // 100 pixels
    private float initialTouchX;
    private long lastSeekTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "onCreate: Starting ExoplayerMediaPlayer");

        // Initialize managers from OmerFlexApplication
        OmerFlexApplication app = OmerFlexApplication.getInstance();
        threadPoolManager = app.getThreadPoolManager();
        httpClientManager = app.getHttpClientManager();
        databaseManager = app.getDatabaseManager();

        // Set up full screen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        try {
            getSupportActionBar().hide();
            setContentView(R.layout.activity_exoplayer);
            playerView = findViewById(R.id.player_view);

            Logger.d(TAG, "onCreate: Setting up player view");

            // Use DatabaseManager from OmerFlexApplication
            dbHelper = databaseManager.getDbHelper();

            // Get movie from intent
            movie = com.omerflex.server.Util.recieveSelectedMovie(getIntent());
            if (movie == null) {
                ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                        "No movie data found in intent", null);
                finish();
                return;
            }

            Logger.d(TAG, "onCreate: Preparing to play movie: " + movie.getTitle());

            // Initialize player with optimized buffer settings
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * 2,
                            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 2,
                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                    .build();

            player = new ExoPlayer.Builder(getApplicationContext())
                    .setLoadControl(loadControl)
                    .build();

            // Set up audio focus for better playback
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(), true);

            // Initialize leanback player adapter if needed
            LeanbackPlayerAdapter leanbackPlayerAdapter =
                    new LeanbackPlayerAdapter(this.getApplicationContext(), player, 16);

            // Configure SSL to accept all certificates (security consideration)
            setupSSL();

            // Set up player view
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            playerView.setPlayer(player);
            playerView.setKeepScreenOn(true);
            playerView.setControllerAutoShow(false);

            // Prepare and play the media
            MediaSource mediaSource = buildMediaSource(movie);
            player.prepare(mediaSource);
            Logger.d(TAG, "onCreate: Player prepared with media source");
            player.play();

            // Set up touch listener for seek gestures
            setupTouchListener();

            // Add listeners for player events
            setupPlayerListeners();

            // Set up back button handling
            setupBackButtonHandling();

        } catch (Exception e) {
            ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR,
                    "Error initializing ExoplayerMediaPlayer", e);
        }
    }

    private void setupSSL() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Trust all clients
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Trust all servers
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            Logger.e(TAG, "Error setting up SSL", e);
        }
    }

    private void setupTouchListener() {
        playerView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getX();
                    lastSeekTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getX() - initialTouchX;
                    // Only seek if we've moved far enough and enough time has passed since last seek
                    if (Math.abs(deltaX) > MIN_SEEK_DISTANCE &&
                            (System.currentTimeMillis() - lastSeekTime) > 500) {

                        long seekAmount = (long) (deltaX > 0 ? SEEK_DURATION_MS : -SEEK_DURATION_MS);
                        long newPosition = player.getCurrentPosition() + seekAmount;

                        // Constrain position to valid range
                        newPosition = Math.max(0, Math.min(newPosition, player.getDuration()));

                        player.seekTo(newPosition);
                        initialTouchX = event.getX(); // Reset for next move
                        lastSeekTime = System.currentTimeMillis();

                        // Show a toast with the seek information
                        String direction = seekAmount > 0 ? "forward" : "backward";
                        Toast.makeText(this, "Seeking " + direction + " " +
                                        Math.abs(seekAmount / 1000) + " seconds",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    // Show/hide controller on tap
                    if (Math.abs(event.getX() - initialTouchX) < MIN_SEEK_DISTANCE) {
                        if (!playerView.isControllerFullyVisible()) {
                            playerView.showController();
                        } else {
                            playerView.hideController();
                        }
                    }
                    return true;
            }
            return false;
        });
    }

    private void setupPlayerListeners() {
        // Listen for audio sink errors
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onAudioSinkError(EventTime eventTime, Exception audioSinkError) {
                Logger.e(TAG, "Audio sink error", audioSinkError);
                try {
                    // Try to rebuild and replay
                    MediaSource mediaSource = buildMediaSource(movie);
                    player.prepare(mediaSource);
                    player.play();
                } catch (Exception e) {
                    ErrorHandler.handleError(ExoplayerMediaPlayer.this,
                            ErrorHandler.PLAYBACK_ERROR,
                            "Error rebuilding media source after audio sink error", e);
                }
            }
        });

        // Listen for player errors and state changes
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Logger.e(TAG, "Player error: " + error.getMessage() +
                        ", code: " + error.errorCode, error);

                int errorCode = error.errorCode;
                String studio = movie.getStudio();

                // Special handling for IPTV/Omar servers
                if (errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
                    if (studio.equals(Movie.SERVER_OMAR) || studio.equals(Movie.SERVER_IPTV)) {
                        try {
                            MediaSource mediaSource = buildMediaSource(movie);
                            player.prepare(mediaSource);
                            player.play();
                            return;
                        } catch (Exception e) {
                            ErrorHandler.handleError(ExoplayerMediaPlayer.this,
                                    ErrorHandler.PLAYBACK_ERROR,
                                    "Error rebuilding media after parse error", e);
                        }
                    }
                }

                // Check if we should delete the movie from database due to playback issues
                boolean shouldDelete = errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                        || errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                        || errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        || errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                        || errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED;

                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                    shouldDelete = error.getCause() != null &&
                            error.getCause().getMessage() != null &&
                            error.getCause().getMessage().contains("verified");
                }

                if (shouldDelete) {
                    Logger.i(TAG, "Deleting movie due to playback error: " + movie.getTitle());
                    try {
                        dbHelper.deleteMovie(movie);
                    } catch (Exception e) {
                        ErrorHandler.handleError(ExoplayerMediaPlayer.this,
                                ErrorHandler.DATABASE_ERROR,
                                "Error deleting movie after playback failure", e);
                    }
                }

                Toast.makeText(ExoplayerMediaPlayer.this,
                        "Failed to play video", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Logger.d(TAG, "Playback state changed: " + playbackState);

                String studio = movie.getStudio();
                if (!studio.equals(Movie.SERVER_IPTV) && !studio.equals(Movie.SERVER_OMAR)) {
                    return;
                }

                // For certain servers, automatically restart playback when it ends
                if (playbackState == Player.STATE_ENDED) {
                    Logger.i(TAG, "Playback ended, restarting for IPTV/Omar server");
                    try {
                        MediaSource mediaSource = buildMediaSource(movie);
                        player.prepare(mediaSource);
                        player.play();
                    } catch (Exception e) {
                        ErrorHandler.handleError(ExoplayerMediaPlayer.this,
                                ErrorHandler.PLAYBACK_ERROR,
                                "Error restarting playback after end", e);
                    }
                }
            }
        });
    }

    private void setupBackButtonHandling() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    handleBackPressed();
                } catch (Exception e) {
                    ErrorHandler.handleError(ExoplayerMediaPlayer.this,
                            ErrorHandler.GENERAL_ERROR,
                            "Error handling back button press", e);
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public void handleBackPressed() {
        Logger.d(TAG, "handleBackPressed: Back button pressed");

        try {
            // Check if waiting time between the second click of back button is less than 1.5 seconds
            if (backPressedTime + 1500 > System.currentTimeMillis()) {
                Logger.i(TAG, "handleBackPressed: Second back press detected, finishing activity");

                // Release player resources and finish
                releasePlayerResources();
                finish();
            } else {
                // If controller is visible, hide it; otherwise show exit message
                if (playerView != null && playerView.isControllerFullyVisible()) {
                    Logger.d(TAG, "handleBackPressed: Hiding player controller");
                    playerView.hideController();
                } else {
                    Logger.d(TAG, "handleBackPressed: Showing exit message");
                    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
            }

            // Update timestamp for back press
            backPressedTime = System.currentTimeMillis();
        } catch (Exception e) {
            ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                    "Error handling back button press", e);
            // Ensure we still update the timestamp even if there's an error
            backPressedTime = System.currentTimeMillis();
        }
    }

    private MediaSource buildMediaSource(Movie movie) {
        // Ensure HTTPS for Akwam server
        updateMovieUrlToHttps(movie);

        // Parse URL and headers
        String url = movie.getVideoUrl();
        String[] parts = url.split("\\|", 2);
        String cleanUrl = parts[0];
        Map<String, String> headers = new HashMap<>();

        if (parts.length == 2) {
            headers = com.omerflex.server.Util.extractHeaders(parts[1]);
            Logger.d(TAG, "Headers extracted from URL: " + headers);
        }

        // Create data source factory with headers
        DataSource.Factory dataSourceFactory = createDataSourceFactory(cleanUrl, headers);
        Uri uri = Uri.parse(cleanUrl);

        // Create appropriate media source based on content type
        return createMediaSource(dataSourceFactory, uri, movie);
    }

    private void updateMovieUrlToHttps(Movie movie) {
        if (Objects.equals(movie.getStudio(), Movie.SERVER_OLD_AKWAM) &&
                !movie.getVideoUrl().contains("https")) {
            movie.setVideoUrl(movie.getVideoUrl().replace("http", "https"));
        }
    }

    private DataSource.Factory createDataSourceFactory(String url, Map<String, String> headers) {
        if (headers.isEmpty()) {
            return new DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(CONNECTION_TIMEOUT)
                    .setReadTimeoutMs(CONNECTION_TIMEOUT);
        }

        return () -> {
            DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(CONNECTION_TIMEOUT)
                    .setReadTimeoutMs(CONNECTION_TIMEOUT);

            DataSource dataSource = httpDataSourceFactory.createDataSource();
            setRequestHeaders(dataSource, headers);
            return dataSource;
        };
    }

    private void setRequestHeaders(DataSource dataSource, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            ((HttpDataSource) dataSource).setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private MediaSource createMediaSource(DataSource.Factory dataSourceFactory, Uri uri, Movie movie) {
        // Check for HLS specific case
        if (movie.getVideoUrl().contains("m3u")) {
            return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
        }

        // Determine content type and create appropriate source
        int type = Util.inferContentType(uri);
        switch (type) {
            case C.CONTENT_TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.CONTENT_TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            case C.CONTENT_TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
            default:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!playerView.isControllerFullyVisible()) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER ||
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    playerView.showController();
                    return true;
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    player.seekTo(Math.min(player.getCurrentPosition() + SEEK_DURATION_MS,
                            player.getDuration()));
                    return true;
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    player.seekTo(Math.max(0, player.getCurrentPosition() - SEEK_DURATION_MS));
                    return true;
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    if (playerView.isControllerFullyVisible()) {
                        playerView.hideController();
                        return true;
                    }
                }
            }
        }

        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    private void releasePlayerResources() {
        if (player != null) {
            try {
                player.stop();
                player.release();
                player = null;
            } catch (Exception e) {
                ErrorHandler.handleError(this, ErrorHandler.PLAYBACK_ERROR,
                        "Error releasing player resources", e);
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        Logger.d(TAG, "onUserLeaveHint: User leaving app");
        try {
            super.onUserLeaveHint();

            if (player != null) {
                player.pause();
            }

            if (playerView != null) {
                playerView.setKeepScreenOn(false);
            }
        } catch (Exception e) {
            ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR,
                    "Error handling user leave hint", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy called");

        if (isFinishing()) {
            releasePlayerResources();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.d(TAG, "onStop called");

        if (player != null) {
            try {
                long playtime = player.getCurrentPosition();
                Logger.d(TAG, "Saving play time: " + (playtime / 60000) + " minutes");

                movie.setPlayedTime(playtime);
                dbHelper.updateMoviePlayTime(movie, playtime);

                // Pause if not finishing
                if (!isFinishing()) {
                    player.pause();
                    playerView.setKeepScreenOn(false);
                }
            } catch (Exception e) {
                ErrorHandler.handleError(this, ErrorHandler.DATABASE_ERROR,
                        "Error saving playback position", e);
            }
        }
    }

    @Override
    protected void onResume() {
        Logger.d(TAG, "onResume called");
        try {
            super.onResume();

            if (playerView != null) {
                playerView.setKeepScreenOn(true);
            }

            if (player != null && !player.isPlaying()) {
                player.play();
            }
        } catch (Exception e) {
            ErrorHandler.handleError(this, ErrorHandler.GENERAL_ERROR, 
                    "Error resuming activity", e);
        }
    }
}