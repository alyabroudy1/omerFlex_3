package com.omerflex.service.image;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.omerflex.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ImageLoaderTest {

    private Context context;

    @Mock
    private ImageView mockImageView;

    @Mock
    private ImageLoader.ImageLoadCallback mockCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.application;
    }

    @Test
    public void testLoadImage_withNullContext() {
        // Should not crash with null context
        ImageLoader.loadImage(null, "http://example.com/image.jpg", mockImageView);
        // No interaction with the ImageView should occur
        verify(mockImageView, never()).setImageDrawable(any());
    }

    @Test
    public void testLoadImage_withNullImageView() {
        // Should not crash with null ImageView
        ImageLoader.loadImage(context, "http://example.com/image.jpg", null);
        // No need to verify anything, just making sure it doesn't crash
    }

    @Test
    @Ignore("Glide cannot be properly initialized in unit tests")
    public void testLoadImage_withValidParameters() {
        // Test the basic loadImage method
        String url = "http://example.com/image.jpg";
        ImageLoader.loadImage(context, url, mockImageView);

        // Since we can't easily verify Glide interactions in a unit test,
        // we're just ensuring the method doesn't crash
    }

    @Test
    @Ignore("Glide cannot be properly initialized in unit tests")
    public void testLoadImage_withCustomPlaceholder() {
        // Test loadImage with custom placeholder and error drawables
        String url = "http://example.com/image.jpg";
        int placeholderId = R.drawable.default_background;
        int errorId = R.drawable.default_background;

        ImageLoader.loadImage(context, url, mockImageView, placeholderId, errorId);

        // Since we can't easily verify Glide interactions in a unit test,
        // we're just ensuring the method doesn't crash
    }

    @Test
    @Ignore("Glide cannot be properly initialized in unit tests")
    public void testLoadImageWithHeaders() {
        // Test loadImageWithHeaders method
        String url = "http://example.com/image.jpg";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Accept", "image/jpeg");

        ImageLoader.loadImageWithHeaders(context, url, headers, mockImageView);

        // Since we can't easily verify Glide interactions in a unit test,
        // we're just ensuring the method doesn't crash
    }

    @Test
    @Ignore("Glide cannot be properly initialized in unit tests")
    public void testLoadImageWithCallback() {
        // Test loadImageWithCallback method
        String url = "http://example.com/image.jpg";

        ImageLoader.loadImageWithCallback(context, url, mockImageView, mockCallback);

        // Since we can't easily verify Glide interactions in a unit test,
        // we're just ensuring the method doesn't crash
    }

    @Test
    public void testLoadImageWithCallback_nullContext() {
        // Test loadImageWithCallback with null context
        String url = "http://example.com/image.jpg";

        ImageLoader.loadImageWithCallback(null, url, mockImageView, mockCallback);

        // Verify that onError is called when context is null
        verify(mockCallback).onError();
    }

    @Test
    @Ignore("Glide cannot be properly initialized in unit tests")
    public void testPreloadImage() {
        // Test preloadImage method
        String url = "http://example.com/image.jpg";

        ImageLoader.preloadImage(context, url);

        // Since we can't easily verify Glide interactions in a unit test,
        // we're just ensuring the method doesn't crash
    }

    @Test
    @Ignore("Glide cannot be properly initialized in unit tests")
    public void testClearImage() {
        // Test clearImage method
        ImageLoader.clearImage(context, mockImageView);

        // Since we can't easily verify Glide interactions in a unit test,
        // we're just ensuring the method doesn't crash
    }

    @Test
    @Ignore("Glide cannot be properly initialized in unit tests")
    public void testGetRequestBuilder() {
        // Test getRequestBuilder method
        String url = "http://example.com/image.jpg";

        RequestBuilder<?> builder = ImageLoader.getRequestBuilder(context, url);

        // Verify that a non-null builder is returned
        assertNotNull(builder);
    }
}
