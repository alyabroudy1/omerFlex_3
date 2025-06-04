package com.omerflex.service.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData; // Added for LiveData

import com.omerflex.entity.Movie;

import java.util.List;

@Dao
public interface MovieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Movie movie);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Movie> movies);

    @Update
    void update(Movie movie);

    @Delete
    void delete(Movie movie);

    @Query("SELECT * FROM movies WHERE videoUrl = :videoUrl LIMIT 1")
    LiveData<Movie> getMovieByVideoUrlLiveData(String videoUrl);

    @Query("SELECT * FROM movies")
    LiveData<List<Movie>> getAllMoviesLiveData(); // Renamed and changed for consistency, though not explicitly in task for MovieRepository

    @Query("SELECT * FROM movies WHERE isHistory = 1 AND studio != :iptvStudio AND group_column IS NULL ORDER BY updatedAt DESC")
    LiveData<List<Movie>> getHistoryMoviesNonIptvLiveData(String iptvStudio);

    @Query("SELECT * FROM movies WHERE isHistory = 1 AND (studio = :iptvStudio OR group_column = :iptvGroup) ORDER BY updatedAt DESC")
    LiveData<List<Movie>> getHistoryMoviesIptvLiveData(String iptvStudio, String iptvGroup);

    @Query("SELECT * FROM movies WHERE studio = :studio")
    List<Movie> findMoviesByStudio(String studio);

    @Query("SELECT * FROM movies WHERE studio = :studio AND (title LIKE '%' || :searchContext || '%' OR group_column LIKE '%' || :searchContext || '%')")
    List<Movie> findMovieBySearchContext(String studio, String searchContext);

    @Query("SELECT * FROM movies WHERE studio = :studio AND mainMovieTitle = :mainMovieLink")
    List<Movie> findSubListByMainMovieLink(String studio, String mainMovieLink);

    @Query("DELETE FROM movies WHERE createdAt < :timestamp AND isHistory = 0")
    void clearNonHistoryOlderThan(long timestamp);

    @Query("UPDATE movies SET playedTime = :playedTime WHERE videoUrl = :videoUrl")
    void updateMoviePlayTime(String videoUrl, long playedTime);

    @Query("UPDATE movies SET isHistory = 1, updatedAt = :timestamp WHERE videoUrl = :videoUrl")
    void setMovieAsHistory(String videoUrl, long timestamp);
}
