package com.omerflex.view.mobile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.repository.MovieRepository;
import com.omerflex.repository.ServerConfigRepository;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.ServerInterface;
import com.omerflex.service.ServerConfigManager;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.utils.Resource;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import androidx.annotation.Nullable; // For processCookieFixResult
import android.util.Log; // For logging

@HiltViewModel
public class MobileHomepageViewModel extends ViewModel {

    public static final int REQUEST_CODE_FIX_COOKIE = 1001;
    private Movie moviePendingCookieFix;

    private final ServerConfigRepository serverConfigRepository;
    private final MovieRepository movieRepository;
    private final ThreadPoolManager threadPoolManager;
    private final Executor networkExecutor;

    private LiveData<List<ServerConfig>> serverConfigs;

    private final MutableLiveData<String> _selectedServerId = new MutableLiveData<>();
    public final LiveData<Resource<List<Movie>>> moviesForSelectedServer;

    private final MutableLiveData<Resource<Movie>> _movieActionOutcome = new MutableLiveData<>();
    public LiveData<Resource<Movie>> getMovieActionOutcome() {
        return _movieActionOutcome;
    }

    @Inject
    public MobileHomepageViewModel(ServerConfigRepository serverConfigRepository, MovieRepository movieRepository, ThreadPoolManager threadPoolManager) {
        this.serverConfigRepository = serverConfigRepository;
        this.movieRepository = movieRepository;
        this.threadPoolManager = threadPoolManager;
        this.networkExecutor = threadPoolManager.getNetworkExecutor();

        this.serverConfigs = serverConfigRepository.getAllServerConfigs();

        this.moviesForSelectedServer = Transformations.switchMap(_selectedServerId, serverId -> {
            if (serverId == null || serverId.isEmpty()) {
                MutableLiveData<Resource<List<Movie>>> emptyResult = new MutableLiveData<>();
                emptyResult.setValue(Resource.error("Server ID is null or empty", new ArrayList<>()));
                return emptyResult;
            }
            return movieRepository.getHomepageMovies(serverId);
        });
    }

    public LiveData<List<ServerConfig>> getServerConfigs() {
        return serverConfigs;
    }

    public void setSelectedServerId(String serverId) {
        if (serverId != null && !serverId.equals(_selectedServerId.getValue())) {
            _selectedServerId.setValue(serverId);
        }
    }

    public LiveData<String> getSelectedServerId() {
        return _selectedServerId;
    }

    // Method to load movies for a specific serverId (category) - now triggers setSelectedServerId
    public void loadMoviesForServer(String serverId) {
        setSelectedServerId(serverId);
    }

    // LiveData to expose movies for the currently selected/loading category - replaced by moviesForSelectedServer
    // public LiveData<Resource<List<Movie>>> getMoviesForCategory() {
    // return moviesForCategory;
    // }

    public void handleMovieClickAction(Movie movie) {
        _movieActionOutcome.setValue(Resource.loading(movie));
        AbstractServer server = ServerConfigManager.getServer(movie.getStudio());
        if (server != null) {
            networkExecutor.execute(() -> {
                server.fetch(movie, movie.getState(), new ServerInterface.ActivityCallback<Movie>() {
                    @Override
                    public void onSuccess(Movie result, String title) {
                        if (result != null && result.getState() == Movie.COOKIE_STATE) {
                            Log.d("MobileHomepageVM", "handleMovieClickAction onSuccess but COOKE_STATE for: " + result.getTitle());
                            MobileHomepageViewModel.this.moviePendingCookieFix = result; // movie is the original parameter, result is what server.fetch returns
                        }
                        _movieActionOutcome.postValue(Resource.success(result));
                    }

                    @Override
                    public void onInvalidCookie(Movie result, String title) {
                        result.setState(Movie.COOKIE_STATE);
                        Log.d("MobileHomepageVM", "handleMovieClickAction onInvalidCookie for: " + result.getTitle());
                        MobileHomepageViewModel.this.moviePendingCookieFix = result; // result is the movie object in this callback
                        _movieActionOutcome.postValue(Resource.error("Invalid cookie for " + title, result));
                    }

                    @Override
                    public void onInvalidLink(Movie result) {
                        _movieActionOutcome.postValue(Resource.error("Invalid link", result));
                    }

                    @Override
                    public void onInvalidLink(String message) {
                        _movieActionOutcome.postValue(Resource.error(message, null));
                    }
                });
            });
        } else {
            _movieActionOutcome.setValue(Resource.error("Server not found for movie: " + movie.getStudio(), movie));
        }
    }

    public void markAsWatched(Movie movie) {
        if (movie == null || movie.getVideoUrl() == null) return;
        movieRepository.setMovieAsHistory(movie.getVideoUrl(), System.currentTimeMillis());
        if (movie.getPlayedTime() > 0) {
            movieRepository.updateMoviePlayTime(movie.getVideoUrl(), movie.getPlayedTime());
        }
    }

    // Example method to trigger a search (can be observed by UI) - unchanged for now
    public LiveData<Resource<List<Movie>>> searchMovies(String serverId, String query) {
        if (serverId == null || serverId.isEmpty()) {
            MutableLiveData<Resource<List<Movie>>> errorResult = new MutableLiveData<>();
            errorResult.setValue(Resource.error("Server ID must be selected for search", null));
            return errorResult;
        }
        return movieRepository.searchMovies(serverId, query);
    }

    public void processCookieFixResult(int resultCode, @Nullable Movie originalMovieFromIntent) {
        Movie movieToRetry = null;
        if (this.moviePendingCookieFix != null) {
            movieToRetry = this.moviePendingCookieFix;
            Log.d("MobileHomepageVM", "processCookieFixResult: Using moviePendingCookieFix: " + movieToRetry.getTitle());
        } else if (originalMovieFromIntent != null) {
            // Fallback if moviePendingCookieFix wasn't set or was cleared,
            // ensure this movie is actually the one we intended to fix.
            // This might require comparing some ID or URL if available.
            movieToRetry = originalMovieFromIntent;
            Log.d("MobileHomepageVM", "processCookieFixResult: Using originalMovieFromIntent: " + movieToRetry.getTitle());
        }

        this.moviePendingCookieFix = null; // Clear it after retrieving

        if (movieToRetry == null) {
            Log.e("MobileHomepageVM", "processCookieFixResult: No movie was identified for cookie fix retry.");
            _movieActionOutcome.postValue(Resource.error("No movie was identified for cookie fix retry.", null));
            return;
        }

        if (resultCode == android.app.Activity.RESULT_OK) {
            Log.d("MobileHomepageVM", "processCookieFixResult: RESULT_OK, retrying action for: " + movieToRetry.getTitle());
            _movieActionOutcome.postValue(Resource.loading(movieToRetry)); // Indicate retry
            // Retry the original action
            handleMovieClickAction(movieToRetry);
        } else {
            Log.d("MobileHomepageVM", "processCookieFixResult: RESULT_CANCELLED or failed for: " + movieToRetry.getTitle());
            // If user cancelled browser or it failed, post error for the original movie
            _movieActionOutcome.postValue(Resource.error("Cookie fix was cancelled or failed.", movieToRetry));
        }
    }
}
