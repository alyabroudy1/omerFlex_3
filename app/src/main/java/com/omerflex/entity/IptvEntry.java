package com.omerflex.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "iptv_entries")
public class IptvEntry {

    private String id;
    private String url;

    @PrimaryKey
    @NonNull
    private String hash;

    public IptvEntry(@NonNull String hash, String id, String url) {
        this.hash = hash;
        this.id = id;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @NonNull
    public String getHash() {
        return hash;
    }

    public void setHash(@NonNull String hash) {
        this.hash = hash;
    }
}
