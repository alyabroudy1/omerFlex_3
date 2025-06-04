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
import java.util.concurrent.Executor; // Added
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainFragmentViewModel extends ViewModel {

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
                        _movieActionOutcome.postValue(Resource.success(result));
                    }

                    @Override
                    public void onInvalidCookie(Movie result, String title) {
                        result.setState(Movie.COOKIE_STATE);
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
}
