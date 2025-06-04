package com.omerflex.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.omerflex.service.database.Converters;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "movie_history")
@TypeConverters(Converters.class)
public class MovieHistory implements Serializable, Parcelable {

    static final long serialVersionUID = 787566175075960653L;

    private int id;
    @PrimaryKey
    @NonNull
    private String mainMovieUrl;
    private String episode;
    private String season;
    private long playedTime;
    private Date playedAt;

    public MovieHistory() {
        this.playedAt = new Date();
    }

    protected MovieHistory(Parcel in) {
        id = in.readInt();
        mainMovieUrl = in.readString();
        episode = in.readString();
        season = in.readString();
        playedAt = new Date(in.readLong());
        playedTime = in.readLong();
    }

    public static final Creator<MovieHistory> CREATOR = new Creator<MovieHistory>() {
        @Override
        public MovieHistory createFromParcel(Parcel in) {
            return new MovieHistory(in);
        }

        @Override
        public MovieHistory[] newArray(int size) {
            return new MovieHistory[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(mainMovieUrl);
        dest.writeString(episode);
        dest.writeString(season);
        dest.writeLong(playedAt.getTime());
        dest.writeLong(playedTime);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMainMovieUrl() {
        return mainMovieUrl;
    }

    public void setMainMovieUrl(String mainMovieUrl) {
        this.mainMovieUrl = mainMovieUrl;
    }

    public String getEpisode() {
        return episode;
    }

    public void setEpisode(String episode) {
        this.episode = episode;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public long getPlayedTime() {
        return playedTime;
    }

    public void setPlayedTime(long playedTime) {
        this.playedTime = playedTime;
    }

    public Date getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Date playedAt) {
        this.playedAt = playedAt;
    }

    @Override
    public String toString() {
        return "MovieHistory{" +
                "id=" + id +
                ", mainMovieUrl='" + mainMovieUrl + '\'' +
                ", episode='" + episode + '\'' +
                ", season='" + season + '\'' +
                ", playedTime=" + playedTime +
                ", playedAt=" + playedAt +
                '}';
    }
}
