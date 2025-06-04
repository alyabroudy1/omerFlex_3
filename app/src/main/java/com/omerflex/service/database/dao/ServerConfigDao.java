package com.omerflex.service.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData; // Added for LiveData

import com.omerflex.entity.ServerConfig;

import java.util.List;

@Dao
public interface ServerConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ServerConfig serverConfig);

    @Update
    void update(ServerConfig serverConfig);

    @Query("SELECT * FROM server_configs WHERE name = :name LIMIT 1")
    LiveData<ServerConfig> getServerConfigByNameLiveData(String name);

    @Query("SELECT * FROM server_configs")
    LiveData<List<ServerConfig>> getAllServerConfigsLiveData();
}
