package com.omerflex.service.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData; // Added for LiveData

import com.omerflex.entity.MovieHistory;

@Dao
public interface MovieHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MovieHistory movieHistory);

    @Update
    void update(MovieHistory movieHistory);

    @Query("SELECT * FROM movie_history WHERE mainMovieUrl = :mainMovieUrl LIMIT 1")
    LiveData<MovieHistory> getMovieHistoryByMainMovieUrlLiveData(String mainMovieUrl);

    @Delete
    void delete(MovieHistory movieHistory);
}
