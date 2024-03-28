package com.omerflex.entity.dto;

public class ServerConfig {

    public String name;
    public String displayName;
    public  String url;
    public  String webName;
    public boolean isActive;
    public String description;
    public String date;

    @Override
    public String toString() {
        return "ServerConfig{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", url='" + url + '\'' +
                ", webName='" + webName + '\'' +
                ", isActive=" + isActive +
                ", description='" + description + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
