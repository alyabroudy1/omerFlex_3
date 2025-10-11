package com.omerflex.service;

import android.net.Uri;
import android.util.Log;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.omerflex.entity.Movie;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@androidx.media3.common.util.UnstableApi
public class MediaSourceFactory {

    private static final String TAG = "MediaSourceFactory";

    public MediaSourceFactory() {
        // public constructor
    }

    public MediaSource buildMediaSource(Movie movie) {
        updateMovieUrlToHttps(movie);

        String url = movie.getVideoUrl();
        // Split the URL to get the clean URL and headers part
        String[] parts = url.split("\\|", 2);
        String cleanUrl = parts[0];
        Map<String, String> headers = new HashMap<>();

        if (parts.length == 2) {
            headers = com.omerflex.server.Util.extractHeaders(parts[1]);
            Log.d(TAG, "buildMediaSource: h:" + parts[1]);
        }
        DataSource.Factory dataSourceFactory = createDataSourceFactory(cleanUrl, headers);

        Log.d(TAG, "buildMediaSource: cleanUrl:" + cleanUrl);
        Uri uri = Uri.parse(cleanUrl);

        MediaSource mediaSource = createMediaSource(dataSourceFactory, uri, movie);
        Log.d(TAG, "buildMediaSource: mediaSource: " + mediaSource.toString());
        Log.d(TAG, "buildMediaSource: done: " + Util.inferContentType(uri) + ", " + uri);
        return mediaSource;
    }

    private void updateMovieUrlToHttps(Movie movie) {
        if (Objects.equals(movie.getStudio(), Movie.SERVER_OLD_AKWAM) && !movie.getVideoUrl().contains("https")) {
            movie.setVideoUrl(movie.getVideoUrl().replace("http", "https"));
        }
    }

    private DataSource.Factory createDataSourceFactory(String url, Map<String, String> headers) {
        Log.d(TAG, "createDataSourceFactory: h:" + headers);
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
            Log.d(TAG, "createDataSourceFactory:setRequestHeaders: " + dataSource.toString());
            setRequestHeaders(dataSource, headers);

            return dataSource;
        };
    }

    private void setRequestHeaders(DataSource dataSource, Map<String, String> headers) {
        Log.d(TAG, "buildMediaSource: extracted headers: " + headers);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            ((HttpDataSource) dataSource).setRequestProperty(entry.getKey(), entry.getValue());
        }
        Log.d(TAG, "buildMediaSource: extracted headers: done");
    }

    private MediaSource createMediaSource(DataSource.Factory dataSourceFactory, Uri uri, Movie movie) {
        int type = Util.inferContentType(uri);
        String mimeType = determineMimeType(uri.toString());

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .setMimeType(mimeType)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(movie.getTitle())
                        .build())
                .build();

        Log.d(TAG, "createMediaSource: type: " + type + ", mimeType: " + mimeType);

        if (movie.getVideoUrl().contains("m3u")) {
            return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        }

        switch (type) {
            case C.CONTENT_TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
            case C.CONTENT_TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
            case C.CONTENT_TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
            default:
                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        }
    }

    private String determineMimeType(String url) {
        Uri uri = Uri.parse(url);
        @C.ContentType int contentType = Util.inferContentType(uri);

        switch (contentType) {
            case C.CONTENT_TYPE_HLS:
                return MimeTypes.APPLICATION_M3U8;
            case C.CONTENT_TYPE_DASH:
                return MimeTypes.APPLICATION_MPD;
            case C.CONTENT_TYPE_SS:
                return MimeTypes.APPLICATION_SS;
            default:
                // For progressive streams, try to detect from file extension
                if (url.contains(".m3u8")) {
                    return MimeTypes.APPLICATION_M3U8;
                } else if (url.contains(".mpd")) {
                    return MimeTypes.APPLICATION_MPD;
                } else if (url.contains(".ism")) {
                    return MimeTypes.APPLICATION_SS;
                } else if (url.contains(".mp4")) {
                    return MimeTypes.VIDEO_MP4;
                } else if (url.contains(".webm")) {
                    return MimeTypes.VIDEO_WEBM;
                } else if (url.contains(".mkv")) {
                    return "video/x-matroska";
                } else {
                    // Default to MP4
                    return MimeTypes.VIDEO_MP4;
                }
        }
    }
}
