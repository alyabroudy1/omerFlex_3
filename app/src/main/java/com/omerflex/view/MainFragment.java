package com.omerflex.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
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
import com.omerflex.R;
import com.omerflex.entity.Movie;
import com.omerflex.server.AbstractServer;
import com.omerflex.server.AkwamServer;
import com.omerflex.server.ArabSeedServer;
import com.omerflex.server.CimaClubServer;
import com.omerflex.server.FaselHdController;
import com.omerflex.server.IptvServer;
import com.omerflex.server.MyCimaServer;
import com.omerflex.server.OldAkwamServer;
import com.omerflex.server.OmarServer;
import com.omerflex.service.ServerManager;
import com.omerflex.service.database.MovieDbHelper;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainFragment extends BrowseSupportFragment {
    private static final String TAG = "MainFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private int HEADER_ROWS_COUNTER = 0;
    private int IPTV_HEADER_ROWS_COUNTER = 0;
    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 15;

    private final Handler mHandler = new Handler(Looper.myLooper());
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;

    //*****
    MovieDbHelper dbHelper;
    Fragment fragment;
    ServerManager serverManager;
    Activity activity;
    ArrayObjectAdapter rowsAdapter;
    public static List<Movie> iptvList;

    private boolean isInitialized = false;

    //*****

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);


        setRetainInstance(true);

        initializeThings();

        prepareBackgroundManager();

        setupUIElements();
        if (!isInitialized) {
            loadRows();
            isInitialized = true;
        }


        setupEventListeners();
    }

    private void initializeThings() {
        fragment = this;
        activity = getActivity();
        dbHelper = MovieDbHelper.getInstance(activity);
        serverManager = new ServerManager(activity, fragment);
        serverManager.updateServers();
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        iptvList = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void loadRows() {
        setAdapter(rowsAdapter);
        CookieManager.getInstance().setAcceptCookie(true);

        // load rows of the home screen
        loadHomepageRaws();

//        test();

        setSelectedPosition(0);
        setAdapter(rowsAdapter);
    }

    private void loadHomepageRaws() {
        //initialize homepage
        Log.d(TAG, "loadHomepageRaws ");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
//            Log.d(TAG, "loadHomepageRaws a ");
            for (AbstractServer server : serverManager.getServers()) {
                Log.d(TAG, "loadHomepageRaws b: " + server.getLabel());
//                ExecutorService executor2 = Executors.newSingleThreadExecutor();
//                executor2.submit(() -> {
                try {
                    // it done in new thread
                    if (server instanceof OmarServer) {
                        loadOmarServerHomepage(server);
                    }else if (
                            server instanceof OldAkwamServer ||
                                    server instanceof CimaClubServer ||
                                    server instanceof FaselHdController //||
//                                    server instanceof AkwamServer ||
//                                    server instanceof ArabSeedServer ||
//                                    server instanceof MyCimaServer
                    ) {
                        continue;
                    }else if (server instanceof IptvServer) {
                        //load history rows first
                        loadHomepageHistoryRaws();

                        loadMoviesRow(server, addRowToMainAdapter(server.getLabel()), null);
//                            //channel list
//                            ArrayList<Movie> channels = dbHelper.getIptvHomepageChannels();
//                            if (channels.size() > 0) {
//                                loadMoviesRow("tv", channels);
//                            }
                    } else {
                        loadMoviesRow(server, addRowToMainAdapter(server.getLabel()), null);
                    }

                } catch (Exception exception) {
                    Log.d(TAG, "loadHomepageRaws: error: " + exception.getMessage());
                    exception.printStackTrace();
                }
//                });
//                executor2.shutdown();
            }
        });
        executor.shutdown();
    }

    private ArrayObjectAdapter addRowToMainAdapter(String label) {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter());
        HeaderItem header = new HeaderItem(HEADER_ROWS_COUNTER++, label);
        rowsAdapter.add(new ListRow(header, adapter));
        return adapter;
    }


    private void loadOmarServerHomepage(AbstractServer server) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            if (activity != null) {
                ArrayList<Movie> movies = server.getHomepageMovies();


                Map<String, List<Movie>> moviesByCategory = movies.stream()
                        .flatMap(movie -> movie.getCategories().stream()
                                .map(category -> new AbstractMap.SimpleEntry<>(category, movie)))
                        .collect(Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                        ));

                // Assuming you already have the 'moviesByCategory' map
                for (Map.Entry<String, List<Movie>> entry : moviesByCategory.entrySet()) {
                    String category = entry.getKey();
                    List<Movie> moviesInCategory = entry.getValue();
                    Log.d(TAG, "loadOmarServerHomepage: " + category + ", " + moviesInCategory.size());
                    loadMoviesRow(server, addRowToMainAdapter(category), (ArrayList) moviesInCategory);
                }
            }

        });
    }

    private void loadMoviesRow(AbstractServer server, ArrayObjectAdapter adapter, ArrayList<Movie> moviesList) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            if (activity != null) {
                final ArrayList<Movie> movies; // Declare as effectively final
                if (moviesList == null && server != null) {
                    movies = server.getHomepageMovies();
                } else {
                    movies = moviesList;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // String newName = rowName + ("(" + finalMovieList.size() + ")");
                        if (movies != null && adapter != null){
                            adapter.addAll(0, movies);
                        }
                    }
                });
            }

        });

        executor.shutdown();
    }

    private void loadMoviesRow_2(String label, ArrayList<Movie> movies) {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter());
        HeaderItem header = new HeaderItem(HEADER_ROWS_COUNTER++, label);
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


    private void loadHomepageHistoryRaws() {
        ArrayList<Movie> historyMovies = dbHelper.getAllHistoryMovies(false);
        loadMoviesRow(null, addRowToMainAdapter("المحفوظات"), historyMovies);

        ArrayList<Movie> iptvHistoryMovies = dbHelper.getAllHistoryMovies(true);
        loadMoviesRow(null, addRowToMainAdapter("محفوظات القنوات"), iptvHistoryMovies);
    }


    private void test() {

//        loadHomepageRaws();


//        Movie mm = new Movie();
//    //String url = "https://ui.cima4u.bio/category/%D8%A7%D9%81%D9%84%D8%A7%D9%85-%D8%A7%D8%AC%D9%86%D8%A8%D9%8A/";
//    //String url = "https://main4.ci4u.co/%d9%81%d9%8a%d9%84%d9%85-your-christmas-or-mine-2022-%d9%85%d8%aa%d8%b1%d8%ac%d9%85-%d8%a7%d9%88%d9%86-%d9%84%d8%a7%d9%8a%d9%86/?wat=1";
////    String url = "https://cimatube.cc/embed1/29f1d50ae92194ed586eb34c47a41945";
////    String url = "https://www.faselhd.express/video_player?uid=0&vid=863adbd5b09c0b764128cec2dcb1d84f&img=https://img.scdns.io/thumb/863adbd5b09c0b764128cec2dcb1d84f/large.jpg&nativePlayer=true";
//    String url = "https://m.gamezone.cam/blastman/play/?playit=4e5467344e54497a&fgame=aHR0cHM6Ly9tLmFzZC5ob21lcw==HhZ35wNXMr,,A838GBww1vdTL2F9AJxvrJvWstQaumxqBnXP6SaihNYaPEZa11xkTUWadvZZ66NZ3k0wmtvG_SRztVSfOiWIdHXBlao1UsSSYbNheJsyjywQ2FZNcD9tWIwPhGXYVS9YVmrn1jprLEwl8WqV1art8jZh32Al1Kbp1_VPDYZa8x7om2IrH9aQa6wwVkF24Z8Cot7f9TbGbGlWsKCnMi7Na9bbrza5sBTH_xiFP2DCBkcLFDrkw2lfbcdoCzPTou4Rafy3zvgDpbHgCJ5emNfuKyC67g-cotgx-AGgE7pyBYIuRzIL4gjlves52m7YnmvY0aoimtxyQfTRHWFGSgvQvsSktMyrbe7teUkYL-kUySwqxv81HgvG6jASzQULbYAXYk3zRZ2176TtMyyqruvcvnadVR4c5kyPbLf873rXcpGJPmrScq48wdKPUEaQAhGw6IPxolMBUfi_eRQAzgylEkdqNe";
//    String url = "https://wecima.show/watch/%d9%85%d8%b4%d8%a7%d9%87%d8%af%d8%a9-%d9%85%d8%b3%d9%84%d8%b3%d9%84-fox-spirit-matchmaker-love-in-pavilion-%d9%85%d9%88%d8%b3%d9%85-1-%d8%ad%d9%84%d9%82%d8%a9-10/";
//    String url = "https://www.google.com/search?sca_esv=792c36b2414f597c&sca_upv=1&sxsrf=ADLYWILq2GQVLfS6OtWBOZttfUz_gO2FpA:1716908543425&q=ss&uds=ADvngMgnp21tvUWATbVPNUHyrBahasC3_Xskxp-yVqIWaah14uWwZGyEs8SEQnojasvFy1klDAGiK1X000V1u8TMw8WbPd4mdOSf3Z-_WfznXIH3KMxOFX-WPTEBgiVxKEucOwT4nWYlEkxizOQdCtNhrw_D4bTD33sKjMmA9bKU2UEcPXFk20y77h4YYmxpfxmbKeQt9hRlNGWgX8Tf0bU6xIx3rtYCluTxCDf7XbrXNNJ2yGCiJpA&udm=2&prmd=ivnbz&sa=X&ved=2ahUKEwjIw97ezrCGAxW_A9sEHf-IDPkQtKgLegQIDRAB";
//    mm.setVideoUrl(url);
//    mm.setStudio(Movie.SERVER_MyCima);
//    mm.setState(Movie.ITEM_STATE);
//        Intent browse = new Intent(getActivity(), BrowserActivity.class);
//         browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//         getActivity().startActivity(browse);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

//            AbstractServer iptv = IptvServer.getInstance(activity, fragment);
//            ServerConfig iptvConfig = new ServerConfig();
//            iptvConfig.url = "https://cimanow.cc";
//            iptv.setConfig(iptvConfig);
//            ArrayList<Movie> movies = iptv.getHomepageMovies();
////            ArrayList<Movie> movies = iptv.search("الجزيرة");
//            Log.d(TAG, "test: movies:"+movies.size());
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(iptv.getLabel(), movies);
//            }

//           ControllableServer cimaNow = CimaNowController.getInstance(activity, fragment);
//            ServerConfig cimaNowConfig = new ServerConfig();
//            cimaNowConfig.url = "https://cimanow.cc";
//            cimaNow.setConfig(cimaNowConfig);
//            ArrayList<Movie> movies = cimaNow.getHomepageMovies();
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(cimaNow.getLabel(), movies);
//            }
//
//            ControllableServer arabseed = ArabSeedController.getInstance(fragment, activity);
//            ServerConfig arabseedConfig = new ServerConfig();
//            arabseedConfig.url = "https://arabseed.show";
//            arabseed.setConfig(arabseedConfig);
//            ArrayList<Movie> movies = arabseed.search("الخائن");
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(arabseed.getLabel(), movies);
//            }

//            ControllableServer cima = CimaClubController.getInstance(fragment, activity);
//            ServerConfig config = new ServerConfig();
//            config.url = "https://cimaclub.top";
//            cima.setConfig(config);
//            ArrayList<Movie> movies = cima.getHomepageMovies();
//            if (movies != null && movies.size() > 0) {
//                loadMoviesRow(cima.getLabel(), movies);
//            }

//            ControllableServer fasel = FaselHdController.getInstance(fragment, activity);
//            ServerConfig faselConfig = new ServerConfig();
//            faselConfig.url = "https://www.faselhd.link";
//            fasel.setConfig(faselConfig);
//            ArrayList<Movie> faselMovies = fasel.getHomepageMovies();
//            if (faselMovies != null && faselMovies.size() > 0) {
//                loadMoviesRow(fasel.getLabel(), faselMovies);
//            }

//                        AbstractServer mycima = MyCimaServer.getInstance(activity, fragment);
//            ServerConfig mycimaConfig = new ServerConfig();
//            mycimaConfig.url = "https://wecima.show/";
//            mycima.setConfig(mycimaConfig);
//            ArrayList<Movie> mycimaMovies = mycima.getHomepageMovies();
//            if (mycimaMovies != null && mycimaMovies.size() > 0) {
//                loadMoviesRow(mycima, addRowToMainAdapter(mycima.getLabel()), mycimaMovies);
//            }
//
//
//        });
//        executor.shutdown();


//        ControllableServer fasel = FaselHdController.getInstance(fragment, activity);
//            ServerConfig faselConfig = new ServerConfig();
//            faselConfig.url = "https://www.faselhd.link";
//            fasel.setConfig(faselConfig);
//        Intent searchResultIntent = new Intent(getActivity(), SearchResultActivity.class);
//        searchResultIntent.putExtra("query", "sonic");
//        startActivity(searchResultIntent);


//        searchResultIntent.putExtra("query", "الغريب");
//        hhhhhhh     searchResultIntent.putExtra("query", "bab");
        // setResult(Activity.RESULT_OK,returnIntent);
        //  finish();


        //hhhhhhh       Intent browse = new Intent(getActivity(), BrowserActivity.class);
        //hhhhhhh browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
        //hhhhhhh getActivity().startActivity(browse);

//    Locale locale = new Locale("en");
//    Locale.setDefault(locale);
//    Configuration config = new Configuration();
//    config.locale = locale;
//    getActivity().getBaseContext().getResources().updateConfiguration(config,
//            getActivity().getBaseContext().getResources().getDisplayMetrics());


        // dbHelper.cleanMovieList();

        //  loadServerRow("شاهد", Shahid4uController.getInstance(getActivity()), "game of thrones" );


        //            loadServerRow("أفلام",AkwamController.getInstance(getActivity()), "spider" );
        //loadServerRow("ماي سيما", MyCimaController.getInstance(), "https://wecima.actor/category/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa/%d9%85%d8%b3%d9%84%d8%b3%d9%84%d8%a7%d8%aa-%d8%b1%d9%85%d8%b6%d8%a7%d9%86-2023-series-ramadan-2023/list/" );
        //loadServerRow("ماي سيما", MyCimaController.getInstance(), "https://mycima22.wecima.cam/seriestv/" );
//    loadServerRow("ماي سيما", MyCimaController.getInstance(), "https://mycima.uno/genre/%d9%83%d9%88%d9%85%d9%8a%d8%af%d9%8a%d8%a7-comedy/" );
        // loadServerRow("سيمافوريو", Cima4uController.getInstance(getActivity()), "https://cima4u.mx/netflix/" );
        //     loadServerRow("أفلام",new AkwamController(new ArrayObjectAdapter(new CardPresenter()), getActivity()), "https://akwam.co/movies" );


//    Movie mm = new Movie();
////    //String url = "https://ui.cima4u.bio/category/%D8%A7%D9%81%D9%84%D8%A7%D9%85-%D8%A7%D8%AC%D9%86%D8%A8%D9%8A/";
////    //String url = "https://main4.ci4u.co/%d9%81%d9%8a%d9%84%d9%85-your-christmas-or-mine-2022-%d9%85%d8%aa%d8%b1%d8%ac%d9%85-%d8%a7%d9%88%d9%86-%d9%84%d8%a7%d9%8a%d9%86/?wat=1";
//////    String url = "https://cimatube.cc/embed1/29f1d50ae92194ed586eb34c47a41945";
//////    String url = "https://www.faselhd.express/video_player?uid=0&vid=863adbd5b09c0b764128cec2dcb1d84f&img=https://img.scdns.io/thumb/863adbd5b09c0b764128cec2dcb1d84f/large.jpg&nativePlayer=true";
//    String url = "https://www.faselhd.link/movies/%d9%81%d9%8a%d9%84%d9%85-sonic-hedgehog-2020-%d9%85%d8%aa%d8%b1%d8%ac%d9%85-ct";
//    mm.setVideoUrl(url);
//    mm.setStudio(Movie.SERVER_FASELHD);
//    mm.setState(Movie.COOKIE_STATE);
//
//    Intent browse = new Intent(getActivity(), BrowserActivity.class);
//     browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//     getActivity().startActivity(browse);


//hhhhhhh
        //hhhhhhh       Movie mm = new Movie();
        //hhhhhhh         mm.setStudio(Movie.SERVER_CIMA_CLUB);
        //mm.setState(Movie.ITEM_STATE);
        //hhhhhhh          mm.setState(Movie.BROWSER_STATE);
        //hhhhhhh       mm.setTitle("test");
        //hhhhhhh        mm.setCardImageUrl("www.google.com");
//        mm.setVideoUrl("https://tv.cima4u.mx/Video/Sonic+the+Hedgehog+2+2022-50604.html");
//       // mm.setVideoUrl("https://www.faselhd.club/?p=194950");
        // mm.setVideoUrl("https://ciima-clup.quest/c135");
        //  mm.setVideoUrl("https://akwam.us/movie/9152/%D9%86%D8%A8%D9%8A%D9%84-%D8%A7%D9%84%D8%AC%D9%85%D9%8A%D9%84-%D8%A3%D8%AE%D8%B5%D8%A7%D8%A6%D9%8A-%D8%AA%D8%AC%D9%85%D9%8A%D9%84");
        //hhhhhhh         mm.setVideoUrl("https://akwam.us/download/149205/9152/%D9%86%D8%A8%D9%8A%D9%84-%D8%A7%D9%84%D8%AC%D9%85%D9%8A%D9%84-%D8%A3%D8%AE%D8%B5%D8%A7%D8%A6%D9%8A-%D8%AA%D8%AC%D9%85%D9%8A%D9%84");
        //      mm.setVideoUrl("#");
//hhh        mm.setVideoUrl("https://cimclllb.sbs/watch/مسلسل-the-wheel-of-time-الموسم-الثاني-الحلقة-5-الخامسة");
//      //  mm.setVideoUrl("https://www.faselhd.ac/?s=sonic");
        //mm.setVideoUrl("https://www.faselhd.club/seasons/%d9%85%d8%b3%d9%84%d8%b3%d9%84-sonic-prime");

////        Log.d(TAG, "loadRows: cookie:"+CookieManager.getInstance().getCookie(mm.getVideoUrl()));


        //hhhhhhh

//            Intent browse = new Intent(getActivity(), BBrowserActivity.class);
//    browse.putExtra(DetailsActivity.MOVIE, (Serializable) mm);
//        getActivity().startActivity(browse);
//        getActivity().startActivity(browse);


//        String newUrl=  "https://k301o.dood.video/u5kj6egmx3flsdgge7bf4osedb4eegnjgawimg7akxdvtb3wrznktqo5c5wq/qzk1uews13~tnLJC64N1E?token=3gyjneucz05go388u7xfnho4&expiry=1673037884992|Referer=https://dood.pm/";
////        String newUrl=  "https://k301o.dood.video/u5kj6egmx3flsdgge7bf4osedb4eegnjgawimg7akxdvtb3wrznjrmw5c5wq/u4sjgmzxhu~LpgSZlwKGZ|token=3gyjneucz05go388u7xfnho4&expiry=1673009930631";
//        String type = "video/*"; // It works for all video application
//        Uri uri = Uri.parse(newUrl);
//        Log.d("yessss2", uri + "");
//        Intent in1 = new Intent(Intent.ACTION_VIEW, uri);
//        in1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        //  in1.setPackage("org.videolan.vlc");
//        in1.setDataAndType(uri, type);
//        // view.stopLoading();
//        startActivity(in1);


        // loadServerRow("سيماكلوب", CimaClubController.getInstance(getActivity()), "game of thrones" );
        //     loadServerRow("ماي سيما", MyCimaController.getInstance(), "game of thrones" );
        //    loadServerRow("كورة",new KooraLiveController(new ArrayObjectAdapter(new CardPresenter()), getActivity()), "https://www.yallashoote.com" );
        //  loadServerRow("اكوام القديم", OldAkwamController.getInstance(getActivity()), "spider" );
        //    loadServerRow("شاهد",Shahid4uController.getInstance( getActivity()), "https://shahed4u.vip/netflix/" );
        //loadServerRow("شاهد",Shahid4uController.getInstance( getActivity()), "game of thrones" );


        //   HeaderItem faselHeader = new HeaderItem(ROWS_COUNTER++, "فاصل");
        //   rowsAdapter.add(new ListRow(faselHeader, faselAdapter));
        //kk  HeaderItem faselHeader = new HeaderItem(ROWS_COUNTER++, "فاصل");
        //kk rowsAdapter.add(new ListRow(faselHeader, faselAdapter));


        // ArrayObjectAdapter  akwamRowsAdapter = new ArrayObjectAdapter(new CardPresenter());
        //ControllableServer akwamS = ;


        //  HeaderItem header = new HeaderItem(0, "rowName");
        //increase row counter location
        //ROWS_COUNTER++;
        //add header name and position of the row
        // rowsAdapter.add(new ListRow(header, akwamRowsAdapter));
        // rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());


//
//    ExecutorService executor = Executors.newSingleThreadExecutor();
//    executor.submit(() -> {
//
//
});

        executor.shutdown();


        //to set focus on the first row
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            handleItemClicked(itemViewHolder, item, row);
//            if (item instanceof Movie) {
//                Movie movie = (Movie) item;
//                Log.d(TAG, "Item: " + item.toString());
//                Intent intent = new Intent(getActivity(), DetailsActivity.class);
//                intent.putExtra(DetailsActivity.MOVIE, (Parcelable) movie);
//
//                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                                getActivity(),
//                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                                DetailsActivity.SHARED_ELEMENT_NAME)
//                        .toBundle();
//                getActivity().startActivity(intent, bundle);
//            } else if (item instanceof String) {
//                if (((String) item).contains(getString(R.string.error_fragment))) {
//                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
//                    startActivity(intent);
//                } else {
//                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
//                }
//            }
        }
    }

    private void handleItemClicked(Presenter.ViewHolder itemViewHolder, Object item, Row row) {
        if (item instanceof Movie) {
            Movie movie = (Movie) item;
            Log.d(TAG, "onItemClicked: " + item.toString());
            if (movie.getStudio().equals(Movie.SERVER_IPTV)) {
                if (movie.getState() == Movie.PLAYLIST_STATE) {
                    if (!iptvList.isEmpty()) {
                        int rowSize = rowsAdapter.size() - 1;
                        int defaultHeaders = rowSize - IPTV_HEADER_ROWS_COUNTER;
                        Log.d(TAG, "handleItemClicked: defaultHeaders:" + defaultHeaders);

//                        Log.d(TAG, "handleItemClicked: iptvStartIndex: "+iptvStartIndex);
                        if (rowSize >= IPTV_HEADER_ROWS_COUNTER) {

                            while (rowSize > defaultHeaders) {
//                                Log.d(TAG, "onItemClicked: remove row:" + iptvLastIndex);
                                try {
                                    rowsAdapter.remove(
                                            rowsAdapter.get((rowSize--)
                                            ));
                                } catch (Exception exception) {
                                    Log.d(TAG, "handleItemClicked: error deleting iptv header on main fragment: " + exception.getMessage());
                                }

                            }
                            iptvList.clear();
                            IPTV_HEADER_ROWS_COUNTER = 0;
                        }
                    }
                    //      try {
                    //   showProgressDialog();
                    IptvServer iptvServer = IptvServer.getInstance(activity, fragment);

                    CompletableFuture<Map<String, List<Movie>>> futureGroupedMovies = iptvServer.fetchAndGroupM3U8ContentAsync(movie);
                    Toast.makeText(getActivity(), "الرجاء الانتظار...", Toast.LENGTH_LONG).show();

                    futureGroupedMovies.thenAcceptAsync(groupedMovies -> {
                        for (Map.Entry<String, List<Movie>> entry : groupedMovies.entrySet()) {
                            String group = entry.getKey();
                            List<Movie> groupMovies = entry.getValue();
                            // Creating a movie magic show with your UI update!
                            getActivity().runOnUiThread(() -> {
                                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
                                iptvList.addAll(groupMovies);
                                listRowAdapter.addAll(0, groupMovies);
                                HeaderItem header = new HeaderItem(HEADER_ROWS_COUNTER++, group);
                                IPTV_HEADER_ROWS_COUNTER++;
                                rowsAdapter.add(new ListRow(header, listRowAdapter));
                            });
                        }
                    }).exceptionally(e -> {
                        // Handle any exceptions with grace (and maybe a touch of humor!)
                        Log.e(TAG, "Something went wrong: " + e.getMessage());
                        return null;
                    });
// This line waits for the completion of the future
                    //  hideProgressDialog();
                    // } catch (ExecutionException | InterruptedException e) {
                } else {
                    Intent intent = new Intent(getActivity(), ExoplayerMediaPlayer.class);
                    intent.putExtra(DetailsActivity.MOVIE, (Serializable) movie);
                    Objects.requireNonNull(getActivity()).startActivity(intent);
                }

                //exist method after handling
                return;
            }

            if (movie.getState() == Movie.NEXT_PAGE_STATE) {
                //todo: add info to say if next already clicked, and handle the rest
                if (movie.getDescription().equals("0")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            AbstractServer server = ServerManager.determineServer(movie, null, getActivity(), fragment);
                            //server
                            List<Movie> nextList = server.search(movie.getVideoUrl());
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
                        }
                    }).start();

                }

            } else {
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

        } else if (item instanceof String) {
            if (((String) item).contains(getString(R.string.error_fragment))) {
                Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
            }
        }
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

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        mDefaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
//        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent getQueryIntent = new Intent(getActivity(), GetSearchQueryActivity.class);
                startActivity(getQueryIntent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
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
                    ContextCompat.getColor(getContext(), R.color.default_background));
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

}