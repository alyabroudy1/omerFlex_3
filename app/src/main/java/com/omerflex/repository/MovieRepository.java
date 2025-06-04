package com.omerflex.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieHistory;
// For ServerConfigManager and AbstractServer, we'll use static access for now
// or assume they are passed if ServerConfigManager is refactored.
import com.omerflex.server.AbstractServer;
import com.omerflex.server.ServerInterface;
import com.omerflex.service.ServerConfigManager; // Static usage
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.database.dao.MovieDao;
import com.omerflex.service.database.dao.MovieHistoryDao;
import com.omerflex.service.database.dao.IptvDao;
import com.omerflex.utils.Resource; // Import the Resource class

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MovieRepository {
    private final MovieDao movieDao;
    private final MovieHistoryDao movieHistoryDao;
    private final IptvDao iptvDao; // Added IptvDao field
    private final ThreadPoolManager threadPoolManager;
    private final Executor diskIoExecutor;
    private final Executor networkExecutor;

    @Inject
    public MovieRepository(MovieDao movieDao, MovieHistoryDao movieHistoryDao, IptvDao iptvDao, // Added IptvDao to constructor
                         ThreadPoolManager threadPoolManager) {
        this.movieDao = movieDao;
        this.movieHistoryDao = movieHistoryDao;
        this.iptvDao = iptvDao; // Initialize IptvDao
        this.threadPoolManager = threadPoolManager;
        this.diskIoExecutor = threadPoolManager.getDiskExecutor();
        this.networkExecutor = threadPoolManager.getNetworkExecutor();
    }

    // --- DAO Methods returning LiveData ---
    public LiveData<Movie> getMovieByVideoUrl(String videoUrl) {
        return movieDao.getMovieByVideoUrlLiveData(videoUrl);
    }

    public LiveData<List<Movie>> getHistoryMoviesNonIptv(String iptvStudio) {
         return movieDao.getHistoryMoviesNonIptvLiveData(iptvStudio);
    }

    public LiveData<List<Movie>> getHistoryMoviesIptv(String iptvStudio, String iptvGroup) {
        return movieDao.getHistoryMoviesIptvLiveData(iptvStudio, iptvGroup);
    }

    public LiveData<MovieHistory> getMovieHistory(String mainMovieUrl) {
        return movieHistoryDao.getMovieHistoryByMainMovieUrlLiveData(mainMovieUrl);
    }

    // --- DAO Write Methods (Async) ---
    public void saveMovie(Movie movie) {
        diskIoExecutor.execute(() -> movieDao.insert(movie));
    }

    public void updateMovie(Movie movie) {
        diskIoExecutor.execute(() -> movieDao.update(movie));
    }

    public void saveMovieHistory(MovieHistory movieHistory) {
        diskIoExecutor.execute(() -> movieHistoryDao.insert(movieHistory));
    }

    public void setMovieAsHistory(String videoUrl, long timestamp) {
        diskIoExecutor.execute(() -> movieDao.setMovieAsHistory(videoUrl, timestamp));
    }

    public void updateMoviePlayTime(String videoUrl, long playedTime) {
        diskIoExecutor.execute(() -> movieDao.updateMoviePlayTime(videoUrl, playedTime));
    }

    // --- Remote Fetch Methods ---
    public LiveData<Resource<List<Movie>>> searchMovies(String serverId, String query) {
        MutableLiveData<Resource<List<Movie>>> resultsLiveData = new MutableLiveData<>();
        resultsLiveData.setValue(Resource.loading(null)); // Initial loading state

        AbstractServer server = ServerConfigManager.getServer(serverId); // Static access
        if (server != null) {
            networkExecutor.execute(() -> {
                server.search(query, new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
                    @Override
                    public void onSuccess(ArrayList<Movie> result, String title) {
                        resultsLiveData.postValue(Resource.success(result));
                    }
                    @Override
                    public void onInvalidCookie(ArrayList<Movie> result, String title) {
                        resultsLiveData.postValue(Resource.error("Invalid cookie for " + title, result));
                    }
                    @Override
                    public void onInvalidLink(ArrayList<Movie> result) {
                        resultsLiveData.postValue(Resource.error("Invalid link", result));
                    }
                    @Override
                    public void onInvalidLink(String message) {
                        resultsLiveData.postValue(Resource.error(message, null));
                    }
                });
            });
        } else {
            resultsLiveData.setValue(Resource.error("Server not found: " + serverId, null));
        }
        return resultsLiveData;
    }

    public LiveData<Resource<List<Movie>>> getHomepageMovies(String serverId) {
        MutableLiveData<Resource<List<Movie>>> resultsLiveData = new MutableLiveData<>();
        resultsLiveData.setValue(Resource.loading(null));
        AbstractServer server = ServerConfigManager.getServer(serverId);
        if (server != null) {
            networkExecutor.execute(() -> server.getHomepageMovies(new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
                @Override
                public void onSuccess(ArrayList<Movie> result, String title) {
                    resultsLiveData.postValue(Resource.success(result));
                }
                @Override
                public void onInvalidCookie(ArrayList<Movie> result, String title) {
                    resultsLiveData.postValue(Resource.error("Invalid Cookie for " + title, result));
                }
                @Override
                public void onInvalidLink(ArrayList<Movie> result) {
                    resultsLiveData.postValue(Resource.error("Invalid Link", result));
                }
                @Override
                public void onInvalidLink(String message) {
                    resultsLiveData.postValue(Resource.error(message, null));
                }
            }));
        } else {
             resultsLiveData.setValue(Resource.error("Server not found: " + serverId, null));
        }
        return resultsLiveData;
    }
}
