package com.omerflex.view;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.omerflex.entity.Movie;
import com.omerflex.repository.MovieRepository;
import com.omerflex.utils.Resource;

import java.util.List; // For subList
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import androidx.lifecycle.viewModelScope;
import kotlinx.coroutines.launch;

@HiltViewModel
public class VideoDetailsViewModel extends ViewModel {

    private final MovieRepository movieRepository;
    private final SavedStateHandle savedStateHandle;

    // Holds the main movie details (overview)
    private final MutableLiveData<Resource<Movie>> _movieDetails = new MutableLiveData<>();
    public final LiveData<Resource<Movie>> movieDetails = _movieDetails;

    // Holds the list of related items (e.g., episodes, seasons, or other related movies)
    // This might be part of the Movie object itself (movie.getSubList()) or fetched separately.
    // For now, assume it's part of the main Movie object fetched.
    // If subList needs its own loading/error state, it would need its own Resource wrapper.

    // For actions like playing a movie or handling a click that results in an action
    private final MutableLiveData<Resource<Movie>> _movieActionOutcome = new MutableLiveData<>();
    public final LiveData<Resource<Movie>> movieActionOutcome = _movieActionOutcome;

    public static final String MOVIE_ARG_KEY = "movie_details_arg"; // Key for SavedStateHandle
    // Define request codes if they are used to interpret results in handleActivityResult
    public static final int REQUEST_CODE_COOKIE_FIX = 1001; // Example for cookie resolution

    @Inject
    public VideoDetailsViewModel(MovieRepository movieRepository, SavedStateHandle savedStateHandle) {
        this.movieRepository = movieRepository;
        this.savedStateHandle = savedStateHandle;

        Movie initialMovie = savedStateHandle.get(MOVIE_ARG_KEY);
        if (initialMovie != null) {
            loadMovieDetails(initialMovie);
        } else {
            _movieDetails.setValue(Resource.error("No movie data provided for details view", null));
        }
    }

    public void loadMovieDetails(Movie movie) {
        if (movie == null || movie.getVideoUrl() == null) {
            _movieDetails.postValue(Resource.error("Invalid movie data for loading details.", null));
            return;
        }
        _movieDetails.postValue(Resource.loading(movie)); // Post initial movie as loading data

        // Fetch full details from repository
        // Assuming getMovieByVideoUrlLiveData returns LiveData<Movie> or LiveData<Resource<Movie>>
        // For simplicity, let's assume it returns LiveData<Movie> and we wrap it.
        // If MovieRepository.getMovieByVideoUrlLiveData itself returns LiveData<Resource<Movie>>, adapt accordingly.
        movieRepository.getMovieByVideoUrlLiveData(movie.getVideoUrl()).observeForever(detailedMovie -> {
            if (detailedMovie != null) {
                // If detailedMovie contains subList and other fetched details, it's good.
                _movieDetails.postValue(Resource.success(detailedMovie));
            } else {
                // If null is returned, it means not found or an error occurred during fetch that wasn't a Resource.Error
                _movieDetails.postValue(Resource.error("Failed to load full movie details.", movie)); // Keep initial movie as data for error
            }
            // Important: If getMovieByVideoUrlLiveData is a one-shot LiveData or if you want to prevent multiple triggers,
            // you might need to remove the observer here, or use a different pattern if it's a continuous stream.
            // For a typical "fetch once" operation, you might not use observeForever or ensure it's cleaned up.
            // However, many repository LiveData are designed to auto-update if underlying data changes.
        }); // This was the problematic observeForever call, ensuring it's commented or removed.

        // Using viewModelScope.launch for suspend function call
        viewModelScope.launch {
            try {
                // TODO: Ensure MovieRepository has a suspend function like getMovieByVideoUrlSync(url: String): Movie?
                // This function should return the detailed movie or null if not found/error.
                val detailedMovie = movieRepository.getMovieByVideoUrlSync(movie.getVideoUrl());
                if (detailedMovie != null) {
                     _movieDetails.postValue(Resource.success(detailedMovie));
                } else {
                    _movieDetails.postValue(Resource.error("Movie details not found.", movie));
                }
            } catch (Exception e) {
                // Log.e("VideoDetailsViewModel", "Error fetching movie details", e);
                _movieDetails.postValue(Resource.error("Error fetching details: " + e.getMessage(), movie));
            }
        }
    }

    // To store the movie for which an external activity (like cookie resolution) was started.
    private Movie moviePendingForResult = null;

    public void playMovieLink(Movie movieToPlay) {
        if (movieToPlay == null) {
            _movieActionOutcome.postValue(Resource.error("Cannot play null movie.", null));
            return;
        }
        _movieActionOutcome.postValue(Resource.loading(movieToPlay));

        viewModelScope.launch {
            // TODO: Ensure MovieRepository has a suspend function like prepareMovieForPlayback(movie: Movie): Resource<Movie>
            // This function in repository would handle server calls, check cookie needs, update movie state (COOKIE_STATE, VIDEO_STATE), etc.
            try {
                Resource<Movie> preparedMovieResource = movieRepository.prepareMovieForPlayback(movieToPlay); // Hypothetical suspend fun

                if (preparedMovieResource.status == Resource.Status.SUCCESS &&
                    preparedMovieResource.data != null &&
                    preparedMovieResource.data.getState() == Movie.COOKIE_STATE) {
                    moviePendingForResult = preparedMovieResource.data;
                }
                _movieActionOutcome.postValue(preparedMovieResource);

            } catch (Exception e) {
                // Log.e("VideoDetailsViewModel", "Error preparing movie for playback", e);
                _movieActionOutcome.postValue(Resource.error("Error preparing movie for playback: " + e.getMessage(), movieToPlay));
            }
        }
    }

    public void handleMovieClickAction(Movie clickedMovie, @Nullable Movie currentMainMovie) {
        if (clickedMovie == null) return;

        // Log.d("VideoDetailsViewModel", "Handling click for: " + clickedMovie.getTitle() + ", current main: " + (currentMainMovie != null ? currentMainMovie.getTitle() : "null"));

        // If the clicked movie is a group (e.g., a season in a series, or another series)
        // then we should load its details.
        if (clickedMovie.getState() == Movie.GROUP_STATE || clickedMovie.getState() == Movie.GROUP_OF_GROUP_STATE) {
            loadMovieDetails(clickedMovie);
        }
        // If the clicked movie is an item that should be playable
        else if (clickedMovie.getState() == Movie.ITEM_STATE ||
                 clickedMovie.getState() == Movie.VIDEO_STATE ||
                 clickedMovie.getState() == Movie.RESOLUTION_STATE ||
                 (clickedMovie.getVideoUrl() != null && !clickedMovie.getVideoUrl().isEmpty())) {
            playMovieLink(clickedMovie);
        } else {
            // Log.d("VideoDetailsViewModel", "Unhandled click action for movie: " + clickedMovie.getTitle());
            // Optionally, post to _movieActionOutcome if there's a generic "cannot handle" state
             _movieActionOutcome.postValue(Resource.error("Cannot determine action for this item.", clickedMovie));
        }
    }

    public void handleActivityResult(int requestCode, @Nullable Movie returnedMovie) {
        // Log.d("VideoDetailsViewModel", "handleActivityResult: requestCode=" + requestCode + ", movie=" + (returnedMovie != null ? returnedMovie.getTitle() : "null"));
        // Example: If a cookie fix was attempted
        if (requestCode == REQUEST_CODE_COOKIE_FIX) {
            if (returnedMovie != null && moviePendingForResult != null) {
                // Potentially update moviePendingForResult with data from returnedMovie if necessary (e.g. new cookie info)
                // For simplicity, we assume returnedMovie itself might be the one to retry if it contains updated info.
                // Or, more likely, the cookie is now globally set, so just retry original pending movie.
                // Log.d("VideoDetailsViewModel", "Retrying action for movie after cookie fix: " + moviePendingForResult.getTitle());
                playMovieLink(moviePendingForResult);
            } else if (moviePendingForResult != null) {
                 _movieActionOutcome.postValue(Resource.error("Cookie fix attempt failed or was cancelled for " + moviePendingForResult.getTitle(), moviePendingForResult));
            } else {
                _movieActionOutcome.postValue(Resource.error("Cookie fix attempt failed or was cancelled.", null));
            }
            moviePendingForResult = null; // Clear pending movie
        }
        // Add more handlers for other requestCodes if needed.
        else if (returnedMovie != null) {
            // A generic handler if some other activity returns a movie.
            // Potentially refresh details or update specific UI elements if needed.
            // Log.d("VideoDetailsViewModel", "Received movie from other activity result: " + returnedMovie.getTitle());
            // loadMovieDetails(returnedMovie); // Or a more specific update.
        }
    }

    public void markAsWatched(Movie movie) {
        if (movie != null && movie.getVideoUrl() != null) {
            viewModelScope.launch {
                try {
                    movieRepository.setMovieAsHistory(movie.getVideoUrl(), System.currentTimeMillis());
                    // Only update play time if it's explicitly set (e.g., from player)
                    // if (movie.getPlayedTime() > 0) {
                    //    movieRepository.updateMoviePlayTime(movie.getVideoUrl(), movie.getPlayedTime());
                    // }
                    // Consider if mSelectedMovie or items in its sublist need their 'isHistory' flag updated
                    // for immediate UI reflection. This might involve re-fetching or manually updating _movieDetails.
                    // For now, relies on repository LiveData to eventually reflect this if the main movie is re-observed.
                } catch (Exception e) {
                    // Log.e("VideoDetailsViewModel", "Error marking movie as watched", e);
                    // Optionally notify UI of this error if important
                }
            }
        }
    }
}
