package com.omerflex.entity.dto;

public class SourceDTO {
    public int id;
    public String title;
    public int state;
    public String createdAt;
    public String updatedAt;
    public String vidoUrl;
    public ServerDTO server;

    @Override
    public String toString() {
        return "SourceDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", state=" + state +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", videoUrl='" + vidoUrl + '\'' +
                ", server=" + server +
                '}';
    }
}
