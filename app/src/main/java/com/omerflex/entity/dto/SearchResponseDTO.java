package com.omerflex.entity.dto;

import java.util.List;

public class SearchResponseDTO {
    public String type;
    public String title;
    public List<MovieDTO> result;

    @Override
    public String toString() {
        return "SearchResponseDTO{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", result=" + result +
                '}';
    }
}
