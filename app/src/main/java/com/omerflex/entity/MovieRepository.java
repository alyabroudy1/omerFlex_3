package com.omerflex.entity;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MovieRepository {
    private static final String TAG = "MovieRepository";
    private static MovieRepository instance;
    private final DatabaseReference databaseReference;

    // mSelectedMovie first initialized at the item click on the main fragment or the search result fragment
    private Movie mSelectedMovie = null;

    private MovieRepository() {
        databaseReference = FirebaseDatabase.getInstance().getReference("movies");
    }

    public static synchronized MovieRepository getInstance() {
        if (instance == null) {
            instance = new MovieRepository();
        }
        return instance;
    }

    public Movie getSelectedMovie() {
        return mSelectedMovie;
    }

    public void setSelectedMovie(Movie movie) {
        mSelectedMovie = movie;
        Log.d(TAG, "setSelectedMovie: "+ movie.getTitle());
    }

    private String encodeUrl(String url) {
        return Base64.encodeToString(url.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public void saveMovie(Movie movie) {
        if (movie == null || movie.getVideoUrl() == null) {
            Log.e(TAG, "Movie or video URL is null");
            return;
        }

        String key = encodeUrl(movie.getVideoUrl());

        databaseReference.child(key).setValue(movie)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Movie saved: " + movie.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving movie", e));
    }

    public void getMovieByUrl(String videoUrl, MovieCallback callback) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.e(TAG, "Video URL is null or empty");
            return;
        }

        String key = encodeUrl(videoUrl);

        databaseReference.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Movie movie = snapshot.getValue(Movie.class);
                callback.onMovieFetched(movie);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch movie", error.toException());
                callback.onMovieFetched(null);
            }
        });
    }

    public void getHomepageMovies(final MovieListCallback callback) {
        Log.d(TAG, "getHomepageMovies: Using dummy data for testing.");

        ArrayList<Movie> movies =(ArrayList<Movie>) generateDummyMovies();

        // The callback is called immediately with the dummy data.
        if (callback != null) {
            callback.onMovieListFetched(movies);
        }
    }

    public void searchMovies(String query, final MovieListCallback callback) {
        Log.d(TAG, "searchMovies: Using dummy data for testing with query: " + query);

        ArrayList<Movie> allMovies =(ArrayList<Movie>) generateDummyMovies();

        if (callback != null) {
            callback.onMovieListFetched(allMovies);
        }
    }


    /**
     * Private helper method to generate a list of mock movies.
     * @return A list of hardcoded Movie objects with real image URLs.
     */
    /**
     * Private helper method to generate a list of mock movies.
     * @return A list of hardcoded Movie objects with real image URLs and a "next page" movie.
     */
    private List<Movie> generateDummyMovies() {
        List<Movie> dummyMovies = new ArrayList<>();
        // Using a placeholder image for the card to ensure it loads correctly.
        String placeholderCardUrl = "data:image/png;base64,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";

        dummyMovies.add(new Movie(
                "series",
                "Inception dream mind heist",
                "Inception",
                "Warner Bros.",
                Movie.GROUP_OF_GROUP_STATE,
                "A thief who steals corporate secrets through the use of dream-sharing technology...",
                "https://image.tmdb.org/t/p/w500/8c728eG8i7v0I1Xf9R9E5b1h7P4.jpg", // bgImageUrl
                placeholderCardUrl, // cardImageUrl is now a placeholder
                "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Inception/Inception.mp4", // videoUrl
                "8.8",
                "https://www.youtube.com/watch?v=YoV436yA778", // trailerUrl
                0,
                0,
                "2010-07-16",
                new Date(),
                0,
                "https://image.tmdb.org/t/p/original/8c728eG8i7v0I1Xf9R9E5b1h7P4.jpg", // backgroundImageUrl
                "Science Fiction"
        ));
        // Dummy data for "Inception" with real images
        dummyMovies.add(new Movie(
                "cookie",
                "Inception dream mind heist",
                "Inception",
                "Warner Bros.",
                Movie.COOKIE_STATE,
                "A thief who steals corporate secrets through the use of dream-sharing technology...",
                "https://image.tmdb.org/t/p/w500/8c728eG8i7v0I1Xf9R9E5b1h7P4.jpg", // bgImageUrl
                placeholderCardUrl, // cardImageUrl is now a placeholder
                "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Inception/Inception.mp4", // videoUrl
                "8.8",
                "https://www.youtube.com/watch?v=YoV436yA778", // trailerUrl
                0,
                0,
                "2010-07-16",
                new Date(),
                0,
                "https://image.tmdb.org/t/p/original/8c728eG8i7v0I1Xf9R9E5b1h7P4.jpg", // backgroundImageUrl
                "Science Fiction"
        ));

        // Dummy data for "The Matrix" with real images
        dummyMovies.add(new Movie(
                "web",
                "The Matrix Neo cyber reality",
                "The Matrix",
                "Warner Bros.",
                Movie.BROWSER_STATE,
                "A computer hacker learns from mysterious rebels about the true nature of his reality...",
                "https://image.tmdb.org/t/p/w500/pSYgP4vB8y5tW4f53E5j0v2J7Ff.jpg", // bgImageUrl
                placeholderCardUrl, // cardImageUrl is now a placeholder
                "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/The%20Matrix/The%20Matrix.mp4", // videoUrl
                "8.7",
                "https://www.youtube.com/watch?v=vKQi3bBA1y8", // trailerUrl
                0,
                0,
                "1999-03-31",
                new Date(),
                0,
                "https://image.tmdb.org/t/p/original/pSYgP4vB8y5tW4f53E5j0v2J7Ff.jpg", // backgroundImageUrl
                "Science Fiction"
        ));

        // Dummy data for "Interstellar" with real images
        dummyMovies.add(new Movie(
                "video",
                "Interstellar space travel wormhole",
                "Interstellar",
                "Paramount Pictures",
                Movie.VIDEO_STATE,
                "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                "https://image.tmdb.org/t/p/w500/wz7xYVv33w3zKkXy60K1d1g4WvE.jpg", // bgImageUrl
                placeholderCardUrl, // cardImageUrl is now a placeholder
                "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Interstellar/Interstellar.mp4", // videoUrl
                "8.6",
                "https://www.youtube.com/watch?v=0vxX92301nU", // trailerUrl
                0,
                0,
                "2014-11-07",
                new Date(),
                0,
                "https://image.tmdb.org/t/p/original/wz7xYVv33w3zKkXy60K1d1g4WvE.jpg", // backgroundImageUrl
                "Science Fiction"
        ));

        // New movie to test the adapter extension logic
        dummyMovies.add(new Movie(
                "Next Page",
                "Click here to load more movies",
                "Next Page",
                "Test Studio",
                Movie.NEXT_PAGE_STATE, // Set state to NEXT_PAGE_STATE
                "A special movie item that triggers loading the next page of content.",
                "https://placehold.co/1920x1080/4F46E5/FFFFFF?text=Next+Page+Background", // bgImageUrl
                "https://placehold.co/500x750/4F46E5/FFFFFF?text=Next+Page", // cardImageUrl
                "https://example.com/api/next-page", // This URL is used by the logic to fetch the next page
                null,
                null,
                0,
                0,
                null,
                new Date(),
                0,
                "https://placehold.co/1920x1080/4F46E5/FFFFFF?text=Next+Page+Background",
                null
        ));

        // New movie to test the adapter extension logic
        dummyMovies.add(new Movie(
                "iptv",
                "Click here to load more rows",
                "iptv",
                "Test Studio",
                Movie.IPTV_PLAY_LIST_STATE, // Set state to IPTV_PLAY_LIST
                "A special movie item that triggers loading the next page of content.",
                "https://placehold.co/1920x1080/4F46E5/FFFFFF?text=Next+Page+Background", // bgImageUrl
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAOEAAADhCAMAAAAJbSJIAAAAclBMVEUkeMD///8AbLvz9voFcL0Wc7690um70+mDrNeBrdcfdr/q8fgAbrw9gsTw9vscdb/i7fZDisifv+CNsNi0zOZ8qNWTt9xdls0rfsN/qtamxOI6hcbR4fDd6fTI2+60zudqntBXkstuodLW5PLM3e5Njsk2lpnPAAAFBUlEQVR4nO2a23qqMBBGAybVRhTP1bo9VNv3f8WNonaGjF8Tkmov/nUnYJIFOUwGlAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADgD5ILFJdzWjyptTGWF1JKF4roRqnlnWZZn4u8GGQdl1GtqGfuqWF/s91N9mVOJcu5VIrITvNS53Lr7RcpMbtzkRd2kAkM89rwRTpZM5qs9a1es7p/YYOuLt/o720htku/k2s6g/aC7Q1PjfvQNtjwRau8Q6tSVmpXPqIV6ScZZtnsohhmqGf0wF7qgXZNL4nppJGG2VGV4YblBz0wkx6QWdBLSvExP8YwG6lwQ1vSLjiS2l/8o3+Rh+qDDOsxEmbIp5FsLBgO6FB9i1osog2zuQk25N10YpxWVavPNx33fBDRhp1qtjGv3obd8zOn3XTjdkJWsThQQxiPx+stKXCxro5Yt6Lz8fFbb9ZvtHllqns+7FOG9A7wM+f26gktYNlsklW0gI+oTnoqztqCmrzq6ohyDXun47Y02rzSQZJln9UzqI4TNO20Lzk9VU8rVcRCmDa7IYsJRjZmJlWCyauRj/eux60eM8WO8wxMjxpKMyFb0D+bV7CZ6D22k4YbVgZ79hCduY4b/rjeObeooAMhupO2MVT5JpP/4WuolrSAA5dgAc0oxSMMN+QhhzPdexjq4/1LWOnCWvIIQ749cKZzD0MzJZf0efTNesggwTzTwpAHxo6DhyGPNNhIZqc2eQrBFoasfW0MlaaRJ5svWfjgrCSPMhxTwza9lHuw6aSg8YezEj3IsDxQw0WLmaaaTemiurbyic8kM2kLQ76HPYSvh6rxqMhNKuliO0/TSYMNrWZBjbP98TJk+4fj94RC715UgibGMGeB88ZR8DK0hhZyG2+2JAFNVIKmvWGp+V7JGYZ+hkp3xdJpUO70/981LOp9Q/FFW1bhJsv8DFl0e3taNOruRCVogg130xOLHd0VnJi4Bn6G1pLRfEsq0l1HXIIm2PAOfe3eZz9DXvy+rtbSkDwuQZPK8Etohachi24vYQONujtpIrZIw4O0YHkaWkWmzctWnqYRoxM0KQz38sbBz5AHDvWqSgOaJHvfOMP+WA45fA1ZNz1vBGkYIKaKH2v4bu/cZF9DpUk3Pe+TaI3CJP1Yw91YmEVDDWnKadkYmQk7aQvD7Urd9QswZLuwaitIu22aBE2I4bF7Yjd7XxzWedF8zc3wNlQ5eWb/Cjb1uMFgBF6Gq1zXmB9nAH9DQ2P4pdUkoFmnm2da7A9/wN+QddO5IemfY7rlXj3TUBmSV5vlJKBJlKC58ExDIjXKybZffr3flicasm5KAppUCZoLTzRUBUl+k4AmVYLmwjMNafL7e2vtvs9qj6327TlbFaq9/CWaeIQhf0fj+6cA7HLa663oa5JZrzft1YoPMcxp8vtKsgTNvS8VOsJ7/N8yZBdf60/x3veC/7cYv2VorVv/LlmC5i8YKr1tVp8wQfMnDFnym1afhj9gaJfDRvVJPk64Ff98Q3cbKuXvWvMXDPm7urQJmpDVIiDYZ28/fza0in+ClDJBcxoE267L7pqeJcfEL11lyjfyP4+9Oqun25U+VozAaolr1eRQyNgoyf98Hr1hlacVBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAODX+A8+5kabQ41kXgAAAABJRU5ErkJggg==", // cardImageUrl
                "https://example.com/api/next-page", // This URL is used by the logic to fetch the next page
                null,
                null,
                0,
                0,
                null,
                new Date(),
                0,
                "https://placehold.co/1920x1080/4F46E5/FFFFFF?text=Next+Page+Background",
                null
        ));

        return dummyMovies;
    }

    public void getHomepageMovie_olds(MovieListCallback callback) {
        Log.d(TAG, "getHomepageMovies called.");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange triggered. Snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Snapshot children count: " + snapshot.getChildrenCount());

                ArrayList<Movie> movies = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Log.d(TAG, "Processing child: " + dataSnapshot.getKey());

                    // Get the raw data as a map to see if it's correct
                    Object rawData = dataSnapshot.getValue();
                    Log.d(TAG, "Raw child data: " + rawData);

                    Movie movie = dataSnapshot.getValue(Movie.class);
                    if (movie != null) {
                        movies.add(movie);
                        Log.d(TAG, "Successfully created Movie object for " + movie.getTitle());
                    } else {
                        Log.w(TAG, "Failed to create Movie object from " + dataSnapshot.getKey());
                    }
                }
                Log.d(TAG, "Final movie list size: " + movies.size());
                callback.onMovieListFetched(movies);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch movie", error.toException());
                callback.onMovieListFetched(null);
            }
        });
    }

    public void fetchNextPage(String videoUrl, MovieListCallback callback) {
        Log.d(TAG, "getHomepageMovies: Using dummy data for testing.");

        ArrayList<Movie> movies =(ArrayList<Movie>) generateDummyMovies();

        // The callback is called immediately with the dummy data.
        if (callback != null) {
            callback.onMovieListFetched(movies);
        }
    }

    public interface MovieCallback {
        void onMovieFetched(Movie movie);
    }

    public interface MovieListCallback {
        void onMovieListFetched(ArrayList<Movie> movies);
    }
}
