package com.omerflex.view.mobile.entity;

// Category.java

import com.omerflex.entity.Movie;

import java.util.List;

public class Category {
    private String categoryName;
    private List<Movie> movieList;

    // Constructor, getters, and setters
    public Category(String categoryName, List<Movie> movieList) {
        this.categoryName = categoryName;
        this.movieList = movieList;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<Movie> getMovieList() {
        return movieList;
    }
}

