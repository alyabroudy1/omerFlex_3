package com.omerflex.service;

import android.util.Log;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieRepository;
import com.omerflex.entity.MovieSyncRepository;
import com.omerflex.entity.dto.MovieListDTO;
import com.omerflex.service.database.MovieDbHelper;

import java.util.ArrayList;

public class MovieDataManager {
    public static String TAG = "MovieDataManager";
    public static int local_Update_Period = 6; // hours
    public static int remote_Update_Period = 12; // hours
    MovieDbHelper localRepo;
    MovieSyncRepository remoteSyncRepo;
    MovieRepository remoteRepo;
    public MovieDataManager(MovieDbHelper localRepo) {
        this.localRepo = localRepo;
        this.remoteSyncRepo = new MovieSyncRepository();
        this.remoteRepo = new MovieRepository();
    }

    public ArrayList<Movie> getLocalMovies() {
        Log.d(TAG, "getLocalMovies: Movie data from local");
        // get local movies first
        return localRepo.getHomepageMovies();
    }

    public ArrayList<Movie> getRemoteMoviesWithinPeriod() {
        Log.d(TAG, "getRemoteMoviesWithinPeriod: Movie data from remote");
        // get local movies first
        ArrayList<Movie> movies = new ArrayList<Movie>();

        // check last local update stamp if less than update period
        boolean isLocalUpdateNeeded = localRepo.isLocalUpdateNeeded(local_Update_Period);

        if (!isLocalUpdateNeeded) {
            return movies;
        }

        // fetch remote movies
        remoteRepo.getHomepageMovies( movieList -> {
            if (movieList != null) {
                Log.d(TAG, "Fetched movieList: " + movieList.size());
            } else {
                Log.d(TAG, "movieList not found.");
            }
        });

        // check last remote update stamp if less than update period
        boolean isRemoteUpdateNeeded = remoteSyncRepo.isRemoteUpdateNeeded(remote_Update_Period);

        if (!isRemoteUpdateNeeded) {
            return movies;
        }


        // get remote movies
        MovieListDTO movieListDTO = remoteSyncRepo.getMovieList();
        if (movieListDTO != null) {
//            movies = movieListDTO.getMovies();
//            // save movies to local
//            localRepo.saveMovies(movies);
        }

        return new ArrayList<Movie>();
    }
}
