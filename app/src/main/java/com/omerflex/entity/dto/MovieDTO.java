package com.omerflex.entity.dto;

import java.util.List;

public class MovieDTO {
    public String type;
    public int id;
    public String title;
    public String description;
    public String cardImage;
    public String backgroundImage;
    public String rate;
    public int playedTime;
    public int totalTime;

    public String createdAt;

    public String updatedAt;
    public LinkDTO link;

    //    public List<SourceDTO> sources;
    public List<CategoryDTO> categories;
    //  public Collection categories;


    @Override
    public String toString() {
        return "MovieDTO{" +
                "type=" + type +
                "id=" + id +
                ", title='" + title + '\'' +
                ", categories='" + categories + '\'' +
                ", description='" + description + '\'' +
                ", cardImage='" + cardImage + '\'' +
                ", backgroundImage='" + backgroundImage + '\'' +
                ", rate='" + rate + '\'' +
                ", playedTime=" + playedTime +
                ", totalTime=" + totalTime +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
//                ", sources=" + sources +
                ", link =" + link.url +
                '}';
    }
}
