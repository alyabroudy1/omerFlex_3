package com.omerflex.view;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.ServerConfigManager;

import java.util.Map;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    private static final int CARD_WIDTH = 360; //313
    private static final int CARD_HEIGHT = 511; // 176
    private static int sSelectedBackgroundColor;
    private static int sDefaultBackgroundColor;
    private Drawable mDefaultCardImage;

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? sSelectedBackgroundColor : sDefaultBackgroundColor;
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setBackgroundColor(color);
        view.setInfoAreaBackgroundColor(color);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
//        Log.d(TAG, "onCreateViewHolder");

        sDefaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.default_background);
        sSelectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.selected_background);
        /*
         * This template uses a default image in res/drawable, but the general case for Android TV
         * will require your resources in xhdpi. For more information, see
         * https://developer.android.com/training/tv/start/layouts.html#density-resources
         */
        mDefaultCardImage = ContextCompat.getDrawable(parent.getContext(), R.drawable.default_background);

        ImageCardView cardView =
                new ImageCardView(parent.getContext()) {
                    @Override
                    public void setSelected(boolean selected) {
                        updateCardBackgroundColor(this, selected);
                        super.setSelected(selected);
                    }
                };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Movie movie = (Movie) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;

//        Log.d(TAG, "onBindViewHolder");
        if (movie.getCardImageUrl() != null) {
            cardView.setTitleText(movie.getTitle());
            cardView.setContentText(movie.getRate() + ' ' + movie.getStudio());

            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

//            Glide.with(viewHolder.view.getContext())
//                    .load(movie.getCardImageUrl())
//                    .centerCrop()
//                    .error(mDefaultCardImage)
//                    .into(cardView.getMainImageView());


            ServerConfig config = ServerConfigManager.getConfig(movie.getStudio());

            boolean noHeaderCondition = config == null ||
                    config.getHeaders() == null ||
                    config.getHeaders().isEmpty();
            if (noHeaderCondition) {
                Glide.with(viewHolder.view.getContext())
                        .load(movie.getCardImageUrl())
                        .fitCenter()
                        //.centerCrop()
                        .error(mDefaultCardImage)
                        .into(cardView.getMainImageView());
                return;
            }

            String cookies = config.getStringCookies();
            if (cookies == null) {
                cookies = "";
            }
//            Log.d(TAG, "onBindViewHolder: cookies: " + cookies);
//            Log.d(TAG, "onBindViewHolder: headers: " + config.getHeaders());
            LazyHeaders.Builder builder = new LazyHeaders.Builder()
                    .addHeader("Cookie", cookies);

            for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }

// Ensure URL is not null
            String imageUrl = movie.getCardImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                Log.e(TAG, "onBindViewHolder: movie.getCardImageUrl() is NULL or EMPTY!");
                imageUrl = "https://your-default-image-url.com/default.jpg"; // Use a fallback
            }

            GlideUrl glideUrl = new GlideUrl(imageUrl, builder.build());
            try {
                Glide.with(viewHolder.view.getContext())
                        .load(glideUrl)
                        .fitCenter()
                        .error(mDefaultCardImage)
                        .into(cardView.getMainImageView());
            }catch (Exception e){
                Log.d(TAG, "onBindViewHolder: error: "+e.getMessage());
            }

        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
//        Log.d(TAG, "onUnbindViewHolder");
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}