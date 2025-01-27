package com.omerflex.service;

import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;

import com.omerflex.entity.Movie;

import java.util.Arrays;
import java.util.List;

public class LinkFilterService {

    static String TAG = "LinkFilterService";

    public static boolean isSupportedMedia(WebResourceRequest request) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(request.getUrl().toString()));
        String acceptEncoding = request.getRequestHeaders().get("Accept-Encoding");
//        String accept = request.getRequestHeaders().get("Accept");
        boolean isAcceptEncoding = acceptEncoding != null && acceptEncoding.contains("identity;q=1");
        if (isAcceptEncoding) {
            Log.d(TAG, "isSupportedMedia: isAcceptEncoding: " + acceptEncoding);
            if (LinkFilterService.isBlackListedUrl(request.getUrl().toString())) {
                return false;
            }
            return true;
        }
//        Log.d(TAG, "isSupportedMedia: accept: "+ accept);
//        Log.d(TAG, "isSupportedMedia: acceptEncoding: "+ acceptEncoding);
        if (mimeType != null) {
                Log.d(TAG, "isSupportedMedia: mimeType: "+mimeType);
            if (mimeType.startsWith("video") || mimeType.startsWith("audio")) {
                Log.d(TAG, "isSupportedMedia: mimeType: " + mimeType + ", accept: "+acceptEncoding);
                if (request.getUrl().getPath().contains("index_")){
//                    Log.d(TAG, "isSupportedMedia: path: "+request.getUrl().getPath());
//                    Log.d(TAG, "isSupportedMedia: headers: "+request.getRequestHeaders());
                    return false;
                }
                return true;
            }  // Check for video mimetypes
        }

        return false;
    }

    public static boolean isBlackListedUrl(String url) {
        // List of substrings to check
        List<String> patterns = Arrays.asList(
                "click",
                "brand",
                "/patrik",
                "adserver",
                ".php",
                ".gif",
                "error",
                "null",
                "/stub",
                ".html"
        );

        // Check if the URL contains any of the substrings
        for (String pattern : patterns) {
            if (url.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVideo(String url, Movie movie) {

        if (LinkFilterService.isBlackListedUrl(url)) {
            return false;
        }
        List<String> patternsMovieUrl = Arrays.asList(
                "vidmoly", ".html"
        );

        String movieUrl = movie.getVideoUrl();

        // Check if the URL contains any of the substrings
        for (String pattern : patternsMovieUrl) {
            if (movieUrl.contains(pattern)) {
                return false;
            }
        }

        if ((url.contains("token=") && (url.contains("inconsistencygasdifficult") || url.contains("video-delivery")))) {
//        if ((url.contains("token=") && (url.contains("inconsistencygasdifficult")))) {
            return false;
        }

        // Check the file extension
        if (url.endsWith(".mp4") || (url.contains("file_code=") && !(url.contains("embed") || url.contains("watch"))) ||
                (url.contains("token=") && !(url.contains("embed") || url.contains("watch"))) ||
                (url.endsWith(".mov") && !url.contains("_ads")) || url.endsWith(".avi") || url.endsWith(".wmv") || url.endsWith(".m3u") || url.endsWith(".m3u8") || url.endsWith(".mkv") || url.contains(".m3u8")) {
            // The URL is likely a video
            // Log.d(TAG, "isVideo: 1");
            if (url.endsWith(".mp4")) {
                //avoid audio only mp4
                String newUrl = url.substring(url.lastIndexOf("/"));
                //   Log.d(TAG, "isVideo: newUrl:" + newUrl);
                if (newUrl.contains("_a_") || url.contains("themes") || url.contains("/test") || url.contains("cloudfront") || url.length() < 50 || url.contains("//store") || url.contains("//rabsh")) {
                    return false;
                }
            }
            Log.d(TAG, "xxx: isVideo: " + url);
            return true;
        }
        // Log.d(TAG, "isVideo: no one 4:" + url);
        return false;
    }
}
