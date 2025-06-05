package com.omerflex.view;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.omerflex.entity.Movie;
import com.omerflex.entity.ServerConfig;
import com.omerflex.repository.MovieRepository;
import com.omerflex.repository.ServerConfigRepository;
import com.omerflex.server.AbstractServer; // Added
import com.omerflex.server.ServerInterface; // Added
import com.omerflex.service.ServerConfigManager; // Added
import com.omerflex.service.concurrent.ThreadPoolManager; // Added
import com.omerflex.utils.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // For IPTV
import java.util.concurrent.Executor; // Added
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import androidx.annotation.Nullable; // For processCookieFixResult
import android.util.Log; // For logging

@HiltViewModel
public class MainFragmentViewModel extends ViewModel {

    public static final int REQUEST_CODE_FIX_COOKIE = 1002; // Different from Mobile one
    private Movie moviePendingCookieFix;

    private final ServerConfigRepository serverConfigRepository;
    private final MovieRepository movieRepository;
    private final ThreadPoolManager threadPoolManager; // Added
    private final Executor networkExecutor; // Added

    public final LiveData<List<ServerConfig>> serverConfigs;

    private final MutableLiveData<String> _selectedServerId = new MutableLiveData<>();
    public final LiveData<Resource<List<Movie>>> moviesForSelectedServer; // Init in constructor

    private final MutableLiveData<Resource<Movie>> _movieActionOutcome = new MutableLiveData<>();
    public LiveData<Resource<Movie>> getMovieActionOutcome() {
        return _movieActionOutcome;
    }

    private final MutableLiveData<Resource<Map<String, List<Movie>>>> _iptvPlaylistChannels = new MutableLiveData<>();
    public LiveData<Resource<Map<String, List<Movie>>>> getIptvPlaylistChannels() { return _iptvPlaylistChannels; }


    @Inject
    public MainFragmentViewModel(ServerConfigRepository serverConfigRepository, MovieRepository movieRepository, ThreadPoolManager threadPoolManager) {
        this.serverConfigRepository = serverConfigRepository;
        this.movieRepository = movieRepository;
        this.threadPoolManager = threadPoolManager; // Init
        this.networkExecutor = threadPoolManager.getNetworkExecutor(); // Init

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

    public void setSelectedServerId(String serverId) {
        if (serverId != null && !serverId.equals(_selectedServerId.getValue())) {
            // Use postValue if there's any chance this is called from a background thread,
            // though setSelectedServerId is typically called from UI thread.
            _selectedServerId.setValue(serverId);
        }
    }

    public LiveData<String> getSelectedServerIdLiveData() {
        return _selectedServerId;
    }

    public void handleMovieClickAction(Movie movie) {
        _movieActionOutcome.setValue(Resource.loading(movie));
        AbstractServer server = ServerConfigManager.getServer(movie.getStudio());
        if (server != null) {
            networkExecutor.execute(() -> {
                server.fetch(movie, movie.getState(), new ServerInterface.ActivityCallback<Movie>() {
                    @Override
                    public void onSuccess(Movie result, String title) {
                        if (result != null && result.getState() == Movie.COOKIE_STATE) {
                            Log.d("MainFragmentVM", "handleMovieClickAction onSuccess but COOKE_STATE for: " + result.getTitle());
                            MainFragmentViewModel.this.moviePendingCookieFix = result;
                        }
                        _movieActionOutcome.postValue(Resource.success(result));
                    }

                    @Override
                    public void onInvalidCookie(Movie result, String title) {
                        result.setState(Movie.COOKIE_STATE);
                        Log.d("MainFragmentVM", "handleMovieClickAction onInvalidCookie for: " + result.getTitle());
                        MainFragmentViewModel.this.moviePendingCookieFix = result;
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

    public LiveData<Resource<List<Movie>>> searchMovies(String serverId, String query) {
        if (serverId == null || serverId.isEmpty()) {
            MutableLiveData<Resource<List<Movie>>> errorResult = new MutableLiveData<>();
            errorResult.setValue(Resource.error("Server ID must be selected for search", null));
            return errorResult;
        }
        return movieRepository.searchMovies(serverId, query);
    }

    public void loadIptvPlaylist(Movie playlistMovie) {
        if (playlistMovie == null || playlistMovie.getState() != Movie.PLAYLIST_STATE) {
            _iptvPlaylistChannels.postValue(Resource.error("Invalid movie provided for IPTV playlist", null));
            return;
        }
        _iptvPlaylistChannels.setValue(Resource.loading(null));
        // Observe the LiveData from repository to update ViewModel's LiveData
        LiveData<Resource<Map<String, List<Movie>>>> repoLiveData = movieRepository.getIptvChannels(playlistMovie);

        // Observe once and then remove observer to prevent multiple triggers if this method is called again for the same playlistMovie
        // or if the underlying LiveData from repo is not a one-shot.
        repoLiveData.observeForever(new androidx.lifecycle.Observer<Resource<Map<String, List<Movie>>>>() {
            @Override
            public void onChanged(Resource<Map<String, List<Movie>>> resource) {
                _iptvPlaylistChannels.postValue(resource);
                // It's crucial to remove the observer if repoLiveData is not a one-shot LiveData
                // or if loadIptvPlaylist can be called multiple times.
                // If repoLiveData is a new LiveData instance each time getIptvChannels is called,
                // this specific observer removal is for *this instance* of repoLiveData.
                repoLiveData.removeObserver(this);
            }
        });
    }

    public void processCookieFixResult(int resultCode, @Nullable Movie originalMovieFromIntent) {
        Movie movieToRetry = null;
        if (this.moviePendingCookieFix != null) {
            movieToRetry = this.moviePendingCookieFix;
            Log.d("MainFragmentVM", "processCookieFixResult: Using moviePendingCookieFix: " + movieToRetry.getTitle());
        } else if (originalMovieFromIntent != null) {
            // Fallback if moviePendingCookieFix wasn't set or was cleared
            movieToRetry = originalMovieFromIntent;
            Log.d("MainFragmentVM", "processCookieFixResult: Using originalMovieFromIntent: " + movieToRetry.getTitle());
        }

        this.moviePendingCookieFix = null; // Clear it after retrieving

        if (movieToRetry == null) {
            Log.e("MainFragmentVM", "processCookieFixResult: No movie was identified for cookie fix retry (TV).");
            _movieActionOutcome.postValue(Resource.error("No movie was identified for cookie fix retry (TV).", null));
            return;
        }

        if (resultCode == android.app.Activity.RESULT_OK) {
            Log.d("MainFragmentVM", "processCookieFixResult: RESULT_OK, retrying action for: " + movieToRetry.getTitle());
            _movieActionOutcome.postValue(Resource.loading(movieToRetry)); // Indicate retry
            handleMovieClickAction(movieToRetry); // Retry
        } else {
            Log.d("MainFragmentVM", "processCookieFixResult: RESULT_CANCELLED or failed for: " + movieToRetry.getTitle());
            _movieActionOutcome.postValue(Resource.error("Cookie fix was cancelled or failed (TV).", movieToRetry));
        }
    }
}
