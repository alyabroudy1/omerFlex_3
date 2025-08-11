package com.omerflex.entity;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.omerflex.entity.dto.MovieListDTO;

public class MovieSyncRepository {
    private static final String TAG = "MovieSyncRepository";
    private final DatabaseReference databaseReference;

    public MovieSyncRepository() {
        databaseReference = FirebaseDatabase.getInstance().getReference("movie_sync");
    }

    private String encodeUrl(String url) {
        return Base64.encodeToString(url.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public void syncTimestampForMovie(Movie movie, long timestamp) {

        // send time stam
        if (movie == null || movie.getVideoUrl() == null) {
            Log.e(TAG, "Movie or video URL is null");
            return;
        }

        String key = encodeUrl(movie.getVideoUrl());

        databaseReference.child(key).setValue(movie)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Movie saved: " + movie.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving movie", e));
    }

    public void getMovieByUrl(String videoUrl, MovieCallback callback) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.e(TAG, "Video URL is null or empty");
            return;
        }

        String key = encodeUrl(videoUrl);

        databaseReference.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Movie movie = snapshot.getValue(Movie.class);
                callback.onMovieFetched(movie);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch movie", error.toException());
                callback.onMovieFetched(null);
            }
        });
    }

    public boolean isRemoteUpdateNeeded(int remoteUpdatePeriod) {
        // todo implement
        return true;
    }

    public MovieListDTO getMovieList() {
        return new MovieListDTO();
    }

    public interface MovieCallback {
        void onMovieFetched(Movie movie);
    }
}
