package com.omerflex.service.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.omerflex.R;

import java.util.Map;

/**
 * Utility class for efficient image loading using Glide.
 * Provides centralized configuration and methods for common image loading scenarios.
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";
    
    /**
     * Load an image from a URL into an ImageView
     * 
     * @param context Context
     * @param url Image URL
     * @param imageView Target ImageView
     */
    public static void loadImage(Context context, String url, ImageView imageView) {
        loadImage(context, url, imageView, R.drawable.default_background, R.drawable.default_background);
    }
    
    /**
     * Load an image from a URL into an ImageView with custom placeholder and error images
     * 
     * @param context Context
     * @param url Image URL
     * @param imageView Target ImageView
     * @param placeholderResId Placeholder resource ID
     * @param errorResId Error resource ID
     */
    public static void loadImage(Context context, String url, ImageView imageView, 
                                @DrawableRes int placeholderResId, 
                                @DrawableRes int errorResId) {
        if (context == null || imageView == null) {
            return;
        }
        
        RequestOptions options = new RequestOptions()
                .placeholder(placeholderResId)
                .error(errorResId)
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        
        Glide.with(context)
                .load(url)
                .apply(options)
                .into(imageView);
    }
    
    /**
     * Load an image from a URL with custom headers into an ImageView
     * 
     * @param context Context
     * @param url Image URL
     * @param headers HTTP headers
     * @param imageView Target ImageView
     */
    public static void loadImageWithHeaders(Context context, String url, Map<String, String> headers, ImageView imageView) {
        if (context == null || imageView == null || url == null) {
            return;
        }
        
        // Build headers
        LazyHeaders.Builder headerBuilder = new LazyHeaders.Builder();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                headerBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        
        GlideUrl glideUrl = new GlideUrl(url, headerBuilder.build());
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.default_background)
                .error(R.drawable.default_background)
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        
        Glide.with(context)
                .load(glideUrl)
                .apply(options)
                .into(imageView);
    }
    
    /**
     * Load an image from a URL with a callback for success/failure
     * 
     * @param context Context
     * @param url Image URL
     * @param imageView Target ImageView
     * @param callback Callback for image loading events
     */
    public static void loadImageWithCallback(Context context, String url, ImageView imageView, 
                                           final ImageLoadCallback callback) {
        if (context == null || imageView == null) {
            if (callback != null) {
                callback.onError();
            }
            return;
        }
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.default_background)
                .error(R.drawable.default_background)
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        
        Glide.with(context)
                .load(url)
                .apply(options)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                                              Target<Drawable> target, boolean isFirstResource) {
                        if (callback != null) {
                            callback.onError();
                        }
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, 
                                                 Target<Drawable> target, DataSource dataSource, 
                                                 boolean isFirstResource) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        return false;
                    }
                })
                .into(imageView);
    }
    
    /**
     * Preload an image into memory cache
     * 
     * @param context Context
     * @param url Image URL
     */
    public static void preloadImage(Context context, String url) {
        if (context == null || url == null) {
            return;
        }
        
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload();
    }
    
    /**
     * Clear image loading requests for a specific view
     * 
     * @param context Context
     * @param imageView ImageView to clear
     */
    public static void clearImage(Context context, ImageView imageView) {
        if (context == null || imageView == null) {
            return;
        }
        
        Glide.with(context).clear(imageView);
    }
    
    /**
     * Get a RequestBuilder for custom Glide operations
     * 
     * @param context Context
     * @param url Image URL
     * @return RequestBuilder for further customization
     */
    public static RequestBuilder<Drawable> getRequestBuilder(Context context, String url) {
        return Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL);
    }
    
    /**
     * Callback interface for image loading events
     */
    public interface ImageLoadCallback {
        void onSuccess();
        void onError();
    }
}