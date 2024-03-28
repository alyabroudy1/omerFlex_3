package com.omerflex.view;

import android.graphics.drawable.Drawable;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.omerflex.entity.Movie;
import com.omerflex.R;
import com.omerflex.server.AbstractServer;
import com.omerflex.service.ServerManager;

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
            cardView.setContentText(movie.getRate() + ' '+ movie.getStudio());

            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

//            Glide.with(viewHolder.view.getContext())
//                    .load(movie.getCardImageUrl())
//                    .centerCrop()
//                    .error(mDefaultCardImage)
//                    .into(cardView.getMainImageView());

            AbstractServer server = ServerManager.determineServer(movie, null, null, null);
            if (server != null && server.getHeaders() != null && server.getHeaders().size() > 0){

                String cookies = server.getCookies();
                if (cookies == null){
                    cookies = "";
                }
                Log.d(TAG, "onBindViewHolder: cookies: "+ cookies);
                Log.d(TAG, "onBindViewHolder: headers: "+ server.getHeaders());
                LazyHeaders.Builder builder = new LazyHeaders.Builder()
                        .addHeader("Cookie", cookies);

                for(Map.Entry<String, String> entry :  server.getHeaders().entrySet()) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }

                GlideUrl glideUrl = new GlideUrl(movie.getCardImageUrl(), builder.build());

                Glide.with(viewHolder.view.getContext())
                        .load(glideUrl)
                        .fitCenter()
                        //.centerCrop()
                        .error(mDefaultCardImage)
                        .into(cardView.getMainImageView());

            }
            else {
                Glide.with(viewHolder.view.getContext())
                        .load(movie.getCardImageUrl())
                        .fitCenter()
                        //.centerCrop()
                        .error(mDefaultCardImage)
                        .into(cardView.getMainImageView());
            }
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}