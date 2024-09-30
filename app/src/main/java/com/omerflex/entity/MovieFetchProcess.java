package com.omerflex.entity;

public class MovieFetchProcess {


    public final static int FETCH_PROCESS_SUCCESS = 0;
    public final static int FETCH_PROCESS_COOKE_REQUIRE = 2;
    public final static int FETCH_PROCESS_BROWSER_ACTIVITY_REQUIRE = 3;
    public final static int FETCH_PROCESS_DETAILS_ACTIVITY_REQUIRE = 4;
    public final static int FETCH_PROCESS_ERROR_UNKNOWN = 5;
    public static final int FETCH_PROCESS_EXOPLAYER = 6;

    public int stateCode;
    public Movie movie;

    public MovieFetchProcess(int stateCode, Movie movie){
        this.stateCode = stateCode;
        this.movie = movie;
    }

}
