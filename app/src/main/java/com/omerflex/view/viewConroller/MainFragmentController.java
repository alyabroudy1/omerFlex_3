package com.omerflex.view.viewConroller;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.omerflex.OmerFlexApplication;
import com.omerflex.entity.MovieRepository;
import com.omerflex.service.UpdateService;

public class MainFragmentController extends BaseFragmentController {

    private MovieRepository movieRepository;
    UpdateService updateService;
    public static String TAG = "MainFragmentController";

    public MainFragmentController(BrowseSupportFragment fragment, ArrayObjectAdapter rowsAdapter, Drawable defaultBackground) {
        super(fragment, rowsAdapter, defaultBackground);
        this.movieRepository = MovieRepository.getInstance(fragment.getActivity(), ((OmerFlexApplication) fragment.getActivity().getApplication()).getDatabase().movieDao());
        updateService = new UpdateService(mFragment);
    }

    @Override
    public void loadData() {
        Log.d(TAG, "loadData: ");
        movieRepository.getHomepageMovies( (category, movieList) -> {
            if (movieList != null) {
                Log.d("Movie", "Fetched movie33: " + movieList.toString());
                // todo let the id be the studio name
                HeaderItem header = new HeaderItem(1, category);
                addMovieRow(header, movieList);
            } else {
                Log.d("Movie", "movieList not found.");
            }
        });
    }

    @Override
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleActivityResult(requestCode, resultCode, data);
        updateService.handleOnActivityResult(requestCode, resultCode, data);
    }

    public void handleOnRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        updateService.handleOnRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void handleOnDestroy() {
        updateService.handleOnDestroy();
    }
}