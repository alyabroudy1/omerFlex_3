package com.omerflex.service.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.omerflex.entity.IptvEntry;

import java.util.List;

@Dao
public interface IptvDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(IptvEntry iptvEntry);

    @Query("SELECT * FROM iptv_entries WHERE hash = :hash LIMIT 1")
    IptvEntry getIptvEntryByHash(String hash);

    @Query("SELECT * FROM iptv_entries WHERE id = :id")
    List<IptvEntry> getIptvEntriesById(String id);
}
