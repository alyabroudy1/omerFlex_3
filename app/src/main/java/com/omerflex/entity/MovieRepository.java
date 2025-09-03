package com.omerflex.entity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.omerflex.data.source.local.LocalDataSource;
import com.omerflex.data.source.remote.RemoteDataSource;
import com.omerflex.db.AppDatabase;
import com.omerflex.db.MovieDao;
import com.omerflex.db.MovieHistoryDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    public LiveData<Movie> getSelectedMovie() {
        return selectedMovie;
    }

    public void setSelectedMovie(Movie movie) {
        selectedMovie.postValue(movie);
    }

    public void saveMovie(Movie movie) {
        new Thread(() -> {
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
                callback.onMovieFetched(movie);
            } else {
                remoteDataSource.fetchMovieByUrl(videoUrl, remoteMovie -> {
                    if (remoteMovie != null) {
                        saveMovie(remoteMovie);
                        callback.onMovieFetched(remoteMovie);
                    } else {
                        callback.onMovieFetched(null);
                    }
                });
            }
        });
    }

    public void getHomepageMovies(final MovieListCallback callback) {
        Log.d(TAG, "getHomepageMovies: ");
        remoteDataSource.fetchHomepageMovies((remoteCategory, remoteMovies) -> {
            Log.d(TAG, "getHomepageMovies: remote: " + remoteMovies.size());
            if (remoteMovies != null && !remoteMovies.isEmpty()) {
                new Thread(() -> {
                    for (int i = 0; i < remoteMovies.size(); i++) {
                        Movie movie = remoteMovies.get(i);
                        Movie existingMovie = movieDao.getMovieByVideoUrlSync(movie.getVideoUrl());

                        if (existingMovie == null) {
                            long newId = movieDao.insert(movie);
                            movie.setId(newId);
                        } else {
                            // Replace the object in the list
                            remoteMovies.set(i, existingMovie);
                        }
                    }
                    // Now that all movies have their correct IDs, call the callback on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onMovieListFetched(remoteCategory, remoteMovies);
                    });
                }).start();
            } else {
                // Handle empty or null list by calling back on the main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onMovieListFetched(remoteCategory, remoteMovies);
                });
            }
        });
    }

    public void fetchNextPage(String url, final MovieListCallback callback) {
        localDataSource.getHomepageMovies((localCategory, movies) -> {
            if (movies != null && !movies.isEmpty()) {
                callback.onMovieListFetched(localCategory, movies);
            } else {
                // todo: save movies to local db
                remoteDataSource.fetchHomepageMovies(callback);
            }
        });
    }

    public void fetchMovieDetails(Movie mSelectedMovie, MovieCallback callback) {
        new Thread(() -> {
            boolean saveCond = mSelectedMovie.getType() == MovieType.SERIES ||
                    mSelectedMovie.getType() == MovieType.SEASON; // keep limit to season so the last sub-movies to be saved is the episodes
//                    mSelectedMovie.getType() == MovieType.EPISODE;
            if (saveCond) {
                List<Movie> localSubList = movieDao.getMoviesByParentIdSync(mSelectedMovie.getId());
                Movie localMselectedMovie = movieDao.getMovieById(mSelectedMovie.getId());

                if (localSubList != null && !localSubList.isEmpty()) {
                    Log.d(TAG, "fetchMovieDetails: found " + localSubList.size() + " sub-movies in local DB");
                    if (localMselectedMovie != null) {
                        localMselectedMovie.setSubList(localSubList);
                        new Handler(Looper.getMainLooper()).post(() -> callback.onMovieFetched(localMselectedMovie));
                    } else {
                        // Fallback or error handling if the parent movie is somehow null
                        new Handler(Looper.getMainLooper()).post(() -> callback.onMovieFetched(mSelectedMovie));
                    }
                    return;
                }
            }

            Log.d(TAG, "fetchMovieDetails: no local sub-movies found, fetching from remote");
            remoteDataSource.fetchMovieDetails(mSelectedMovie, remoteMovie -> {
                if (remoteMovie != null) {
                    new Thread(() -> {
                            movieDao.update(remoteMovie); // Update the parent movie (Series/Season)
                        if (saveCond && remoteMovie.getSubList() != null && !remoteMovie.getSubList().isEmpty()) {
                            Log.d(TAG, "fetchMovieDetails: saving/updating remote's sublist");

                            List<Movie> subListFromRemote = remoteMovie.getSubList();
                            List<Movie> finalSubList = new ArrayList<>();

                            for (Movie remoteSubMovie : subListFromRemote) {
                                Log.d(TAG, "DEBUG: Processing remote sub-movie: " + remoteSubMovie.getTitle() + " | url: " + remoteSubMovie.getVideoUrl());
                                Movie localSubMovie = movieDao.getMovieByVideoUrlSync(remoteSubMovie.getVideoUrl());

                                if (localSubMovie != null) {
                                    Log.d(TAG, "DEBUG: Found existing local movie with ID: " + localSubMovie.getId() + ". Updating it.");
                                    // Update existing movie
                                    localSubMovie.setTitle(remoteSubMovie.getTitle());
                                    localSubMovie.setDescription(remoteSubMovie.getDescription());
                                    localSubMovie.setCardImageUrl(remoteSubMovie.getCardImageUrl());
                                    localSubMovie.setBackgroundImageUrl(remoteSubMovie.getBackgroundImageUrl());
                                    localSubMovie.setVideoUrl(remoteSubMovie.getVideoUrl());
                                    // Add any other fields that need to be updated from the remote source
                                    movieDao.update(localSubMovie);
                                    finalSubList.add(localSubMovie);
                                } else {
                                    Log.d(TAG, "DEBUG: No local movie found for url: " + remoteSubMovie.getVideoUrl() + ". Inserting new one.");
                                    // Insert new movie
                                    remoteSubMovie.setParentId(remoteMovie.getId());
                                    long newId = movieDao.insert(remoteSubMovie);
                                    remoteSubMovie.setId(newId);
                                    Log.d(TAG, "DEBUG: New movie inserted with ID: " + newId);
                                    finalSubList.add(remoteSubMovie);
                                }
                            }
                            remoteMovie.setSubList(finalSubList);
                        }
                        // After processing, post the result back to the main thread
                        new Handler(Looper.getMainLooper()).post(() -> callback.onMovieFetched(remoteMovie));
                    }).start();
                } else {
                    // Remote fetch failed or returned null
                    new Handler(Looper.getMainLooper()).post(() -> callback.onMovieFetched(mSelectedMovie));
                }
            });
        }).start();
    }

    public LiveData<List<Movie>> getMoviesByType(MovieType type) {
        return movieDao.getMoviesByType(type);
    }

    public LiveData<List<Movie>> getMoviesByParentId(long parentId) {
        return movieDao.getMoviesByParentId(parentId);
    }

    public void updateWatchedTime(Long movieId, long watchedTime) {
        new Thread(() -> {
            // Ensure the movie has a valid ID from the database before proceeding.
            if (movieId == null || movieId == 0) {
                Log.d(TAG, "updateWatchedTime: Error invalid movie with id: " + movieId);
                return;
            }
            Movie movie = movieDao.getMovieById(movieId);
            if (movie == null) {
                Log.d(TAG, "updateWatchedTime: Error movie with id: " + movieId + " doesn't exist");
                return;
            }

            // Update MovieHistory for any movie type
            updateMovieHistory(movie, watchedTime);

            // Proceed with progress update only for episodes
            if (movie.getType() != MovieType.EPISODE) {
                return;
            }

            Movie episode = movie;
            // 1. Update Episode's playedTime
            long episodeLength = episode.getMovieLength();
            if (episodeLength > 0) {
                long episodePlayedTime = (watchedTime * 100) / episodeLength;
                episode.setPlayedTime(episodePlayedTime);
                episode.setUpdatedAt(new Date());
                movieDao.update(episode);
            }

            // 2. Update Parent Season's playedTime
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

                        // 3. Update Parent Series' playedTime
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
        return movieDao.getHistory();
    }

    public void deleteMovie(Movie movie) {
        new Thread(() -> movieDao.delete(movie)).start();
    }

    public void updateMovieLength(long id, long movieLength) {
        // update parent movie if id belongs to resolution
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
}