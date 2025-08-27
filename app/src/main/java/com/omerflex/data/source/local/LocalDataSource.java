package com.omerflex.data.source.local;

import android.content.Context;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieRepository;
import com.omerflex.service.database.MovieDbHelper;

import java.util.ArrayList;

public class LocalDataSource {

    private static LocalDataSource instance;
    private MovieDbHelper dbHelper;

    private LocalDataSource(Context context) {
        dbHelper = MovieDbHelper.getInstance(context);
    }

    public static synchronized LocalDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDataSource(context.getApplicationContext());
        }
        return instance;
    }

    public void getMovieByUrl(String videoUrl, MovieRepository.MovieCallback callback) {
        // Assuming findMovieByUrl is synchronous
        Movie movie = dbHelper.findMovieByUrl(videoUrl);
        callback.onMovieFetched(movie);
    }

    public void getHomepageMovies(MovieRepository.MovieListCallback callback) {
        dbHelper.getAllHistoryMovies(false, callback);
    }

    public void saveMovie(Movie movie) {
        dbHelper.saveMovie(movie, false);
    }

    public void fetchMovieDetails(Movie mSelectedMovie, MovieRepository.MovieCallback callback) {
    }
}
