package com.omerflex.server;

import com.omerflex.entity.Movie;

import java.util.ArrayList;

public interface ServerInterface {
    public ArrayList<Movie> search(String query);
    public Movie fetch(Movie movie);
    public int fetchNextAction(Movie movie);
}
