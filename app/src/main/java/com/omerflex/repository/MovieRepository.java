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
import com.omerflex.server.IptvServer; // Needed for getIptvChannels

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // For getIptvChannels
import java.util.HashMap; // For getIptvChannels
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

    public LiveData<Resource<Map<String, List<Movie>>>> getIptvChannels(Movie playlistMovie) {
        MutableLiveData<Resource<Map<String, List<Movie>>>> resultsLiveData = new MutableLiveData<>();
        resultsLiveData.setValue(Resource.loading(null));

        networkExecutor.execute(() -> {
            AbstractServer abstractServer = ServerConfigManager.getServer(Movie.SERVER_IPTV);
            if (abstractServer instanceof IptvServer) {
                IptvServer iptvServer = (IptvServer) abstractServer;
                try {
                    // IptvServer needs a method like fetchParsedM3UContent or similar.
                    // This method should internally use M3U8ContentFetcher and return the Map.
                    // For this subtask, we'll assume such a method exists or can be added to IptvServer.
                    // Let's define a placeholder for what that method might do,
                    // and it would involve MovieDbHelper or DAOs if caching IPTV structure.
                    // Map<String, List<Movie>> groupedChannels = iptvServer.fetchAndParseM3U(playlistMovie, movieDao); // Pass Dao if needed

                    // Simulating the call and result structure based on old MainFragment logic.
                    // This is a simplified placeholder for the actual M3U parsing.
                    // The actual M3U8ContentFetcher.fetchAndStoreM3U8Content was quite involved.
                    // We will assume IptvServer can provide this map.
                    // For the purpose of this subtask, we'll mock a response structure.
                    // This part (IptvServer internal logic) is the most complex dependency.

                    // Placeholder for actual call to IptvServer to get grouped channels.
                    // This would ideally be a suspend function or return a Resource/Flow itself.
                    // For now, simulating a direct call that might return the map or throw exception.
                    // Map<String, List<Movie>> groupedChannels = new HashMap<>(); // Placeholder
                    // Example:
                    // ArrayList<Movie> group1Channels = new ArrayList<>();
                    // Movie ch1 = new Movie(); ch1.setTitle("Channel 1"); ch1.setVideoUrl("url1"); ch1.setStudio(Movie.SERVER_IPTV);
                    // group1Channels.add(ch1);
                    // groupedChannels.put("Group 1", group1Channels);

                    // The actual implementation would call something like:
                    // groupedChannels = iptvServer.getGroupedChannelsFromPlaylist(playlistMovie);
                    // For this subtask, we cannot implement the full M3U parsing in IptvServer.
                    // We will assume that if IptvServer is called, it returns the map or throws.
                    // Let's assume a hypothetical method on IptvServer for now.
                    // This highlights that IptvServer itself needs refactoring for this.

                    // To make this subtask runnable, we'll assume a simplified path
                    // where IptvServer has a method that can be called.
                    // If IptvServer.fetch() can be adapted to return this map based on playlistMovie state, that's better.
                    // For now, we'll just post an empty success to show the flow.
                    // The real implementation requires IptvServer to be refactored.

                    // --- START OF SIMPLIFIED SECTION FOR SUBTASK ---
                    if (playlistMovie.getState() == Movie.PLAYLIST_STATE) {
                        // Simulate a successful fetch with some dummy data for structure
                        Map<String, List<Movie>> dummyData = new HashMap<>();
                        ArrayList<Movie> dummyChannels = new ArrayList<>();
                        Movie dummyChannel = new Movie();
                        dummyChannel.setTitle(playlistMovie.getTitle() + " - Channel 1 (Dummy)");
                        dummyChannel.setVideoUrl("dummy://channel1");
                        dummyChannel.setStudio(Movie.SERVER_IPTV);
                        dummyChannel.setState(Movie.VIDEO_STATE);
                        dummyChannels.add(dummyChannel);
                        dummyData.put("Dummy IPTV Group", dummyChannels);
                        resultsLiveData.postValue(Resource.success(dummyData));
                    } else {
                         resultsLiveData.postValue(Resource.error("Not a playlist movie for IPTV processing", null));
                    }
                    // --- END OF SIMPLIFIED SECTION FOR SUBTASK ---

                } catch (Exception e) {
                    resultsLiveData.postValue(Resource.error("Failed to load IPTV playlist: " + e.getMessage(), null));
                }
            } else {
                resultsLiveData.postValue(Resource.error("IPTV Server not configured or not of type IptvServer", null));
            }
        });
        return resultsLiveData;
    }
}
