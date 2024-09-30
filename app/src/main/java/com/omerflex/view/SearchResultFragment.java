package com.omerflex.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.ServerInterface;
import com.omerflex.service.ServerManager;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchResultFragment extends BrowseSupportFragment {
    private static final String TAG = "SearchResultFragment";
    private List<AbstractServer> servers = new ArrayList<>();
    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 15;

    private final Handler mHandler = new Handler();
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;

    ArrayObjectAdapter rowsAdapter;
    private int ROWS_COUNTER = 0;
    WebView webView;
    Movie clickedMovie;

    Fragment fragment = this;
    Activity activity;
    ArrayObjectAdapter faselAdapter;
    ArrayObjectAdapter cimaClubAdapter;
    ArrayObjectAdapter cima4uAdapter;
    ArrayObjectAdapter shahidAdapter;
    ArrayObjectAdapter arabSeedAdapter;
    ArrayObjectAdapter tvAdapter;

    String query;
    int requestCounter = 0;
    //    private GeckoBroadcastReceiver geckoReceiver;
//
//    private GeckoService geckoService;
    private boolean isBound = false;
    ServerManager serverManager;

    private boolean isInitialized = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        start();
    }

    public void start() {
        if (isInitialized) {
            return;
        }

        activity = getActivity();
        serverManager = new ServerManager(activity, fragment);

        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        webView = getActivity().findViewById(com.omerflex.R.id.webView_main);

//// Register the broadcast receiver
//        geckoReceiver = new GeckoBroadcastReceiver();
//        IntentFilter filter = new IntentFilter("com.example.gecko.broadcast");
//        getActivity().registerReceiver(geckoReceiver, filter);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
        isInitialized = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
            webView.stopLoading();
            // webView.destroy();
        }
        // Unregister the broadcast receiver
        //  getActivity().unregisterReceiver(geckoReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        webView.stopLoading();
        //webView.destroy();
    }

    private void loadSearchResultRaws() {
        for (AbstractServer server : serverManager.getServers()) {
            if (server == null) {
                return;
            }

            ArrayObjectAdapter adapter = getServerAdapter(server.getServerId());

            HeaderItem header = new HeaderItem(ROWS_COUNTER++, server.getLabel());
            rowsAdapter.add(new ListRow(header, adapter));


            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    ArrayList<Movie> movies = server.search(query, new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
                        @Override
                        public void onSuccess(ArrayList<Movie> result) {

                        }

                        @Override
                        public void onInvalidCookie(ArrayList<Movie> result) {

                        }

                        @Override
                        public void onInvalidLink(ArrayList<Movie> result) {

                        }

                        @Override
                        public void onInvalidLink(String message) {

                        }
                    });
                    // it done in new thread
                    Log.d(TAG, "loadHomepageRaws: " + server.getLabel() + ", " + movies.size());

                    if (!movies.isEmpty()) {
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // String newName = rowName + ("(" + finalMovieList.size() + ")");
                                    adapter.addAll(0, movies);
                                }
                            });
                        }
                    }
                } catch (Exception exception) {
                    Log.d(TAG, "loadHomepageRaws: error: " + server.getLabel() + ", " + exception.getMessage());
                }

            });
            executor.shutdown();
        }


//        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter());
//        AbstractServer iptvServer = IptvServer.getInstance(activity, fragment);
//        HeaderItem header = new HeaderItem(ROWS_COUNTER++, iptvServer.getLabel());
//        rowsAdapter.add(new ListRow(header, adapter));
//
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.submit(() -> {
//            try {
//                ArrayList<Movie> movies = iptvServer.search(query);
//                // it done in new thread
//                if (movies.size() > 0) {
//                    if (activity != null) {
//                        activity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                // String newName = rowName + ("(" + finalMovieList.size() + ")");
//                                adapter.addAll(0, movies);
//                            }
//                        });
//                    }
//                }
//            } catch (Exception exception) {
//                Log.d(TAG, "loadHomepageRaws: error: " + exception.getMessage());
//            }
//        });
//        executor.shutdown();
//
//        ExecutorService executor2 = Executors.newSingleThreadExecutor();
//        //load iptv m3u8 file list from google drive
//        executor2.submit(this::loadIptvGDriveFileList);
//        executor2.shutdown();
    }


    private void loadMoviesRow(String label, ArrayList<Movie> movies) {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter());
        HeaderItem header = new HeaderItem(ROWS_COUNTER++, label);
        rowsAdapter.add(new ListRow(header, adapter));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // String newName = rowName + ("(" + finalMovieList.size() + ")");
                        adapter.addAll(0, movies);
                    }
                });
            }

        });

        executor.shutdown();
    }

    private void loadRows() {
        query = Objects.requireNonNull(getActivity().getIntent().getStringExtra("query")).trim();
        faselAdapter = new ArrayObjectAdapter(new CardPresenter());
        cimaClubAdapter = new ArrayObjectAdapter(new CardPresenter());
        cima4uAdapter = new ArrayObjectAdapter(new CardPresenter());
        shahidAdapter = new ArrayObjectAdapter(new CardPresenter());
        arabSeedAdapter = new ArrayObjectAdapter(new CardPresenter());
        tvAdapter = new ArrayObjectAdapter(new CardPresenter());
        loadSearchResultRaws();
//
////        AbstractServer omerServer = OmarServer.getInstance(getActivity(), fragment);
////        if (omerServer.getConfig() == null || omerServer.getConfig().isActive){
////            loadServerRow("عمر", omerServer, new ArrayObjectAdapter(new CardPresenter()), query);
////        }
//
//        AbstractServer myCimaController = MyCimaController.getInstance(getActivity(), fragment);
//        if (myCimaController.getConfig() == null || myCimaController.getConfig().isActive){
//            loadServerRow("ماي سيما", myCimaController, new ArrayObjectAdapter(new CardPresenter()), query);
//        }
//
//        AbstractServer arabseedController = ArabSeedController.getInstance(fragment, getActivity());
//        if (arabseedController.getConfig() == null || arabseedController.getConfig().isActive){
//            loadServerRow(arabseedController.getLabel(), arabseedController,  new ArrayObjectAdapter(new CardPresenter()), query);
//        }
//
//        AbstractServer oldAkwamController = OldAkwamController.getInstance(getActivity(), fragment);
//        if (oldAkwamController.getConfig() == null || oldAkwamController.getConfig().isActive){
//            loadServerRow("اكوام القديم", oldAkwamController,  new ArrayObjectAdapter(new CardPresenter()), query);
//        }
//
//
//        AbstractServer akwamController = AkwamController.getInstance(getActivity(), fragment);
//        if (akwamController.getConfig() == null || akwamController.getConfig().isActive){
//            loadServerRow("اكوام", akwamController,  new ArrayObjectAdapter(new CardPresenter()), query);
//        }
//
//        AbstractServer watanFlixController = WatanFlixController.getInstance(fragment, getActivity());
//        if (watanFlixController.getConfig() == null || watanFlixController.getConfig().isActive){
//            loadServerRow("watan", watanFlixController, new ArrayObjectAdapter(new CardPresenter()), query);
//        }
//
//        AbstractServer faselHdController = FaselHdController.getInstance(this, getActivity());
//        if (faselHdController.getConfig() == null || faselHdController.getConfig().isActive){
//            loadServerRow("فاصل", faselHdController, faselAdapter, query);
//        }
//
//        AbstractServer cimaClubController = CimaClubController.getInstance(this, getActivity());
//        if (cimaClubController.getConfig() == null || cimaClubController.getConfig().isActive){
//            loadServerRow("سيماكلوب", cimaClubController, cimaClubAdapter, query);
//        }


        //     loadServerRow("اكوام القديم", OldAkwamController.getInstance(getActivity()),null, query);
        //       loadServerRow("سيمافوريو", Cima4uController.getInstance(fragment, getActivity()), cima4uAdapter, query);

//zzz        loadServerRow("شاهد فوريو", Shahid4uController.getInstance(this, getActivity()), shahidAdapter, query);
        //      loadServerRow("arabseed", ArabSeedController.getInstance(this, getActivity()), arabSeedAdapter, query);
//        CookieManager cookieManager = CookieManager.getInstance();

//        Log.d(TAG, "loadRows: cookies:"+cookieManager.getCookie("https://www.faselhd.club"));
//        if (cookieManager.getCookie("https://www.faselhd.club/?s=sonic") != null){
//            FaselHdController.getInstance(getActivity()).setCookies(cookieManager.getCookie("https://www.faselhd.club/?s=sonic"));
//
//            loadServerRow("فاصل", FaselHdController.getInstance(this, getActivity()), query);
//        }
// hhhhhhhhhhhhhh       loadServerRow("قنوات", IptvController.getInstance(getActivity(), this), tvAdapter, query);
//        loadServerRow("مسلسلات",new SeriesTimeController(new ArrayObjectAdapter(new CardPresenter()), getActivity()), query);
//

//        loadServerRow("جوجل",null, query);

//        Movie mm = new Movie();
//        mm.setStudio(Movie.SERVER_FASELHD);
//        mm.setState(Movie.RESULT_STATE);
//        //  mm.setVideoUrl("https://ciima-clup.quest/c135");
//        mm.setVideoUrl("https://www.faselhd.club/?s=sonic");
//
//        Log.d(TAG, "loadRows: cookie:"+CookieManager.getInstance().getCookie(mm.getVideoUrl()));
//
//        Intent browse = new Intent(getActivity(), BrowserActivity.class);
//        browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//        startActivityForResult(browse, 0);

        setAdapter(rowsAdapter);
    }


    private void loadServerRow(String rowName, AbstractServer server, ArrayObjectAdapter adapter, String query) {


        if (server == null) {
            Movie movie = new Movie();
            movie.setTitle("ابحث في جوجل");
            movie.setStudio(Movie.SERVER_GOOGLE);
            movie.setVideoUrl(query);
            movie.setCardImageUrl(String.valueOf(mDefaultBackground));
            // adapter.add(0, movie);
        } else {
//            if (server instanceof FaselHdController){
//                server.setListRowAdapter(adapter);
//            }
            Activity activity = getActivity();


            HeaderItem header = new HeaderItem(ROWS_COUNTER++, rowName);
            rowsAdapter.add(new ListRow(header, adapter));

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                List<Movie> finalMovieList = server.search(query, new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
                    @Override
                    public void onSuccess(ArrayList<Movie> result) {

                    }

                    @Override
                    public void onInvalidCookie(ArrayList<Movie> result) {

                    }

                    @Override
                    public void onInvalidLink(ArrayList<Movie> result) {

                    }

                    @Override
                    public void onInvalidLink(String message) {

                    }
                });
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.addAll(0, finalMovieList);
                    }
                });
            });

            executor.shutdown();
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
//        private ArrayObjectAdapter objectAdapter;
//
//        public ItemViewClickedListener(ArrayObjectAdapter adapter) {
//            objectAdapter = adapter;
//        }

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                //  if (movie.getStudio().equals(Movie.SERVER_GOOGLE)){
                /*if (movie.getStudio().equals(Movie.SERVER_FASELHD)){
                    FaselHdController server = FaselHdController.getInstance(getActivity());

//                    Intent intent = new Intent(getActivity(), BBrowserActivity.class);
//                    intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
//                    startActivity(intent);
                    List<Movie> mList= server.searchWebView(query);
                    Log.d(TAG, "onItemClicked: MovieListWeb:"+ mList);


                }else { */
                clickedMovie = movie;
                if (movie.getState() == Movie.COOKIE_STATE) {
                    Toast.makeText(getContext(), "renewing Cookie", Toast.LENGTH_SHORT).show();

                    renewCookie(movie);
                } else if (movie.getStudio().equals(Movie.SERVER_IPTV) && movie.getState() == Movie.VIDEO_STATE) {
                    Intent intent = new Intent(getActivity(), ExoplayerMediaPlayer.class);
                    intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
                    intent.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
                    Objects.requireNonNull(getActivity()).startActivity(intent);

                } else {
                    if (movie.getState() == Movie.NEXT_PAGE_STATE) {
                        //todo: add info to say if next already clicked, and handle the rest
                        if (movie.getDescription().equals("0")) {
                            ExecutorService executor = Executors.newSingleThreadExecutor();

                            executor.submit(() -> {
                                AbstractServer server = ServerManager.determineServer(movie, null, getActivity(), fragment);
                                //server
                                List<Movie> nextList = server.search(movie.getVideoUrl(), new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
                                    @Override
                                    public void onSuccess(ArrayList<Movie> result) {

                                    }

                                    @Override
                                    public void onInvalidCookie(ArrayList<Movie> result) {

                                    }

                                    @Override
                                    public void onInvalidLink(ArrayList<Movie> result) {

                                    }

                                    @Override
                                    public void onInvalidLink(String message) {

                                    }
                                });
                                Log.d(TAG, "handleItemClicked: nextPage:" + nextList.toString());

                                ArrayObjectAdapter adapter = (ArrayObjectAdapter) ((ListRow) row).getAdapter();
                                Log.d(TAG, "onItemClicked: adapter :" + adapter.toString());
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.addAll(adapter.size(), nextList);
                                    }
                                });

                                //flag that its already clicked
                                movie.setDescription("1");
                            });

                            executor.shutdown();
                        }

                    } else {
                        AbstractServer server = ServerManager.determineServer(movie, null, getActivity(), fragment);
                        int nextAction = server.fetchNextAction((Movie) item);
                        if (nextAction == VideoDetailsFragment.ACTION_OPEN_NO_ACTIVITY) {
                            Toast.makeText(getActivity(), "الرجاء الانتظار...", Toast.LENGTH_LONG).show();
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
//                                    Movie movie = server.fetch((Movie) item, ((Movie) item).getState()).movie;
                                }
                            });
                            t.start();
                        } else {
                            Log.d(TAG, "Item: " + item.toString());
                            Intent intent = new Intent(getActivity(), DetailsActivity.class);
                            intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
                            intent.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());

                            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                            getActivity(),
                                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                            DetailsActivity.SHARED_ELEMENT_NAME)
                                    .toBundle();
                            getActivity().startActivity(intent, bundle);
                        }
                    }
                }
            } else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.error_fragment))) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void renewCookie(Movie movie) {
        Intent browse = new Intent(getActivity(), BrowserActivity.class);

        browse.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
        browse.putExtra(DetailsActivity.MAIN_MOVIE, (Serializable) movie.getMainMovie());
        browse.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(browse, 0);
        //  startActivity(browse);
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            if (item instanceof Movie) {
                mBackgroundUri = ((Movie) item).getBackgroundImageUrl();
                startBackgroundTimer();
            }
        }
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .fallback(mDefaultBackground)
                .placeholder(mDefaultBackground)
                .into(new SimpleTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(@NonNull Drawable drawable,
                                                @Nullable Transition<? super Drawable> transition) {
                        mBackgroundManager.setDrawable(drawable);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateBackground(mBackgroundUri);
                }
            });
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(
                    ContextCompat.getColor(getActivity(), R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    private void prepareBackgroundManager() {

        Activity currentActivity = getActivity();
        Context context = getContext();
        if (currentActivity == null || context == null) {
            return;
        }
        mBackgroundManager = BackgroundManager.getInstance(currentActivity);
        mBackgroundManager.attach(currentActivity.getWindow());

        mDefaultBackground = ContextCompat.getDrawable(context, R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        currentActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        //setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), GetSearchQueryActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        setOnItemViewSelectedListener(new ItemViewSelectedListener());
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        if (clickedMovie == null) {
            return;
        }
        ArrayObjectAdapter objectAdapter = getServerAdapter(clickedMovie.getStudio());
        AbstractServer server = ServerManager.determineServer(clickedMovie, objectAdapter, getActivity(), fragment);

        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String result = data.getStringExtra("result");
                    Log.d(TAG, "onActivityResult: " + result);
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            Movie movie = new Movie();
//                            movie.setTitle(query);
//                            movie.setStudio(result);

//                            Log.d(TAG, "run: after renewing the cookie:" + server.getHeaders());
                            //List<Movie> movies = server.search(query);
                            Gson gson = new Gson();
                            Type movieListType = new TypeToken<List<Movie>>() {
                            }.getType();
                            List<Movie> movies = gson.fromJson(result, movieListType);

                            if (clickedMovie.getState() == Movie.COOKIE_STATE) {
                                for (Movie mov : movies) {
                                    mov.setState(server.detectMovieState(mov));
                                    //sets main movie to it self same as search method as renewing cookie only search for movies
                                    mov.setMainMovie(mov);
//                                    if (server.isSeries(mov)){
//                                        movies.get(movies.indexOf(mov)).setState(Movie.GROUP_OF_GROUP_STATE);
//                                    }else {
//                                        movies.get(movies.indexOf(mov)).setState(Movie.ITEM_STATE);
//                                    }
                                }
                            }

                            if (movies.size() > 0) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "run: adapter> " + objectAdapter.size());
                                        objectAdapter.addAll(1, movies);
                                    }
                                });
                            } else {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "لايوجد نتائج", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        }
                    });
                    t.start();
                    Log.d(TAG, "onActivityResult: cookies:");
//
//                    Gson gson = new Gson();
//                    Type movieListType = new TypeToken<List<Movie>>(){}.getType();
//                    List<Movie> movies = gson.fromJson(result, movieListType);
//                    Log.d(TAG, "onActivityResult: movie:"+movies.toString());
                }
                // Do something with the result here

            }
        } else if (resultCode == Activity.RESULT_OK) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    List<Movie> movies = server.search(clickedMovie.getTitle(), new ServerInterface.ActivityCallback<ArrayList<Movie>>() {
                        @Override
                        public void onSuccess(ArrayList<Movie> result) {

                        }

                        @Override
                        public void onInvalidCookie(ArrayList<Movie> result) {

                        }

                        @Override
                        public void onInvalidLink(ArrayList<Movie> result) {

                        }

                        @Override
                        public void onInvalidLink(String message) {

                        }
                    });
                    if (movies.size() > 0) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "run: adapter> " + objectAdapter.size());
                                objectAdapter.addAll(1, movies);
                            }
                        });
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "لايوجد نتائج", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
            });
            t.start();
        }

    }

    private ArrayObjectAdapter getServerAdapter(String serverId) {
        switch (serverId) {
            case Movie.SERVER_CIMA_CLUB:
                return cimaClubAdapter;
            case Movie.SERVER_CIMA4U:
                return cima4uAdapter;
            case Movie.SERVER_SHAHID4U:
                return shahidAdapter;
            case Movie.SERVER_ARAB_SEED:
                return arabSeedAdapter;
            case Movie.SERVER_FASELHD:
                return faselAdapter;
            case Movie.SERVER_IPTV:
                return tvAdapter;
            default:
                return new ArrayObjectAdapter(new CardPresenter());
        }
    }
}