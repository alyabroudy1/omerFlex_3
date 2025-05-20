package com.omerflex.entity;

import android.os.Parcel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MovieTest {

    private Movie movie;

    @Mock
    private Parcel parcel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        movie = new Movie();
    }

    @Test
    public void testMovieConstructor() {
        // Verify that a new Movie instance is properly initialized
        assertNotNull(movie);
        assertNotNull(movie.getSubList());
        assertNotNull(movie.getCategories());
        assertNotNull(movie.getCreatedAt());
        assertNotNull(movie.getUpdatedAt());
        assertEquals("", movie.getRate());
        assertEquals(1, movie.getFetch());
    }

    @Test
    public void testSetAndGetProperties() {
        // Test setting and getting basic properties
        movie.setId(1);
        movie.setTitle("Test Movie");
        movie.setDescription("Test Description");
        movie.setVideoUrl("http://example.com/video.mp4");
        movie.setCardImageUrl("http://example.com/image.jpg");
        movie.setBgImageUrl("http://example.com/bg.jpg");
        movie.setStudio("Test Studio");
        movie.setState(Movie.ITEM_STATE);

        // Verify the properties were set correctly
        assertEquals(1, movie.getId());
        assertEquals("Test Movie", movie.getTitle());
        assertEquals("Test Description", movie.getDescription());
        assertEquals("http://example.com/video.mp4", movie.getVideoUrl());
        assertEquals("http://example.com/image.jpg", movie.getCardImageUrl());
        assertEquals("http://example.com/bg.jpg", movie.getBgImageUrl());
        assertEquals("Test Studio", movie.getStudio());
        assertEquals(Movie.ITEM_STATE, movie.getState());
    }

    @Test
    public void testCategories() {
        // Test adding and retrieving categories
        List<String> categories = Arrays.asList("Action", "Drama", "Thriller");
        movie.setCategories(categories);

        assertEquals(categories, movie.getCategories());

        // Test adding a single category
        movie.setCategories(new ArrayList<>());
        movie.addCategory("Comedy");

        assertTrue(movie.getCategories().contains("Comedy"));
        assertEquals(1, movie.getCategories().size());
    }

    @Test
    public void testSubList() {
        // Test adding and retrieving sublist items
        Movie subMovie1 = new Movie();
        subMovie1.setTitle("Sub Movie 1");

        Movie subMovie2 = new Movie();
        subMovie2.setTitle("Sub Movie 2");

        movie.addSubList(subMovie1);
        movie.addSubList(subMovie2);

        assertEquals(2, movie.getSubList().size());
        assertEquals("Sub Movie 1", movie.getSubList().get(0).getTitle());
        assertEquals("Sub Movie 2", movie.getSubList().get(1).getTitle());
    }

    @Test
    public void testToString() {
        movie.setTitle("Test Movie");
        movie.setVideoUrl("http://example.com/video.mp4");

        String toString = movie.toString();

        assertTrue(toString.contains("Test Movie"));
        assertTrue(toString.contains("http://example.com/video.mp4"));
    }

    @Test
    public void testEquals() {
        movie.setVideoUrl("http://example.com/video.mp4");

        Movie sameMovie = new Movie();
        sameMovie.setVideoUrl("http://example.com/video.mp4");

        Movie differentMovie = new Movie();
        differentMovie.setVideoUrl("http://example.com/different.mp4");

        assertTrue(movie.equals(sameMovie));
        assertTrue(!movie.equals(differentMovie));
    }
}
