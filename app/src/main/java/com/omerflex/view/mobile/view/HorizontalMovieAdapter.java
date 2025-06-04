package com.omerflex.view.mobile.view;

// HorizontalMovieAdapter.java

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.omerflex.R;
import com.omerflex.entity.Movie;

import java.util.ArrayList;
import java.util.List;

public class HorizontalMovieAdapter extends RecyclerView.Adapter<HorizontalMovieAdapter.MovieViewHolder> {
    private Context context;
    private List<Movie> movieList;
    public static String TAG = "HorizontalMovieAdapter";

    private OnMovieClickListener onMovieClickListener;

    public HorizontalMovieAdapter(Context context, List<Movie> movieList, OnMovieClickListener onMovieClickListener) {
        this.context = context;
        this.movieList = movieList;
        this.onMovieClickListener = onMovieClickListener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: " + parent);
        View view = LayoutInflater.from(context).inflate(R.layout.mobile_item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: ");
        Movie movie = movieList.get(position);
        // Load movie image using Glide or any image loading library
        Glide.with(context).load(movie.getCardImageUrl()).into(holder.movieImageView);

        // Set an OnClickListener to open the detailed activity when a movie card is clicked
        holder.movieTitleTextView.setText(movie.getTitle());
        holder.movieRatingTextView.setText(movie.getRate());
        holder.movieStudioTextView.setText(movie.getStudio());

        // Set an OnClickListener to open the detailed activity when a movie card is clicked
        HorizontalMovieAdapter horizontalMovieAdapter = this;
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onMovieClickListener != null) {
                    onMovieClickListener.onMovieClick(movie, position, horizontalMovieAdapter);  // Trigger the callback
                }
            }
        });
    }

    public List<Movie> getMovieList() {
        return movieList;
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public boolean addAll(ArrayList<Movie> movies) {
        return movieList.addAll(movies);
    }

    public void setMovies(List<Movie> newMovies) {
        this.movieList.clear();
        if (newMovies != null) {
            this.movieList.addAll(newMovies);
        }
        notifyDataSetChanged(); // Consider DiffUtil for better performance later
    }

    // These are simplified versions. A real implementation might change item visibility,
    // show a progress bar in the item, or display text differently.
    public void showLoading(boolean isLoading) {
        // This is tricky to do well without modifying item layouts or using multiple view types.
        // For now, we'll log, and a more complex UI would be needed to show per-item loading.
        // If the whole adapter represents one category, the loading state might be shown
        // at the CategoryAdapter level or on the RecyclerView itself.
        // This method is a placeholder for where such logic would go if items could show individual loading.
        if (isLoading) {
            Log.d(TAG, "showLoading: true (individual items might not reflect this without layout changes)");
            // Example: if you had a loading indicator per item, you'd manage it here
            // or if this adapter is for a single category that is loading,
            // you might clear items and show a single "loading" item.
            // For now, clearing and adding a placeholder could be an option if desired.
            // movieList.clear();
            // notifyDataSetChanged(); // Show empty while loading
        } else {
            Log.d(TAG, "showLoading: false");
        }
    }

    public void showError(String message) {
        // Similar to showLoading, displaying a specific error per item is complex without layout changes.
        // This might involve showing a single item with the error message.
        Log.e(TAG, "showError: " + message);
        // Example: Clear items and show a placeholder error item
        // movieList.clear();
        // Movie errorMovie = new Movie(); // Create a dummy movie or use a specific error type
        // errorMovie.setTitle(message != null ? message : "Error loading movies");
        // movieList.add(errorMovie);
        // notifyDataSetChanged();
    }


    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView movieImageView;
        TextView movieTitleTextView;
        TextView movieRatingTextView;
        TextView movieStudioTextView;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            movieImageView = itemView.findViewById(R.id.movieImageView);
            movieTitleTextView = itemView.findViewById(R.id.movieTitleTextView);
            movieRatingTextView = itemView.findViewById(R.id.movieRatingTextView);
            movieStudioTextView = itemView.findViewById(R.id.movieStudioTextView);
        }
    }
}
