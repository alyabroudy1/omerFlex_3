package com.omerflex.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.omerflex.R;
import com.omerflex.entity.Movie;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movieList;
    private OnItemClickListener onItemClickListener;

    public MovieAdapter(List<Movie> movieList, OnItemClickListener onItemClickListener) {
        this.movieList = movieList != null ? movieList : new ArrayList<>();
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(itemView, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        holder.movieTitle.setText(movie.getTitle());
//        holder.movieImage.setImageURI(movie.getCardImageUrl());
        // Load the movie image using an image loading library like Glide or Picasso
        // For example, using Glide:
         Glide.with(holder.itemView.getContext()).load(movie.getCardImageUrl()).into(holder.movieImage);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    public void updateData(List<Movie> newMovies) {
        // Compare old and new lists and notify specific changes
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MovieDiffCallback(this.movieList, newMovies));
        this.movieList.clear();
        this.movieList.addAll(newMovies);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private OnItemClickListener onItemClickListener;
        private Movie movie;
        public ImageView movieImage;
        public TextView movieTitle;

        public MovieViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            movieImage = view.findViewById(R.id.movie_image);
            movieTitle = view.findViewById(R.id.movie_title);
            this.onItemClickListener = onItemClickListener;
            itemView.setOnClickListener(this);
        }

        public void bind(Movie movie) {
            this.movie = movie;
            // Bind your movie data to views here
        }

        @Override
        public void onClick(View v) {
            onItemClickListener.onItemClick(movie);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Movie movie);
    }

    // DiffUtil.Callback implementation
    static class MovieDiffCallback extends DiffUtil.Callback {
        private final List<Movie> oldList;
        private final List<Movie> newList;

        public MovieDiffCallback(List<Movie> oldList, List<Movie> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Compare the unique identifiers of the items
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Compare the content of the items
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
