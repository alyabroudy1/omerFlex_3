package com.omerflex.server;


import com.omerflex.entity.dto.ImdbMovieDto;
import com.omerflex.entity.dto.ImdbMovieResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApi {
    @GET("movie/popular")
    Call<ImdbMovieResponse> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    @GET("movie/{movie_id}")
    Call<ImdbMovieDto> getMovieDetails(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    @GET("tv/{series_id}")
    Call<ImdbMovieDto> getSeriesDetails(
            @Path("series_id") int seriesId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    @GET("search/multi")
    Call<ImdbMovieResponse> searchMulti(
            @Query("api_key") String apiKey,
            @Query("query") String query,
            @Query("language") String language
    );
}