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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.service.database.MovieDbHelper;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ExoplayerMediaPlayer extends AppCompatActivity {

    @Nullable private static SimpleExoPlayer player;
    static String TAG = "Exoplayer";

    MovieDbHelper dbHelper;
    StyledPlayerView playerView;
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


        player = new SimpleExoPlayer.Builder(getApplicationContext()).build();
       // player.setPlayWhenReady(true);
        //leanbackPlayerAdapter = new LeanbackPlayerAdapter(this, player, 0);
        dbHelper = MovieDbHelper.getInstance(this);
         movie =(Movie) getIntent().getSerializableExtra(DetailsActivity.MOVIE);
        Movie movieMainMovie =(Movie) getIntent().getSerializableExtra(DetailsActivity.MAIN_MOVIE);
        movie.setMainMovie(movieMainMovie);

        leanbackPlayerAdapter = new LeanbackPlayerAdapter(this.getApplicationContext(), player, 16);


        //Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers()
                    {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType)
                    {
                        //
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType)
                    {
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

        playerView.setControllerAutoShow(false);


        // Set the touch listener for the view that displays the ExoPlayer
        playerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.d("TAG", "onPlayerError: xxxx:"+error.getMessage()+", "+error.errorCode+", "+ error.toString()+", "+movie.getVideoUrl());
               int c = error.errorCode;
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
                    dbHelper.deleteMovie(movie);
                }

                Player.Listener.super.onPlayerError(error);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d(TAG, "onPlaybackStateChanged: xxxx:"+playbackState);
                if (playbackState == Player.STATE_READY){
                    if (movie.getStudio().equals(Movie.SERVER_IPTV)){
                        dbHelper.addMovieToHistory(movie, false);
                    }
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

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Log.d("TAG", "onBackPressed: 1");
        //check if waiting time between the second click of back button is greater less than 2 seconds so we finish the app
        if (backPressedTime + 1500 > System.currentTimeMillis()) {
            Log.d("TAG", "onBackPressed: 2");
            if (player != null){
            //    movie.setPlayedTime(String.valueOf(player.getCurrentPosition()));
               // movie.save(dbHelper);
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

    private MediaSource buildMediaSource(Movie movie) {
        if (Objects.equals(movie.getStudio(), Movie.SERVER_OLD_AKWAM) && !movie.getVideoUrl().contains("https")){
            movie.setVideoUrl(movie.getVideoUrl().replace("http", "https"));
        }
        String url = movie.getVideoUrl();
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        if (url.contains("|")){
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
        Uri uri = Uri.parse(url);


        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));

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
            case C.CONTENT_TYPE_DASH:
// Create a dash media source pointing to a dash manifest uri.
                mediaSource = new DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
                return mediaSource;
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


    private MediaItem createMediaSource(Movie movie) {
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
        if (player != null) {
            long playtime = player.getCurrentPosition();
            Log.d(TAG, "onStop: playtime:"+playtime  / 60000);
            movie.setPlayedTime(playtime);
             dbHelper.updateMoviePlayTime(movie, playtime);
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