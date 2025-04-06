package com.omerflex.service.phub;

/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpRetriever {

	static final String COOKIES_HEADER = "Set-Cookie";
	static final String TAG = "HttpRetriever";
	static final String COOKIE = "Cookie";
	static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.88 Mobile Safari/537.36";
	static CookieManager cookieManager = new CookieManager();
	
	
    public static String retrieve(String url) {
        URL targetURL;
        try {
            targetURL = new URL(url);
        } catch (MalformedURLException e) {
            Log.d(TAG, "retrieve: error: "+e.getMessage());
            return null;
        }
        HttpURLConnection urlConnection = null;
        String response;
        try {
            urlConnection = (HttpURLConnection) targetURL.openConnection();
            urlConnection.setRequestMethod("GET");
			urlConnection.setRequestProperty("Accept-Encoding", "gzip");
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);


            if (cookieManager.getCookieStore().getCookies().size() > 0) {
                List<String> validCookies = new ArrayList<>();
                for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
                    // Proper cookie format: name=value
                    validCookies.add(cookie.getName() + "=" + cookie.getValue());
                }
                urlConnection.setRequestProperty("Cookie", TextUtils.join("; ", validCookies));
            }
			// log.i(TAG,"Cookie " + TextUtils.join(";", cookieManager.getCookieStore().getCookies()));
			Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
			List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
			if (cookiesHeader != null) {
				for (String cookie : cookiesHeader) {
					cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
				}
			}
			
            response = readStream(urlConnection);
        } catch (IOException e) {
            Log.d(TAG, "retrieve: error: "+e.getMessage());
            return null;
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        // log.d(TAG, "retrieve: done");
        return response;
    }

    private static String readStream(HttpURLConnection connection) {
        BufferedReader br;
        StringBuilder builder = new StringBuilder();
        String line;
        try {
			if ("gzip".equals(connection.getContentEncoding())) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
			}
			else {
				br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			}
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            //Unable to read from the stream
            return null;
        }


        return builder.toString();
    }
}
