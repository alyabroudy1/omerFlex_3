package com.omerflex.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieRepository;

public class SharedViewModel extends AndroidViewModel {
    private final MovieRepository movieRepository;
    public final LiveData<Movie> selectedMovie;

    public SharedViewModel(@NonNull Application application) {
        super(application);
        movieRepository = MovieRepository.getInstance(application);
        selectedMovie = movieRepository.getSelectedMovie();
    }

    public void updateMovie(Movie movie) {
        movieRepository.setSelectedMovie(movie);
        movieRepository.updateMovieInDb(movie);
    }

    public void setSelectedMovie(Movie movie) {
        movieRepository.setSelectedMovie(movie);
    }
}
