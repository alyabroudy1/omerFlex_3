package com.omerflex.entity;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.omerflex.data.source.local.LocalDataSource;
import com.omerflex.data.source.remote.RemoteDataSource;

import java.util.ArrayList;

public class MovieRepository {
    private static final String TAG = "MovieRepository";
    private static MovieRepository instance;
    private final LocalDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;
    private final MutableLiveData<Movie> selectedMovie = new MutableLiveData<>();

    private MovieRepository(Context context) {
        localDataSource = LocalDataSource.getInstance(context);
        remoteDataSource = RemoteDataSource.getInstance();
    }

    public static synchronized MovieRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MovieRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<Movie> getSelectedMovie() {
        return selectedMovie;
    }

    public void setSelectedMovie(Movie movie) {
        selectedMovie.postValue(movie);
    }

    public void updateMovieInDb(Movie movie) {
        localDataSource.saveMovie(movie);
    }

    public void getMovieByUrl(String videoUrl, MovieCallback callback) {
        localDataSource.getMovieByUrl(videoUrl, movie -> {
            if (movie != null) {
                callback.onMovieFetched(movie);
            } else {
                remoteDataSource.fetchMovieByUrl(videoUrl, remoteMovie -> {
                    if (remoteMovie != null) {
                        localDataSource.saveMovie(remoteMovie);
                        callback.onMovieFetched(remoteMovie);
                    } else {
                        callback.onMovieFetched(null);
                    }
                });
            }
        });
    }

    public void getHomepageMovies(final MovieListCallback callback) {
        Log.d(TAG, "getHomepageMovies: ");
        remoteDataSource.fetchHomepageMovies((remoteCategory, remoteMovies) -> {
            // todo: save movies to local db
            Log.d(TAG, "getHomepageMovies: remote: "+ remoteMovies.size());
            callback.onMovieListFetched(remoteCategory, remoteMovies);
        });
//        localDataSource.getHomepageMovies((category, movies) -> {
//            if (movies != null && !movies.isEmpty()) {
//                Log.d(TAG, "getHomepageMovies: local: "+ movies.size());
//                callback.onMovieListFetched(category, movies);
//            } else {
//                remoteDataSource.fetchHomepageMovies((remoteCategory, remoteMovies) -> {
//                    // todo: save movies to local db
//                    Log.d(TAG, "getHomepageMovies: remote: "+ remoteMovies.size());
//                    callback.onMovieListFetched(remoteCategory, remoteMovies);
//                });
//            }
//        });
    }

    public void fetchNextPage(String url, final MovieListCallback callback) {
        localDataSource.getHomepageMovies((localCategory, movies) -> {
            if (movies != null && !movies.isEmpty()) {
                callback.onMovieListFetched(localCategory, movies);
            } else {
                // todo: save movies to local db
                remoteDataSource.fetchHomepageMovies(callback);
            }
        });
    }

    public void fetchMovieDetails(Movie mSelectedMovie, MovieCallback callback) {
        remoteDataSource.fetchMovieDetails( mSelectedMovie, callback);
//        localDataSource.fetchMovieDetails( mSelectedMovie, localFetchedMovie -> {
//            if (localFetchedMovie != null) {
//                callback.onMovieFetched(localFetchedMovie);
//            } else {
//                // todo: save movies to local db
//                remoteDataSource.fetchMovieDetails( mSelectedMovie, callback);
//            }
//        });
    }

    public interface MovieCallback {
        void onMovieFetched(Movie movie);
    }

    public interface MovieListCallback {
        void onMovieListFetched(String category, ArrayList<Movie> movies);
    }
}