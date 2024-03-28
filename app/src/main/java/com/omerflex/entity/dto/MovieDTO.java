package com.omerflex.entity.dto;

import java.util.List;
import java.util.Locale;

public class MovieDTO {
    public int id;
    public int webId;
    public String title;
    public String description;
    public int state;
    public String cardImage;
    public String backgroundImage;
    public String rate;
    public int playedTime;
    public int totalTime;

    public String createdAt;

    public String updatedAt;

//    public List<SourceDTO> sources;
    public int mainMovieId;
    public int mainMovieUrl;
    public List<MovieDTO> subMovies;
    public String videoUrl;
    public String serverUrl;
    public List<CategoryDTO> categories;
    //  public Collection categories;


    @Override
    public String toString() {
        return "MovieDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", serverUrl='" + serverUrl + '\'' +
                ", categories='" + categories + '\'' +
                ", description='" + description + '\'' +
                ", state=" + state +
                ", cardImage='" + cardImage + '\'' +
                ", backgroundImage='" + backgroundImage + '\'' +
                ", rate='" + rate + '\'' +
                ", playedTime=" + playedTime +
                ", totalTime=" + totalTime +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
//                ", sources=" + sources +
                ", mainMovieId=" + mainMovieId +
                ", mainMovieUrl=" + mainMovieUrl +
                ", subMovies=" + subMovies +
                ", videoUrl='" + videoUrl + '\'' +
                '}';
    }
}
