package com.omerflex.entity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.omerflex.data.source.local.LocalDataSource;
import com.omerflex.data.source.remote.RemoteDataSource;
import com.omerflex.db.AppDatabase;
import com.omerflex.db.MovieDao;
import com.omerflex.db.MovieHistoryDao;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.ServerFactory;
import com.omerflex.server.ServerInterface;
import com.omerflex.server.config.ServerConfigRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieRepository {
    private static final String TAG = "MovieRepository";
    private static MovieRepository instance;
    private final LocalDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;
    private final MutableLiveData<Movie> selectedMovie = new MutableLiveData<>();
    private final MovieDao movieDao;
    private final MovieHistoryDao movieHistoryDao;

    private MovieRepository(Context context, MovieDao movieDao) {
        localDataSource = LocalDataSource.getInstance(context);
        remoteDataSource = RemoteDataSource.getInstance();
        this.movieDao = movieDao;
        this.movieHistoryDao = AppDatabase.getDatabase(context).movieHistoryDao();
    }

    public static synchronized MovieRepository getInstance(Context context, MovieDao movieDao) {
        if (instance == null) {
            instance = new MovieRepository(context.getApplicationContext(), movieDao);
        }
        return instance;
    }

    private String removeDomain(String url) {
        if (url == null || !url.startsWith("http")) {
            return url;
        }
        int doubleSlash = url.indexOf("//");
        if (doubleSlash == -1) {
            return url;
        }
        int nextSlash = url.indexOf('/', doubleSlash + 2);
        if (nextSlash == -1) {
            return "/"; // URL is just the domain
        }
        return url.substring(nextSlash);
    }

    private void reAddDomainToMovie(Movie movie) {
        if (movie != null && movie.getVideoUrl() != null && !movie.getVideoUrl().startsWith("http")) {
            ServerConfig config = ServerConfigRepository.getInstance().getConfig(movie.getStudio());
            if (config != null) {
                movie.setVideoUrl(config.getUrl() + movie.getVideoUrl());
            }
        }
    }

    private void reAddDomainToMovies(List<Movie> movies) {
        if (movies != null) {
            for (Movie movie : movies) {
                reAddDomainToMovie(movie);
            }
        }
    }

    public LiveData<Movie> getSelectedMovie() {
        return selectedMovie;
    }

    public void setSelectedMovie(Movie movie) {
        selectedMovie.postValue(movie);
    }

    public void saveMovie(Movie movie) {
        new Thread(() -> {
            movie.setVideoUrl(removeDomain(movie.getVideoUrl()));
            long id = movieDao.insert(movie);
            movie.setId(id);
            if (movie.getMovieHistory() != null) {
                movie.getMovieHistory().setMovieId(movie.getId());
                movieHistoryDao.insert(movie.getMovieHistory());
            }
        }).start();
    }

    public void getMovieByUrl(String videoUrl, MovieCallback callback) {
        localDataSource.getMovieByUrl(videoUrl, movie -> {
            if (movie != null) {
                reAddDomainToMovie(movie);
                callback.onMovieFetched(movie);
            } else {
                remoteDataSource.fetchMovieByUrl(videoUrl, remoteMovie -> {
                    if (remoteMovie != null) {
                        saveMovie(remoteMovie);
                        reAddDomainToMovie(remoteMovie);
                        callback.onMovieFetched(remoteMovie);
                    } else {
                        callback.onMovieFetched(null);
                    }
                });
            }
        });
    }

    public void getHomepageMovies(boolean handleCookie, final MovieListCallback callback) {
        Log.d(TAG, "getHomepageMovies: ");
        remoteDataSource.fetchHomepageMovies(handleCookie, (remoteCategory, remoteMovies) -> {
            Log.d(TAG, "getHomepageMovies: remote: " + remoteMovies.size());
            if (remoteMovies != null && !remoteMovies.isEmpty()) {
                new Thread(() -> {
                    boolean isIpTv = remoteMovies.get(0).getStudio().equals(Movie.SERVER_IPTV);
                    for (int i = 0; i < remoteMovies.size(); i++) {
                        Movie movie = remoteMovies.get(i);
                        String videoUrl = isIpTv ? movie.getVideoUrl() : removeDomain(movie.getVideoUrl());
                        movie.setVideoUrl(videoUrl);
                        Movie existingMovie = movieDao.getMovieByVideoUrlSync(movie.getVideoUrl());

                        if (existingMovie == null) {
                            long newId = movieDao.insert(movie);
                            movie.setId(newId);
                        } else {
                            remoteMovies.set(i, existingMovie);
                        }
                    }
                    reAddDomainToMovies(remoteMovies);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onMovieListFetched(remoteCategory, remoteMovies);
                    });
                }).start();
            } else {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onMovieListFetched(remoteCategory, remoteMovies);
                });
            }
        });
    }

    public void saveIptvMovies(HashMap<String, ArrayList<Movie>> iptvMovies, final IptvMovieListCallback callback) {
        new Thread(() -> {
            for (Map.Entry<String, ArrayList<Movie>> entry : iptvMovies.entrySet()) {
                ArrayList<Movie> movies = entry.getValue();
                for (int i = 0; i < movies.size(); i++) {
                    Movie movie = movies.get(i);
                    Movie existingMovie = movieDao.getMovieByVideoUrlSync(movie.getVideoUrl());
                    if (existingMovie == null) {
                        long newId = movieDao.insert(movie);
                        movie.setId(newId);
                    } else {
                        movies.set(i, existingMovie);
                    }
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onIptvMovieListFetched(iptvMovies);
            });
        }).start();
    }

    public void fetchNextPage(String url, final MovieListCallback callback) {
        localDataSource.getHomepageMovies((localCategory, movies) -> {
            if (movies != null && !movies.isEmpty()) {
                reAddDomainToMovies(movies);
                callback.onMovieListFetched(localCategory, movies);
            } else {
                remoteDataSource.fetchHomepageMovies(false, callback);
            }
        });
    }

    public void fetchMovieDetails(Movie mSelectedMovie, ServerInterface.ActivityCallback<Movie> callback) {
        new Thread(() -> {
            reAddDomainToMovie(mSelectedMovie);
            boolean saveCond = mSelectedMovie.getType() == MovieType.SERIES ||
                    mSelectedMovie.getType() == MovieType.SEASON;

            // Commenting out the local data check in fetchMovieDetails created a loop for the following reason:
            //
            //   1. UI Requests Data: The details screen (VideoDetailsFragmentController) requests movie details from the MovieRepository to display them.
            //
            //   2. Local Cache Skipped: With the local check commented out, the MovieRepository no longer looks in the local database first. It jumps directly to fetching the data from the remote server every single time
            //      fetchMovieDetails is called.
            //
            //   3. Remote Fetch and DB Save: The app successfully fetches the data from the server, and as part of the success callback, it saves or updates that data in the local database.
            //
            //   4. UI Update: After saving, it notifies the UI (the VideoDetailsFragmentController) that the data has been fetched. The controller then updates the SharedViewModel with this new data.
            //
            //   5. The Loop: The UI is designed to be reactive. When the SharedViewModel is updated with new movie details, the UI observes this change and automatically refreshes itself to display the new data. This
            //      refresh, in turn, triggers the initial data request (step 1) all over again.
            if (saveCond) {
                List<Movie> localSubList = movieDao.getMoviesByParentIdSync(mSelectedMovie.getId());
                Movie localMselectedMovie = movieDao.getMovieById(mSelectedMovie.getId());

                if (localSubList != null && !localSubList.isEmpty()) {
                    Log.d(TAG, "fetchMovieDetails: found " + localSubList.size() + " sub-movies in local DB");
                    reAddDomainToMovies(localSubList);
                    reAddDomainToMovie(localMselectedMovie);
                    if (localMselectedMovie != null) {
                        localMselectedMovie.setSubList(localSubList);
                        Movie finalLocalMselectedMovie = localMselectedMovie;
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(finalLocalMselectedMovie, finalLocalMselectedMovie.getTitle()));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(mSelectedMovie, mSelectedMovie.getTitle()));
                    }
                    return;
                }
            }

            Log.d(TAG, "fetchMovieDetails: no local sub-movies found, fetching from remote");
            remoteDataSource.fetchMovieDetails(mSelectedMovie, new ServerInterface.ActivityCallback<Movie>() {
                @Override
                public void onSuccess(Movie remoteMovie, String title) {
                    if (remoteMovie != null) {
                        new Thread(() -> {
                            remoteMovie.setVideoUrl(removeDomain(remoteMovie.getVideoUrl()));
                            movieDao.update(remoteMovie);
                            if (saveCond && remoteMovie.getSubList() != null && !remoteMovie.getSubList().isEmpty()) {
                                List<Movie> subListFromRemote = remoteMovie.getSubList();
                                List<Movie> finalSubList = new ArrayList<>();
                                for (Movie remoteSubMovie : subListFromRemote) {
                                    remoteSubMovie.setVideoUrl(removeDomain(remoteSubMovie.getVideoUrl()));
                                    Movie localSubMovie = movieDao.getMovieByVideoUrlSync(remoteSubMovie.getVideoUrl());
                                    if (localSubMovie != null) {
                                        localSubMovie.setTitle(remoteSubMovie.getTitle());
                                        localSubMovie.setDescription(remoteSubMovie.getDescription());
                                        localSubMovie.setCardImageUrl(remoteSubMovie.getCardImageUrl());
                                        localSubMovie.setBackgroundImageUrl(remoteSubMovie.getBackgroundImageUrl());
                                        localSubMovie.setVideoUrl(remoteSubMovie.getVideoUrl());
                                        movieDao.update(localSubMovie);
                                        finalSubList.add(localSubMovie);
                                    } else {
                                        remoteSubMovie.setParentId(remoteMovie.getId());
                                        long newId = movieDao.insert(remoteSubMovie);
                                        remoteSubMovie.setId(newId);
                                        finalSubList.add(remoteSubMovie);
                                    }
                                }
                                remoteMovie.setSubList(finalSubList);
                            }
                            reAddDomainToMovie(remoteMovie);
                            reAddDomainToMovies(remoteMovie.getSubList());
                            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(remoteMovie, title));
                        }).start();
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(mSelectedMovie, mSelectedMovie.getTitle()));
                    }
                }

                @Override
                public void onInvalidCookie(Movie result, String title) {
                    Log.d(TAG, "onInvalidCookie: MovieRepo fetchMovieDetails");
                    new Handler(Looper.getMainLooper()).post(() -> callback.onInvalidCookie(result, title));
                }

                @Override
                public void onInvalidLink(Movie result) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onInvalidLink(result));
                }

                @Override
                public void onInvalidLink(String message) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onInvalidLink(message));
                }
            });
        }).start();
    }

    public LiveData<List<Movie>> getMoviesByType(MovieType type) {
        return Transformations.map(movieDao.getMoviesByType(type), movies -> {
            reAddDomainToMovies(movies);
            return movies;
        });
    }

    public LiveData<List<Movie>> getMoviesByParentId(long parentId) {
        return Transformations.map(movieDao.getMoviesByParentId(parentId), movies -> {
            reAddDomainToMovies(movies);
            return movies;
        });
    }

    public void updateWatchedTime(Long movieId, long watchedTime) {
        new Thread(() -> {
            if (movieId == null || movieId == 0) {
                Log.d(TAG, "updateWatchedTime: Error invalid movie with id: " + movieId);
                return;
            }
            Movie movie = movieDao.getMovieById(movieId);
            if (movie == null) {
                Log.d(TAG, "updateWatchedTime: Error movie with id: " + movieId + " doesn't exist");
                return;
            }

            updateMovieHistory(movie, watchedTime);

            if (movie.getType() != MovieType.EPISODE) {
                return;
            }

            Movie episode = movie;
            long episodeLength = episode.getMovieLength();
            if (episodeLength > 0) {
                long episodePlayedTime = (watchedTime * 100) / episodeLength;
                episode.setPlayedTime(episodePlayedTime);
                episode.setUpdatedAt(new Date());
                movieDao.update(episode);
            }

            Long seasonId = episode.getParentId();
            if (seasonId != null && seasonId != 0) {
                Movie season = movieDao.getMovieById(seasonId);
                if (season != null && season.getType() == MovieType.SEASON) {
                    List<Movie> episodes = movieDao.getMoviesByParentIdSync(seasonId);
                    if (episodes != null && !episodes.isEmpty()) {
                        long totalPlayedTime = 0;
                        for (Movie ep : episodes) {
                            totalPlayedTime += ep.getPlayedTime();
                        }
                        long seasonPlayedTime = totalPlayedTime / episodes.size();
                        season.setPlayedTime(seasonPlayedTime);
                        season.setUpdatedAt(new Date());
                        movieDao.update(season);

                        Long seriesId = season.getParentId();
                        if (seriesId != null && seriesId != 0) {
                            Movie series = movieDao.getMovieById(seriesId);
                            if (series != null && series.getType() == MovieType.SERIES) {
                                List<Movie> seasons = movieDao.getMoviesByParentIdSync(seriesId);
                                if (seasons != null && !seasons.isEmpty()) {
                                    long totalSeasonPlayedTime = 0;
                                    for (Movie s : seasons) {
                                        totalSeasonPlayedTime += s.getPlayedTime();
                                    }
                                    long seriesPlayedTime = totalSeasonPlayedTime / seasons.size();
                                    series.setPlayedTime(seriesPlayedTime);
                                    series.setUpdatedAt(new Date());
                                    movieDao.update(series);
                                }
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private void updateMovieHistory(Movie movie, long watchedTime) {
        MovieHistory movieHistory = movieHistoryDao.getMovieHistoryByMovieIdSync(movie.getId());
        if (movieHistory == null) {
            movieHistory = new MovieHistory(movie.getId(), watchedTime, new Date());
        } else {
            movieHistory.setWatchedPosition(watchedTime);
            movieHistory.setLastWatchedDate(new Date());
        }
        movieHistoryDao.insert(movieHistory);
    }

    public void saveMovieHistory(MovieHistory movieHistory) {
        new Thread(() -> movieHistoryDao.insert(movieHistory)).start();
    }

    public LiveData<MovieHistory> getMovieHistoryByMovieIdLive(long movieId) {
        return movieHistoryDao.getMovieHistoryByMovieIdLive(movieId);
    }

    public MovieHistory getMovieHistoryByMovieIdSync(long movieId) {
        return movieHistoryDao.getMovieHistoryByMovieIdSync(movieId);
    }

    public LiveData<List<Movie>> getHistory() {
        return Transformations.map(movieDao.getHistory(), movies -> {
            reAddDomainToMovies(movies);
            return movies;
        });
    }

    public void deleteMovie(Movie movie) {
        new Thread(() -> movieDao.delete(movie)).start();
    }

    public void updateMovieLength(long id, long movieLength) {
        new Thread(() -> {
            movieDao.updateMovieLength(id, movieLength);
            Movie movie = movieDao.getMovieById(id);
            if (movie != null && movie.getType() == MovieType.RESOLUTION) {
                Long parentId = movie.getParentId();
                if (parentId != null && parentId != 0) {
                    movieDao.updateMovieLength(parentId, movieLength);
                }
            }
        }).start();
    }


    public interface MovieCallback {
        void onMovieFetched(Movie movie);
    }

    public interface MovieListCallback {
        void onMovieListFetched(String category, ArrayList<Movie> movies);
    }

    public interface IptvMovieListCallback {
        void onIptvMovieListFetched(HashMap<String, ArrayList<Movie>> movies);
    }
}