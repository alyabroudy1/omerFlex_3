package com.omerflex.view;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
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
import com.omerflex.db.AppDatabase;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieHistory;
import com.omerflex.entity.MovieRepository;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@androidx.media3.common.util.UnstableApi
public class ExoplayerMediaPlayer extends AppCompatActivity {

    @Nullable private static ExoPlayer player;
    static String TAG = "Exoplayer";

    MovieRepository movieRepository;
    PlayerView playerView;
    private long backPressedTime;
    Movie movie;

    // url of video which we are loading.

    LeanbackPlayerAdapter leanbackPlayerAdapter;

    //ontouch
    public long MAX_SEEK_DURATION_MS = 2000; // 20 seconds
    private static final long MAX_VIDEO_DURATION = 7200; // 2 hours
    public long videoDuration; // The duration of the video in milliseconds
    public static long SEEK_DURATION_MS = 1000;
    public static long CONNECTION_TIMEOUT = 5000;
    private static final float MIN_SEEK_DISTANCE = 100; //  pixels
    public static float initialX = 0;
    private Map<String, String> headers;
    private boolean hasSeekedToWatchedPosition = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title
        // Hide the status bar.
        // Hide status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getSupportActionBar().hide();
        setContentView(R.layout.activity_exoplayer);
        //  setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.player_view);


//        player = new ExoPlayer.Builder(getApplicationContext()).build();
       // player.setPlayWhenReady(true);
        //leanbackPlayerAdapter = new LeanbackPlayerAdapter(this, player, 0);
        AppDatabase db = AppDatabase.getDatabase(this);
        movieRepository = MovieRepository.getInstance(this, db.movieDao());
        movie = com.omerflex.server.Util.recieveSelectedMovie(getIntent());

        leanbackPlayerAdapter = new LeanbackPlayerAdapter(this.getApplicationContext(), player, 16);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
//                if (webView.canGoBack()) {
//                    webView.stopLoading(); // Stop loading the page
//                    webView.goBack();      // Navigate back in WebView's history
//                } else {
//                    // If there's no history, close the activity
//                    finish();
//                }
                handleBackPressed();
            }
        };

        // Add the callback to the back pressed dispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);


        // increase audio buffer
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .build();


        player = new ExoPlayer.Builder(getApplicationContext())
                .setLoadControl(loadControl)
                .build();


        // give audio focus for iptv live videos
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(), true);



        //Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {

                    public X509Certificate[] getAcceptedIssuers()
                    {
                        Log.d(TAG, "getAcceptedIssuers: ");
                        return new X509Certificate[0];
//                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType)
                    {
                        Log.d(TAG, "checkClientTrusted: ");
                        //
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType)
                    {
                        Log.d(TAG, "checkServerTrusted: "+ movie.getVideoUrl());
                        //
                    }
                }
        };

//Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            Log.d(TAG, "onCreate: error: "+e.getMessage());
            e.printStackTrace();
        }

        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        playerView.setPlayer(player);


// Set the media source to be played.
     //   player.setMediaSource(createMediaSource(movie));

//        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory();
//
//        DataSource.Factory dataSourceFactory = () -> {
//            HttpDataSource dataSource = httpDataSourceFactory.createDataSource();
//            // Set a custom authentication request header.
//            String[] parts = movie.getVideoUrl().split("\\|");
//            for (String part : parts) {
//                if (part.contains("=")) {
//                    String[] keyValue = part.split("=");
//                    String key = keyValue[0];
//                    String value = keyValue[1];
//                    dataSource.setRequestProperty(key, value);
//                }
//            }
//            movie.setVideoUrl(movie.getVideoUrl().substring(0, movie.getVideoUrl().indexOf('|')));
//            Log.d("TAG", "dataSource:exoplayer "+movie.getVideoUrl());
//            return dataSource;
//        };
//        MediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(Uri.parse(movie.getVideoUrl()));

//
//        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(Uri.parse(movie.getVideoUrl()));
//        player.setMediaSource(mediaSource);


//hier        player.setMediaItem(createMediaSource(movie));
//
//// Prepare the player.
//        player.prepare();
        MediaSource mediaSource = buildMediaSource(movie);

        player.prepare(mediaSource);
        Log.d(TAG, "onCreate: player.prepare(mediaSource) ");

        playerView.setControllerAutoShow(false);


        // Set the touch listener for the view that displays the ExoPlayer
        playerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouch: ");
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Save the initial x position of the touch event
                    ExoplayerMediaPlayer.initialX = event.getX();
                    Log.d("player", "onTouch:"+initialX+" ACTION_DOWN");

                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.d("player", "onTouch:"+initialX+" ACTION_MOVE "+event.getX());

                    // Update the current x position of the touch event
                    float currentX = event.getX();

                    // Do something with the deltaX value, such as updating a visual indication of the seek position
                    if (Math.abs(event.getX()) > initialX + MIN_SEEK_DISTANCE) {
                        // Seek forward or backward by 15 seconds if the touch moved a certain distance
                        player.seekTo(player.getCurrentPosition() + SEEK_DURATION_MS);
                    }
                    if (Math.abs(event.getX()) < initialX - MIN_SEEK_DISTANCE) {
                        // Seek forward or backward by 15 seconds if the touch moved a certain distance
                        player.seekTo(player.getCurrentPosition() - SEEK_DURATION_MS);
                    }
                    return true;
                }else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Calculate the difference between the initial and final x position of the touch event
                    Log.d("player", "onTouch:"+event.getX()+" ACTION_UP "+initialX);
                    ExoplayerMediaPlayer.initialX = 0;
                    if (!playerView.isControllerFullyVisible()){
                        playerView.showController();
                    }
                    return true;
                }
                return true;
            }
        });

        player.addAnalyticsListener(new AnalyticsListener(){
            @Override
            public void onAudioSinkError(EventTime eventTime, Exception audioSinkError) {
                Log.d(TAG, "onAudioSinkError: "+audioSinkError.getMessage());
//                AnalyticsListener.super.onAudioSinkError(eventTime, audioSinkError);
                MediaSource mediaSource = buildMediaSource(movie);
                player.prepare(mediaSource);
                player.play();
            }
        });
        player.addListener(new Player.Listener() {
            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                Log.d(TAG, "onTimelineChanged: " + reason);
                if (player.getDuration() != C.TIME_UNSET) {
                    if (!hasSeekedToWatchedPosition) {
                        long movieLength = player.getDuration();
                        hasSeekedToWatchedPosition = true; // Set it here to only try once.
                        new Thread(() -> {
                            movieRepository.updateMovieLength(movie.getParentId(), movieLength);

                            // --- Start Gemini Debug Logging ---
                            Object parentIdObj = movie.getParentId();
                            Log.d(TAG, "DEBUG: parentId object is: " + parentIdObj);
                            if (parentIdObj != null) {
                                Log.d(TAG, "DEBUG: parentId class is: " + parentIdObj.getClass().getName());
                                Log.d(TAG, "DEBUG: parentId value is: '" + parentIdObj + "'");
                            }
                            // --- End Gemini Debug Logging ---

                            MovieHistory history = movieRepository.getMovieHistoryByMovieIdSync(movie.getParentId());
                            Log.d(TAG, "onTimelineChanged: parentId: "+ movie.getParentId() );
                            Log.d(TAG, "onTimelineChanged: movieId: "+ movie.getId() );
                            Log.d(TAG, "onTimelineChanged: history: "+ history );
                            if (history != null) {
                                runOnUiThread(() -> {
                                    long watchedPosition = history.getWatchedPosition();
                                    Log.d(TAG, "onTimelineChanged: watchedPosition: "+ watchedPosition + ", length: "+ movieLength);
                                    if (watchedPosition > 0 && movieLength > 0) {
                                        long percentage = (watchedPosition * 100) / movieLength;
                                        if (percentage < 95) {
                                            player.seekTo(watchedPosition);
                                            Log.d(TAG, "onTimelineChanged: seeked to: "+ watchedPosition);

                                        }
                                    }
                                });
                            }
                        }).start();
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
//            public void onPlayerError(ExoPlaybackException error) {
                Log.d("TAG", "onPlayerError: xxxx:"+error.getMessage()+", "+error.errorCode+", "+ error.toString()+ ", "+movie.getVideoUrl());

                int c = error.errorCode;
                if (c == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ){
                    String studio = movie.getStudio();
                    if (studio.equals(Movie.SERVER_OMAR) || studio.equals(Movie.SERVER_IPTV)){
                        MediaSource mediaSource = buildMediaSource(movie);
                        player.prepare(mediaSource);
                        player.play();
                        return;
                    }
                }


                boolean deleteCond = c == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                        || c == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                        || c == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        || c == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                        || c == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED;
                       // || c == PlaybackException.;
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED){
                    deleteCond = error.getCause() != null && error.getCause().getMessage() != null && error.getCause().getMessage().contains("verified");
                }
                if (deleteCond){
                    Log.d("TAG", "onPlayerError: movie deleted: "+movie.toString());
                    movieRepository.deleteMovie(movie);
                }
                Toast.makeText(ExoplayerMediaPlayer.this, "فشل في تشغيل الرابط", Toast.LENGTH_SHORT).show();
                Player.Listener.super.onPlayerError(error);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d(TAG, "onPlaybackStateChanged: xxxx:"+playbackState);
               String studio = movie.getStudio();
                if (!studio.equals(Movie.SERVER_IPTV)
                && !studio.equals(Movie.SERVER_OMAR)){
                    return;
                }
                if (playbackState == Player.STATE_READY){
                    Log.d(TAG, "onPlaybackStateChanged: xxxx: STATE_READY");
//                    player.play();
                    return;
//                    dbHelper.addMovieToHistory(movie, false);
//                    if (player != null && player.getCurrentPosition() == 0){
//                      //  player.seekTo(movie.getPlayedTime());
//                    }
                }
                if (playbackState == Player.STATE_ENDED){
                    Log.d(TAG, "onPlaybackStateChanged: xxxx: STATE_ENDED");
                    MediaSource mediaSource = buildMediaSource(movie);
                    player.prepare(mediaSource);
                    player.play();
                    return;
//                    dbHelper.addMovieToHistory(movie, false);
//                    if (player != null && player.getCurrentPosition() == 0){
//                      //  player.seekTo(movie.getPlayedTime());
//                    }
                }
                Player.Listener.super.onPlaybackStateChanged(playbackState);
            }
        });

        //  player.play()
        playerView.setKeepScreenOn(true);
        player.play();
        //   player.play();

    }


    public void handleBackPressed() {
        //super.onBackPressed();
        Log.d("TAG", "onBackPressed: 1");
        //check if waiting time between the second click of back button is greater less than 2 seconds so we finish the app
        if (backPressedTime + 1500 > System.currentTimeMillis()) {
            Log.d("TAG", "onBackPressed: 2 +");
            if (player != null){
                Log.d("TAG", "onBackPressed: 2 player released");
                //    movie.setPlayedTime(String.valueOf(player.getCurrentPosition()));
               // movie.save(dbHelper);
                player.stop();
                player.release();
            }
            finish();
        } else {
            Log.d("TAG", "onBackPressed: 3");
            if (playerView.isControllerFullyVisible())
                playerView.hideController();
            else
                Toast.makeText(this, "Press back 2 time to exit", Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
//        super.onBackPressed();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        assert player != null;
       // movie.setPlayedTime(String.valueOf(player.getCurrentPosition()));
       // movie.save(dbHelper);
        player.pause();
        playerView.setKeepScreenOn(false);
    }

    public  List<String> splitString(String headers) {
        List<String> headerList = new ArrayList<>();
        String[] headerParts = headers.split("&");
        for (String headerPart : headerParts) {
            String[] keyValuePair = headerPart.split("=");
            if (keyValuePair.length == 2) {
                String key = keyValuePair[0];
                String value = keyValuePair[1];
            }
        }
        return headerList;
    }

    private MediaSource buildMediaSource_old(Movie movie) {
        if (Objects.equals(movie.getStudio(), Movie.SERVER_OLD_AKWAM) && !movie.getVideoUrl().contains("https")){
            movie.setVideoUrl(movie.getVideoUrl().replace("http", "https"));
        }
        String url = movie.getVideoUrl();
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        if (url.contains("||")){
            String[] splitString = url.split("\\|\\|");
            url = splitString[0];
            dataSourceFactory = () -> {
                DataSource.Factory  httpDataSourceFactory = new DefaultHttpDataSource.Factory();
                // HttpDataSource dataSource = httpDataSourceFactory.createDataSource();
                DataSource dataSource = httpDataSourceFactory.createDataSource();
                if (splitString.length == 2){
                    Log.d("TAG", "buildMediaSource: extracted headers ssss: "+splitString[1]);
                    String[] headerParts = splitString[1].split("&");
                    for (String headerPart : headerParts) {
                        String[] keyValuePair = headerPart.split("=");
                        if (keyValuePair.length == 2) {
                            String key = keyValuePair[0];
                            String value = keyValuePair[1];
                            Log.d(TAG, "buildMediaSource: "+ key+", "+value);
                            // Set a custom authentication request header.
                            ((HttpDataSource) dataSource).setRequestProperty(key, value);
                        }
                    }
                }

                return dataSource;
            };
        } 
        else if (url.contains("|")){
            String[] splitString = url.split("\\|");
            url = splitString[0];
            dataSourceFactory = () -> {
                DataSource.Factory  httpDataSourceFactory = new DefaultHttpDataSource.Factory();
                // HttpDataSource dataSource = httpDataSourceFactory.createDataSource();
                DataSource dataSource = httpDataSourceFactory.createDataSource();
                if (splitString.length == 2){
                    Log.d("TAG", "buildMediaSource: extracted headers ssss: "+splitString[1]);
                    String[] headerParts = splitString[1].split("&");
                    for (String headerPart : headerParts) {
                        String[] keyValuePair = headerPart.split("=");
                        if (keyValuePair.length == 2) {
                            String key = keyValuePair[0];
                            String value = keyValuePair[1];
                            // Set a custom authentication request header.

                            ((HttpDataSource) dataSource).setRequestProperty(key, value);
                        }
                    }
                }

                return dataSource;
            };
        }
        Log.d(TAG, "buildMediaSource: "+url);
        Uri uri = Uri.parse(url);


        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));

//        acceptAllSSLCertificate(dataSourceFactory);
        int type = Util.inferContentType(uri);

        Log.d("TAG", "buildMediaSource: play: "+type+", "+ uri);
        if (movie.getVideoUrl().contains("m3u")){
            return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
        }
        switch (type) {
            case C.CONTENT_TYPE_SS:
// Create a SmoothStreaming media source pointing to a manifest uri.
                mediaSource = new SsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
                return mediaSource;
//            case C.CONTENT_TYPE_DASH:
//// Create a dash media source pointing to a dash manifest uri.
//                mediaSource = new DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
//                return mediaSource;
            case C.CONTENT_TYPE_HLS:
// Create a HLS media source pointing to a playlist uri.
                HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
                return hlsMediaSource;
        }

        return mediaSource;
//        if (movie.getVideoUrl().contains("m3u")){
//            HlsMediaSource.Factory factory = new HlsMediaSource.Factory(
//                    new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "exoplayer-codelab"))); 
//            HlsMediaSource mediaSource = factory.createMediaSource(uri);
//            return mediaSource;
//        }


//      DataSource.Factory dataSourceFactory = 
//                new DefaultDataSourceFactory(this, "exoplayer-codelab");
//        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
    }

//    private void acceptAllSSLCertificate(DataSource.Factory dataSourceFactory) {
//        try {
//            // Set up SSL context to trust all certificates
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            TrustManager[] trustManagers = new TrustManager[]{
//                    new X509TrustManager() {
//                        @Override
//                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
//                            // Do nothing
//                        }
//
//                        @Override
//                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
//                            // Do nothing
//                        }
//
//                        @Override
//                        public X509Certificate[] getAcceptedIssuers() {
//                            return new X509Certificate[0];
//                        }
//                    }
//            };
//
//            sslContext.init(null, trustManagers, new java.security.SecureRandom());
//
//            // Use the custom SSL context
//            dataSourceFactory.setSslSocketFactory(sslContext.getSocketFactory());
//        } catch (NoSuchAlgorithmException | KeyManagementException e) {
//            e.printStackTrace();
//        }
//    }

    private MediaSource buildMediaSource(Movie movie) {
        updateMovieUrlToHttps(movie);

        String url = movie.getVideoUrl();
        // Split the URL to get the clean URL and headers part
        String[] parts = url.split("\\|", 2);
        String cleanUrl = parts[0];
        Map<String, String> headers = new HashMap<>();

        if (parts.length == 2) {
            headers = com.omerflex.server.Util.extractHeaders(parts[1]);
            Log.d("TAG", "buildMediaSource: h:" + parts[1]);
        }
        DataSource.Factory dataSourceFactory = createDataSourceFactory(cleanUrl, headers);

        Log.d("TAG", "buildMediaSource: cleanUrl:" + cleanUrl);
        Uri uri = Uri.parse(cleanUrl);

        MediaSource mediaSource = createMediaSource(dataSourceFactory, uri, movie);
        Log.d(TAG, "buildMediaSource: mediaSource: "+mediaSource.toString());
        Log.d("TAG", "buildMediaSource: done: " + Util.inferContentType(uri) + ", " + uri);
        return mediaSource;
    }

    private void updateMovieUrlToHttps(Movie movie) {
        if (Objects.equals(movie.getStudio(), Movie.SERVER_OLD_AKWAM) && !movie.getVideoUrl().contains("https")) {
            movie.setVideoUrl(movie.getVideoUrl().replace("http", "https"));
        }
    }

    private DataSource.Factory createDataSourceFactory(String url, Map<String, String> headers) {
        Log.d(TAG, "createDataSourceFactory: h:"+headers);
        if (headers.isEmpty()) {
            return new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(60000)
                    .setReadTimeoutMs(60000);
        }
            return () -> {
                DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(60000)
                        .setReadTimeoutMs(60000);
                DataSource dataSource = httpDataSourceFactory.createDataSource();
                Log.d(TAG, "createDataSourceFactory:setRequestHeaders: "+dataSource.toString());
                    setRequestHeaders(dataSource, headers);

                return dataSource;
            };
    }

    private void setRequestHeaders(DataSource dataSource, Map<String, String> headers) {
        Log.d("TAG", "buildMediaSource: extracted headers: " + headers);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            ((HttpDataSource) dataSource).setRequestProperty(entry.getKey(), entry.getValue());
        }
        Log.d(TAG, "buildMediaSource: extracted headers: done");
    }

    private MediaSource createMediaSource(DataSource.Factory dataSourceFactory, Uri uri, Movie movie) {
        int type = Util.inferContentType(uri);
        Log.d(TAG, "createMediaSource: type: "+type);

        if (movie.getVideoUrl().contains("m3u")) {
            return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
        }
        Log.d(TAG, "createMediaSource: type "+ type);
        switch (type) {
            case C.CONTENT_TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
            case C.CONTENT_TYPE_DASH:
//                // Configure DRM for PlayReady
//                MediaItem.DrmConfiguration drmConfig = new MediaItem.DrmConfiguration.Builder(C.PLAYREADY_UUID)
////                        .setLicenseUri("YOUR_PLAYREADY_LICENSE_SERVER_URL") // Replace with actual license URL
////                        .setForceDefaultLicenseUri(true)
//                        .setMultiSession(true)
//                        .build();
//                MediaItem mediaItem = new MediaItem.Builder()
//                        .setUri(uri)
//                        .setDrmConfiguration(drmConfig)
//                        .build();
//                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
            case C.CONTENT_TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
            default:
                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
        }
    }

    private MediaItem createMediaSource_old(Movie movie) {
        // Uri videoUri = Uri.parse("https://media.geeksforgeeks.org/wp-content/uploads/20201217163353/Screenrecorder-2020-12-17-16-32-03-350.mp4");
        // Build the media item.
        if (Objects.equals(movie.getStudio(), Movie.SERVER_OLD_AKWAM) && !movie.getVideoUrl().contains("https")){
            movie.setVideoUrl(movie.getVideoUrl().replace("http", "https"));
        }
        MediaItem mediaItem = MediaItem.fromUri(movie.getVideoUrl());
        Log.d("Exoplayer", "createMediaSource: "+movie.getVideoUrl()+ "[ "+Uri.parse(movie.getVideoUrl())+" ]");
// Set the media item to be played.
    /*    player.setMediaItem(mediaItem);
// Prepare the player.
        player.prepare();
// Start the playback.
        player.play();

     */
/*
        // Create a data source factory.
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory();
// Create a SmoothStreaming media source pointing to a manifest uri.
        MediaSource mediaSource = 
                new SsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUri));

 */
        return mediaItem;

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // See whether the player view wants to handle media or DPAD keys events.
        Log.d("Exoplayer", "dispatchKeyEvent: "+event.toString());

        if (!playerView.isControllerFullyVisible()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
            {
                playerView.showController();
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)
            {
                playerView.hideController();

                Objects.requireNonNull(player).seekTo(player.getCurrentPosition() + 15000);
            }else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT){
                Objects.requireNonNull(player).seekTo(player.getCurrentPosition() - 15000);
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK){
                if (playerView.isControllerFullyVisible()){
                    playerView.hideController();
                }
            }
        }

        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "onDestroy: yess ");
        if (isFinishing()){
            if (player != null) {
                // movie.setPlayedTime(String.valueOf(player.getCurrentPosition()));
                //movie.save(dbHelper);
                player.release();
                player = null;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d("TAG", "onStop: yess ");
        if (player != null && movie.getParentId() != null) {
            long watchedPosition = player.getCurrentPosition();
            long movieLength = player.getDuration();

            if (movieLength > 0) {
                long playedPercentage = (watchedPosition * 100) / movieLength;
                movie.setPlayedTime(playedPercentage);
            }

            MovieHistory movieHistory = movie.getMovieHistory();
            if (movieHistory == null) {
                // use parent movie
                movieHistory = new MovieHistory(movie.getParentId(), watchedPosition, new Date());
            } else {
                movieHistory.setWatchedPosition(watchedPosition);
                movieHistory.setLastWatchedDate(new Date());
            }
            movie.setMovieHistory(movieHistory);

            movieRepository.updateWatchedTime(movie.getParentId(), watchedPosition);

            // Check if the activity is still running
            if (!isFinishing()) {
                player.pause();
                playerView.setKeepScreenOn(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerView.setKeepScreenOn(true);
        Log.d("TAG", "onResume: yess");
    }

}
