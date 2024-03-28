package com.omerflex.entity.dto;

public class ServerDTO {
    public String name;
    public String webAddress;

    @Override
    public String toString() {
        return "ServerDTO{" +
                "name='" + name + '\'' +
                ", webAddress='" + webAddress + '\'' +
                '}';
    }
}
