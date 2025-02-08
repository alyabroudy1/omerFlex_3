package com.omerflex.service;

import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;

import com.omerflex.entity.Movie;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LinkFilterService {

    static String TAG = "LinkFilterService";

    private static final Set<String> blockedDomains = new HashSet<>(Arrays.asList(
            "doubleclick.net",
            "googleadservices.com",
            "ads.pubmatic.com",
            "admob.com",
            "amazon-adsystem.com",
            "facebook.com", // Be careful with this one
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "yieldmo.com",
            "scorecardresearch.com",
            "quantserve.com",
            "roagrofoogrobo.com",
            "onmanectrictor.com",
            "2mdn.net",
            "adalliance.io",
            "adform.net",
            "adgrx.com",
            "adhigh.net",
            "admeld.com",
            "adnxs.com",
            "adroll.com",
            "adsafeprotected.com",
            "adsrvr.org",
            "adservice.google.com",
            "adtechus.com",
            "advertising.com",
            "adthor.com",
            "adyoulike.com",
            "amazon-adsystem.com",
            "aniview.com",
            "aps.amazon.com",
            "bidswitch.net",
            "bluekai.com",
            "brightroll.com",
            "casalemedia.com",
            "cdn.stickyadstv.com",
            "chartbeat.net",
            "clickaine.com",
            "cloudflareinsights.com",
            "criteo.com",
            "crwdcntrl.net",
            "demdex.net",
            "doubleclick.net",
            "doubleverify.com",
            "dpm.demdex.net",
            "emxdgt.com",
            "everesttech.net",
            "exelator.com",
            "eyeota.net",
            "facebook.net",
            "fbcdn.net",
            "gemius.pl",
            "google-analytics.com",
            "googleadservices.com",
            "googlesyndication.com",
            "googletagmanager.com",
            "hotjar.com",
            "hubspot.com",
            "imrworldwide.com",
            "innovid.com",
            "kruxdigital.com",
            "lijit.com",
            "loopme.com",
            "lotame.com",
            "media.net",
            "mediavoice.com",
            "mgid.com",
            "moatads.com",
            "mookie1.com",
            "mouseflow.com",
            "nativo.com",
            "nexac.com",
            "openx.net",
            "outbrain.com",
            "permuitive.com",
            "pinterest.com",
            "polarbyte.com",
            "prebid.org",
            "primis.tech",
            "propellorads.com",
            "pubmatic.com",
            "pushnative.com",
            "quantcount.com",
            "quantserve.com",
            "revcontent.com",
            "rfihub.com",
            "rhyhthmone.com",
            "richaudience.com",
            "rtbidder.net",
            "rubiconproject.com",
            "sascdn.com",
            "scorecardresearch.com",
            "semantic.com",
            "serving-sys.com",
            "sharethis.com",
            "sharethrough.com",
            "simpli.fi",
            "smartadserver.com",
            "sonobi.com",
            "sovrn.com",
            "spot.im",
            "spotxchange.com",
            "taboola.com",
            "tapad.com",
            "teads.tv",
            "themediagrid.com",
            "themidiagrid.com",
            "tidaltv.com",
            "tr.snapchat.com",
            "trafficfactory.biz",
            "tremorhub.com",
            "tribalfusion.com",
            "tru.am",
            "turn.com",
            "twitter.com",
            "ueili.com",
            "undertone.com",
            "uniconsent.com",
            "verizonmedia.com",
            "vidoomy.com",
            "vungle.com",
            "xaxis.com",
            "yandex.ru",
            "yieldlab.net",
            "yieldlove.com",
            "yieldmo.com",
            "zeal.com",
            "zedo.com",
            "zemanta.com",
            "33across.com",
            "360yield.com",
            "eehassoosostoa.com",
            "glempirteechacm.com"
            // ... Add more domains here!
    ));

    public static boolean isAdDomain(String domain) {
        return domain != null && blockedDomains.contains(domain);
    }

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
            Log.d(TAG, "isSupportedMedia: mimeType: " + mimeType);
            if (mimeType.startsWith("video") || mimeType.startsWith("audio")) {
                Log.d(TAG, "isSupportedMedia: mimeType: " + mimeType + ", accept: " + acceptEncoding);
                if (request.getUrl().getPath().contains("index_")) {
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

    public static String decryptUrl(String encodedString, int shift) {
        // Step 1: Base64 decode the input string
        byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);

        // Step 2: Convert bytes to UTF-8 string
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

        // Step 3: Shift each character's code point
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < decodedString.length(); i++) {
            int codePoint = decodedString.codePointAt(i);
            codePoint -= shift;  // Subtract the shift value
            result.appendCodePoint(codePoint);
        }

        return result.toString();
    }

    // Overload with default shift value (e=3)
    public static String decryptUrl(String encodedString) {
        return decryptUrl(encodedString, 3);
    }
}
