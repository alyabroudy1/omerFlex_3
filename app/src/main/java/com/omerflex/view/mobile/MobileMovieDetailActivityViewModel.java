package com.omerflex.view.mobile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.omerflex.entity.Movie;
import com.omerflex.repository.MovieRepository;
import com.omerflex.utils.Resource;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MobileMovieDetailActivityViewModel extends ViewModel {

    private final MovieRepository movieRepository;
    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<Resource<Movie>> _movieDetails = new MutableLiveData<>();
    public final LiveData<Resource<Movie>> movieDetails = _movieDetails;

    public static final String MOVIE_ARG_KEY = "movie_object"; // Key to receive movie from Activity navigation arguments

    @Inject
    public MobileMovieDetailActivityViewModel(MovieRepository movieRepository, SavedStateHandle savedStateHandle) {
        this.movieRepository = movieRepository;
        this.savedStateHandle = savedStateHandle;

        // Attempt to retrieve the movie from SavedStateHandle, which Hilt populates from Intent extras
        Movie initialMovie = this.savedStateHandle.get(MOVIE_ARG_KEY);

        if (initialMovie != null && initialMovie.getVideoUrl() != null) {
            // Initial movie data is available, post it as success or loading then success
            // For now, directly posting as success, assuming it's detailed enough.
            // If fetching further details is needed, post loading then fetch.
            _movieDetails.setValue(Resource.loading(initialMovie)); // Show initial data while potentially loading more
            loadMovieDetails(initialMovie);
        } else {
            _movieDetails.setValue(Resource.error("No movie data provided in arguments", null));
        }
    }

    public void loadMovieDetails(Movie movie) {
        // This method could be used to refresh details or load more data like sub-lists.
        // For the initial load from SavedStateHandle, we're just using the provided movie.
        // If 'movie' is a stub, this is where you'd use movieRepository to fetch full details.
        // Example:
        // _movieDetails.setValue(Resource.loading(movie));
        // movieRepository.getResolvedMovieDetails(movie).observe(getViewLifecycleOwner(), resource -> {
        //     _movieDetails.setValue(resource);
        // });

        // For this subtask, we assume the initial movie passed via SavedStateHandle is sufficient,
        // or further actions (like fetching episodes) will be triggered by UI interactions.
        // So, we just confirm the initial data as success.
        if (movie != null) {
            _movieDetails.postValue(Resource.success(movie));
        } else {
            // This case should ideally be caught by the constructor check
            _movieDetails.postValue(Resource.error("Movie data is null, cannot load details", null));
        }
    }

    // Call this if an episode or link is clicked from the details view
    public LiveData<Resource<Movie>> playMovieLink(Movie movieLink) {
        // This method is intended to handle a click on a sub-item (like an episode or a specific video link).
        // It might involve resolving the link further using MovieRepository if movieLink is not directly playable.
        // For this example, we assume movieLink is directly playable or its details are sufficient.
        // A more complex scenario:
        // return movieRepository.getPlayableMovieResource(movieLink);

        MutableLiveData<Resource<Movie>> result = new MutableLiveData<>();
        if (movieLink != null && movieLink.getVideoUrl() != null) {
            // Here, one might call a repository method to "prepare" or "resolve" the link
            // For instance, if it's a Movie.ITEM_STATE that needs one more fetch.
            // For simplicity, we assume it's good to go.
            result.setValue(Resource.success(movieLink));
        } else {
            result.setValue(Resource.error("Invalid movie link provided for playback", null));
        }
        return result;
    }

    public void markAsWatched(Movie movie) {
        if (movie != null && movie.getVideoUrl() != null) {
            // Ensure playedTime is reasonable if it's being set.
            // If movie.getPlayedTime() is 0, it might just mark as history without specific progress.
            movieRepository.setMovieAsHistory(movie.getVideoUrl(), System.currentTimeMillis());
            if (movie.getPlayedTime() > 0) {
                movieRepository.updateMoviePlayTime(movie.getVideoUrl(), movie.getPlayedTime());
            }
        }
    }
}
