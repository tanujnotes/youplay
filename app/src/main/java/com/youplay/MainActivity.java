package com.youplay;

import android.animation.Animator;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, VideoInterface,
        ChannelInterface, ChannelPlaylistInterface, ToolbarAndFabInterface, FragmentInterface,
        NavigationView.OnNavigationItemSelectedListener {

    protected static final String TAG = "MainActivity";
    protected static final int MY_PERMISSIONS_REQUEST = 9;

    @Inject
    NetworkService networkService;

    @Inject
    PreferenceManager pm;

    @Inject
    DaoSession daoSession;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout drawerLayout;

    private View loadMoreBar;
    private List<ItemModel.Item> searchResultList;
    private List<String> searchSuggestionList = new ArrayList<>();
    private SearchResultAdapter searchResultAdapter;
    private SearchSuggestionAdapter searchSuggestionAdapter;
    private TextView searchReferenceText;
    private EditText searchEditText;
    private LinearLayout searchLayout;
    private RecyclerView searchSuggestionsRV, searchResultsRV;
    private String nextPageToken = null, searchQuery = "";
    private boolean backPressedOnce = false;
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onBackPressed() {
        if (searchSuggestionsRV.getVisibility() == View.VISIBLE) {
            searchSuggestionsRV.setVisibility(View.GONE);
            if (searchResultList.size() == 0) {
                searchLayout.setVisibility(View.GONE);
                Utils.changeStatusBarColor(this, R.color.colorPrimaryDark);
            }
        } else if (searchLayout.getVisibility() == View.VISIBLE) {
            searchLayout.setVisibility(View.GONE);
            Utils.changeStatusBarColor(this, R.color.colorPrimaryDark);
        } else if (getFragmentManager().getBackStackEntryCount() == 0) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            if (backPressedOnce) {
                super.onBackPressed();
                return;
            }
            backPressedOnce = true;
            Utils.showToast(this, "Tap BACK again to exit");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressedOnce = false;
                }
            }, 2000);

        } else if (getFragmentManager().getBackStackEntryCount() == 1) {
            getFragmentManager().popBackStack();
            setupToolbar(AppConstants.YOUPLAY, true);
        } else if (getFragmentManager().getBackStackEntryCount() > 1)
            getFragmentManager().popBackStack();
        else
            super.onBackPressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((MyApp) getApplication()).getNetComponent().inject(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Intent intent = getIntent();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getFragmentManager().beginTransaction().add(R.id.main_fragment, new MainFragment()).commit();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        searchLayout = findViewById(R.id.search_layout);
        searchEditText = findViewById(R.id.search_text);
        ImageView clearSearchText = findViewById(R.id.clear_search_text);
        clearSearchText.setOnClickListener(this);
        searchReferenceText = findViewById(R.id.search_reference_text);
        loadMoreBar = findViewById(R.id.load_more_bar);

        searchSuggestionAdapter = new SearchSuggestionAdapter(this, searchSuggestionList, pm);
        searchSuggestionsRV = findViewById(R.id.search_suggestions_rv);
        searchSuggestionsRV.setLayoutManager(new LinearLayoutManager(this));
        searchSuggestionsRV.setAdapter(searchSuggestionAdapter);

        searchResultList = new ArrayList<>();
        searchResultAdapter = new SearchResultAdapter(this, searchResultList);
        searchResultsRV = findViewById(R.id.search_results_rv);
        searchResultsRV.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRV.setAdapter(searchResultAdapter);
        searchResultsRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    searchSuggestionsRV.setVisibility(View.GONE);
                    searchEditText.clearFocus();
                    Utils.hideKeyboard(MainActivity.this);
                }
            }
        });
        searchResultAdapter.setLoadMoreListener(new SearchResultAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                searchResultsRV.post(new Runnable() {
                    @Override
                    public void run() {
                        loadMoreBar.setVisibility(View.VISIBLE);
                        searchReferenceText.setText(R.string.loading_);
                        getSearchResults(searchQuery, false);
                    }
                });
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        drawerLayout = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav_drawer, R.string.close_nav_drawer);
        mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getFragmentManager().getBackStackEntryCount() > 0) onBackPressed();
            }
        });
        drawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        String country = Utils.getUserCountry(this);
        if (country != null) pm.setUserCountry(country);

        initListeners();
        checkPermission();
        checkInternet();

        try {
            String videoId = intent.getStringExtra("shared_video_id");
            if (videoId == null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)))
                return;
            firebaseAnalytics.logEvent(AppConstants.Event.OPEN_WITH_YOUPLAY, new Bundle());
            getVideoDetails(videoId, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkInternet() {
        if (Utils.isInternetAvailable(this, false)) return;

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isInternetAvailable(MainActivity.this, false))
                    EventBus.getDefault().post(new LatestVideosRefreshEvent());
                else
                    checkInternet();
            }
        };
        Utils.showNoInternetMessage(this, clickListener);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("Hello!")
                    .setMessage("We need permission to play videos even while using other apps.")
                    .setPositiveButton("OPEN SETTINGS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + getPackageName()));
                                startActivityForResult(intent, MY_PERMISSIONS_REQUEST);
                            }

                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    private void initListeners() {
        searchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    String searchText = searchEditText.getText().toString().trim();
                    if (searchText.length() > 1) {
                        getSearchSuggestions(searchText);
                    } else {
                        searchSuggestionList.clear();
                        searchSuggestionList.addAll(Utils.getSavedSearchList(pm.getSavedSearchQueries()));
                        searchSuggestionAdapter.notifyDataSetChanged();
                        searchSuggestionsRV.setVisibility(View.VISIBLE);
                    }

                }
            }
        });
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    getSearchResults(searchEditText.getText().toString().trim(), true);
                    return true;
                }
                return false;
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (getCurrentFocus() != searchEditText) return;
                String searchText = editable.toString().trim();
                if (searchText.length() > 1) {
                    getSearchSuggestions(searchText);
                } else {
                    searchSuggestionList.clear();
                    searchSuggestionList.addAll(Utils.getSavedSearchList(pm.getSavedSearchQueries()));
                    searchSuggestionAdapter.notifyDataSetChanged();
                    searchSuggestionsRV.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void getSearchSuggestions(final String query) {
        if (!Utils.isInternetAvailable(MainActivity.this, true)) {
            searchReferenceText.setText(getString(R.string.no_internet_connection));
            return;
        }

        String url = "http://suggestqueries.google.com/complete/search?";
        HashMap<String, Object> map = new HashMap<>();
        map.put("client", "youtube");
        map.put("client", "firefox");
        map.put("ds", "yt");
        map.put("q", query);
        Observable<List<Object>> observable = networkService.getSearchSuggestions(url, map);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Object>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Error: " + e.getMessage());
                        if (!Utils.isInternetAvailable(MainActivity.this, false))
                            searchReferenceText.setText(getString(R.string.no_internet_connection));
                    }

                    @Override
                    public void onNext(List<Object> model) {
                        if (searchEditText.getText().toString().trim().length() == 0) return;
                        searchSuggestionList.clear();
                        searchSuggestionsRV.setVisibility(View.VISIBLE);
                        try {
                            List<String> list = (ArrayList<String>) model.get(1);
                            int listSize = list.size();
                            if (listSize > 5) {
                                searchSuggestionList.add(list.get(0));
                                searchSuggestionList.add(list.get(1));
                                searchSuggestionList.add(list.get(2));
                                searchSuggestionList.add(list.get(3));
                                searchSuggestionList.add(list.get(4));
                            } else {
                                searchSuggestionList.addAll(list);
                            }
                            searchSuggestionAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void openSearchView() {
        searchLayout.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int x = searchLayout.getRight() - Utils.dpToPx(MainActivity.this, 28);
                    int y = Utils.dpToPx(MainActivity.this, 28);
                    int startRadius = 0;
                    int endRadius = (int) Math.hypot(searchLayout.getWidth(), searchLayout.getHeight());
                    Utils.changeStatusBarColor(MainActivity.this, R.color.white);
                    Animator anim = ViewAnimationUtils.createCircularReveal(searchLayout, x, y, startRadius, endRadius);
                    searchLayout.setVisibility(View.VISIBLE);
                    anim.start();
                } else {
                    searchLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        if (!Utils.isInternetAvailable(MainActivity.this, true))
            searchReferenceText.setText(getString(R.string.no_internet_connection));
        else if (searchResultList.size() == 0) {
            searchReferenceText.setText("");
            searchEditText.requestFocus();
            Utils.showKeyboard(this);
        }
        firebaseAnalytics.logEvent(AppConstants.Event.OPEN_SEARCH, new Bundle());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        SearchManager searchManager = (SearchManager) MainActivity.this.getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(MainActivity.this.getComponentName()));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                openSearchView();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.clear_search_text:
                searchQuery = "";
                searchEditText.setText("");
                searchEditText.requestFocus();
                searchSuggestionList.clear();
                searchSuggestionList.addAll(Utils.getSavedSearchList(pm.getSavedSearchQueries()));
                searchSuggestionAdapter.notifyDataSetChanged();
                searchSuggestionsRV.setVisibility(View.VISIBLE);
                searchReferenceText.setText("");
                searchResultList.clear();
                searchResultAdapter.notifyDataChanged();
                Utils.showKeyboard(this);
                firebaseAnalytics.logEvent(AppConstants.Event.SEARCH_BAR_CLEAR, new Bundle());
                break;

            default:
                break;
        }
    }

    private void getVideoDetails(final String videoId, final boolean finishActivity) {
        if (!Utils.isInternetAvailable(MainActivity.this, true)) return;
        Utils.showToast(this, "Loading video...");

        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet,contentDetails,statistics");
        map.put("id", videoId);
        map.put("key", BuildConfig.youtube_key);
        Observable<ItemModel> observable = networkService.getVideos(map);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ItemModel>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Error: " + e.getMessage());
                        Utils.showToast(getApplicationContext(), "Oops! Please try again.");
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        if (model.getItems().size() == 0) {
                            Utils.showToastLong(MainActivity.this, "Video not found!");
                            return;
                        }
                        ItemModel.Item videoItem = model.getItems().get(0);
                        ItemModel.ResourceId resourceId = new ItemModel.ResourceId();
                        resourceId.setVideoId(videoId);
                        videoItem.getSnippet().setResourceId(resourceId);
                        onVideoPlay(videoItem);
                        if (finishActivity)
                            finish();
                    }
                });
    }

    public void getSearchResults(final String query, final boolean newSearch) {
        if (!Utils.isInternetAvailable(MainActivity.this, true)) {
            searchReferenceText.setText(getString(R.string.no_internet_connection));
            return;
        }

        if (newSearch) nextPageToken = null;
        Utils.hideKeyboard(this);
        Utils.saveSearchQuery(query, pm);
        searchSuggestionsRV.setVisibility(View.GONE);
        searchEditText.clearFocus();
        searchEditText.setText(query);
        searchReferenceText.setText(String.format("%s '%s' ...", getString(R.string.searching), query));
        searchQuery = query;
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 50);
        map.put("q", query);
        map.put("type", "video");
        map.put("key", BuildConfig.youtube_key);
        if (nextPageToken != null) map.put("pageToken", nextPageToken);
        networkService.getSearchResults(map)
                .flatMap(new Func1<ItemModel, Observable<ItemModel>>() {
                    @Override
                    public Observable<ItemModel> call(ItemModel itemModel) {
                        nextPageToken = itemModel.getNextPageToken();
                        if (nextPageToken != null) searchResultAdapter.setMoreDataAvailable(true);
                        else searchResultAdapter.setMoreDataAvailable(false);

                        StringBuilder videoIds = new StringBuilder();
                        for (ItemModel.Item item : itemModel.getItems())
                            videoIds.append(item.getSnippet().getResourceId().getVideoId()).append(",");

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("part", "snippet,contentDetails,statistics");
                        map.put("id", videoIds.toString().substring(0, videoIds.length() - 1));
                        map.put("key", BuildConfig.youtube_key);
                        return networkService.getVideos(map);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ItemModel>() {
                    @Override
                    public void onCompleted() {
                        Bundle bundle = new Bundle();
                        bundle.putString(AppConstants.Param.SEARCH_QUERY_TEXT, query);
                        firebaseAnalytics.logEvent(AppConstants.Event.SEARCH_QUERY, bundle);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Error: " + e.getMessage());
                        loadMoreBar.setVisibility(View.GONE);
                        if (Utils.isInternetAvailable(MainActivity.this, true))
                            searchReferenceText.setText(getString(R.string.please_try_again));
                        else
                            searchReferenceText.setText(getString(R.string.no_internet_connection));
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        loadMoreBar.setVisibility(View.GONE);
                        populateSearchResults(model, query);
                    }
                });
    }

    private void populateSearchResults(ItemModel model, String searchQuery) {
        if (searchResultAdapter.isLoadingMore()) {
            Bundle bundle = new Bundle();
            bundle.putString(AppConstants.Param.SEARCH_QUERY_TEXT, searchQuery);
            bundle.putInt(AppConstants.Param.SEARCH_RESULT_SIZE, searchResultList.size());
            firebaseAnalytics.logEvent(AppConstants.Event.SEARCH_LOAD_MORE, bundle);
        } else {
            searchResultList.clear();
        }

        searchResultList.addAll(model.getItems());
        if (searchResultList.size() > 0) {
            searchReferenceText.setText(String.valueOf(searchResultList.size()).concat(" results"));
        } else
            searchReferenceText.setText(getString(R.string.no_results_found));
        searchResultAdapter.notifyDataChanged();
    }

    public void getPlaylistVideos(final String playlistId, String ids, final boolean replaceQueue, final String className) {
        if (!Utils.isInternetAvailable(this, true)) return;
        if (playlistId.isEmpty() && ids.isEmpty()) return;

        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 50);
        if (!playlistId.isEmpty()) map.put("playlistId", playlistId);
        else map.put("id", ids);
        map.put("key", BuildConfig.youtube_key);

        networkService.getPlaylistVideos(map)
                .flatMap(new Func1<ItemModel, Observable<ItemModel>>() {
                    @Override
                    public Observable<ItemModel> call(ItemModel itemModel) {
                        StringBuilder videoIds = new StringBuilder();
                        for (ItemModel.Item item : itemModel.getItems())
                            videoIds.append(item.getSnippet().getResourceId().getVideoId()).append(",");

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("part", "snippet,contentDetails,statistics");
                        map.put("id", videoIds.toString().substring(0, videoIds.length() - 1));
                        map.put("key", BuildConfig.youtube_key);
                        return networkService.getVideos(map);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ItemModel>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Error: " + e.getMessage());
                    }

                    @Override
                    public void onNext(ItemModel model) {
//                        if (!channelName.isEmpty())
//                            for (int i = 0; i < model.getItems().size(); i++)
//                                model.getItems().get(i).getSnippet().setChannelTitle(channelName);

                        playAllFromPlaylist(model, replaceQueue, className);
                    }
                });
    }

    @Override
    public void onChannelClicked(String channelTitle, String channelId, String channelThumbnail, String videoId) {
        if (!Utils.isInternetAvailable(MainActivity.this, true)) return;
        if (channelId == null || channelId.isEmpty()) {
            Utils.showToast(this, "Failed to open channel");
            return;
        }

        ChannelFragment channelFragment = new ChannelFragment();
        Bundle args = new Bundle();
        args.putString("channel_title", channelTitle);
        args.putString("channel_id", channelId);
        args.putString("channel_thumbnail", channelThumbnail);
        channelFragment.setArguments(args);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                android.R.animator.fade_in, android.R.animator.fade_out);
        transaction.replace(R.id.main_fragment, channelFragment);
        transaction.addToBackStack(ChannelFragment.class.getSimpleName());
        transaction.commit();

        // If channel is opened from search layout
        if (searchLayout.getVisibility() == View.VISIBLE) hideSearchLayout();

        Bundle bundle = new Bundle();
        bundle.putString(AppConstants.Param.CHANNEL_ID, channelId);
        firebaseAnalytics.logEvent(AppConstants.Event.CHANNEL_SELECTED, bundle);

    }

    private void hideSearchLayout() {
        Utils.hideKeyboard(this);
        searchLayout.setVisibility(View.GONE);
        Utils.changeStatusBarColor(this, R.color.colorPrimaryDark);
    }

    @Override
    public void onPlaylistClicked(String playlistTitle, String playlistId, int playlistItemCount, String channelId) {
        if (!Utils.isInternetAvailable(MainActivity.this, true)) return;

        PlaylistFragment playlistFragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putString("playlist_title", playlistTitle);
        args.putString("playlist_id", playlistId);
        args.putInt("playlist_video_count", playlistItemCount);
        playlistFragment.setArguments(args);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                android.R.animator.fade_in, android.R.animator.fade_out);
        transaction.replace(R.id.main_fragment, playlistFragment);
        transaction.addToBackStack(PlaylistFragment.class.getSimpleName());
        transaction.commit();

        // If playlist is opened from search layout
        if (searchLayout.getVisibility() == View.VISIBLE) hideSearchLayout();

        Bundle bundle = new Bundle();
        bundle.putString(AppConstants.Param.PLAYLIST_ID, playlistId);
        bundle.putString(AppConstants.Param.CHANNEL_ID, channelId);
        firebaseAnalytics.logEvent(AppConstants.Event.PLAYLIST_SELECTED, bundle);
    }

    @Override
    public void playAllFromPlaylist(String playlistId, boolean replaceQueue, String className) {
        if (!Utils.isInternetAvailable(MainActivity.this, true)) return;
        if (playlistId == null || playlistId.isEmpty()) return;
        getPlaylistVideos(playlistId, "", replaceQueue, className);
    }

    @Override
    public void playAllFromPlaylist(ItemModel itemModel, boolean replaceQueue, String className) {
        if (!Utils.isInternetAvailable(this, true)) return;

        if (((MyApp) getApplication()).isVideoServiceRunning()) {
            EventBus.getDefault().post(new VideoListEvent(itemModel, replaceQueue, className));
            return;
        }

        try {
            Gson gson = new Gson();
            List<ItemModel.Item> videoList = new ArrayList<>();
            videoList.addAll(itemModel.getItems());
            pm.setSavedQueue(gson.toJson(videoList));
            startService(new Intent(MainActivity.this, VideoService.class));
        } catch (Exception e) {
            Utils.showToast(getApplicationContext(), "Please try again!");
            e.printStackTrace();
        }
    }

    @Override
    public void showPlaylists(String playlistType) {
    }

    @Override
    public void onVideoPlay(String videoId) {
        getVideoDetails(videoId, false);
    }

    @Override
    public void onVideoPlay(final ItemModel.Item video) {
        if (!Utils.isInternetAvailable(this, true)) return;

        if (((MyApp) getApplication()).isVideoServiceRunning()) {
            EventBus.getDefault().post(new PlayVideoEvent(video, false));
            return;
        }

        try {
            Gson gson = new Gson();
            List<ItemModel.Item> videoList = new ArrayList<>();
            videoList.add(video);
            pm.setSavedQueue(gson.toJson(videoList));
            startService(new Intent(MainActivity.this, VideoService.class));
        } catch (Exception e) {
            Utils.showToast(getApplicationContext(), "Please try again!");
            e.printStackTrace();
        }
    }

    @Override
    public void onVideoAdd(final ItemModel.Item video, final boolean playNext) {
        if (!Utils.isInternetAvailable(MainActivity.this, true)) return;

        if (((MyApp) getApplication()).isVideoServiceRunning()) {
            EventBus.getDefault().post(new AddVideoEvent(video, playNext));
            return;
        }

        try {
            Gson gson = new Gson();
            List<ItemModel.Item> videoList = new ArrayList<>();
            videoList.add(video);
            pm.setSavedQueue(gson.toJson(videoList));
            startService(new Intent(MainActivity.this, VideoService.class));
        } catch (Exception e) {
            Utils.showToast(getApplicationContext(), "Please try again!");
            e.printStackTrace();
        }
    }

    @Override
    public void setupToolbar(String title, boolean drawerIndicatorEnabled) {
        mDrawerToggle.setDrawerIndicatorEnabled(drawerIndicatorEnabled);
        boolean showImage = false;
        boolean scrollingEnabled = false;

        ImageView collapsingImage = findViewById(R.id.collapsing_image);
        View collapsingImageGradientTop = findViewById(R.id.collapsing_image_gradient_top);
        View collapsingImageGradientBottom = findViewById(R.id.collapsing_image_gradient_bottom);
        if (collapsingImage != null && collapsingImageGradientTop != null && collapsingImageGradientBottom != null) {
            if (showImage) {
                collapsingImage.setVisibility(View.VISIBLE);
                collapsingImageGradientTop.setVisibility(View.VISIBLE);
                collapsingImageGradientBottom.setVisibility(View.VISIBLE);
            } else {
                collapsingImage.setVisibility(View.GONE);
                collapsingImage.setImageDrawable(null);
                collapsingImageGradientTop.setVisibility(View.GONE);
                collapsingImageGradientBottom.setVisibility(View.GONE);
            }
        }
        // set scrolling behaviour
        CollapsingToolbarLayout toolbar = findViewById(R.id.collapsing_toolbar);
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();

        if (scrollingEnabled && !showImage) {
            toolbar.setTitleEnabled(false);
            setTitle(title);
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    + AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED
                    + AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        } else if (!scrollingEnabled && showImage && collapsingImage != null) {
            toolbar.setTitleEnabled(true);
            toolbar.setTitle(title);
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                    + AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        } else {
            toolbar.setTitleEnabled(false);
            setTitle(title);
            params.setScrollFlags(0);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_nav_home:
                drawerLayout.closeDrawers();
                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                return true;

            case R.id.menu_nav_favorites:
                int index = getFragmentManager().getBackStackEntryCount() - 1;
                if (index >= 0) {
                    FragmentManager.BackStackEntry backStackEntry = getFragmentManager().getBackStackEntryAt(index);
                    if (backStackEntry.getName().equals(FavoriteFragment.class.getSimpleName())) {
                        drawerLayout.closeDrawers();
                        return true;
                    }
                }

                FavoriteFragment favoriteFragment = new FavoriteFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                        android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.main_fragment, favoriteFragment, FavoriteFragment.class.getSimpleName());
                transaction.addToBackStack(FavoriteFragment.class.getSimpleName());
                transaction.commit();
                drawerLayout.closeDrawers();
                firebaseAnalytics.logEvent(AppConstants.Event.OPEN_FAVORITES, new Bundle());
                return true;

            case R.id.menu_nav_history:
                HistoryFragment historyFragment = new HistoryFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                        android.R.animator.fade_in, android.R.animator.fade_out);
                fragmentTransaction.replace(R.id.main_fragment, historyFragment, HistoryFragment.class.getSimpleName());
                fragmentTransaction.addToBackStack(HistoryFragment.class.getSimpleName());
                fragmentTransaction.commit();
                drawerLayout.closeDrawers();
                firebaseAnalytics.logEvent(AppConstants.Event.OPEN_WATCH_HISTORY, new Bundle());
                return true;

            case R.id.menu_nav_timer:
                if (((MyApp) getApplication()).isVideoServiceRunning()) showTimerDialog();
                else
                    Utils.showToastLong(MainActivity.this, "Timer works only after playing a video");
                drawerLayout.closeDrawers();
                firebaseAnalytics.logEvent(AppConstants.Event.TIMER_CLICKED, new Bundle());
                return true;

//            case R.id.menu_nav_share:
//                Utils.sendShareAppIntent(this, firebaseAnalytics);
//                drawerLayout.closeDrawers();
//                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showTimerDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.timer_layout, null);
        dialogBuilder.setView(dialogView);

        final EditText editText = dialogView.findViewById(R.id.timer_value);

        dialogBuilder.setTitle("Set Timer");
        dialogBuilder.setMessage(getString(R.string.timer_dialog_message));
        dialogBuilder.setPositiveButton("SET TIMER", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    int minutes = Integer.parseInt(editText.getText().toString());
                    if (minutes < 10 || minutes > 1000)
                        Utils.showToast(MainActivity.this, "Please enter a number between 10 and 1000.");
                    else
                        EventBus.getDefault().post(new ServiceEvent(AppConstants.EVENT_SET_TIMER, minutes));

                } catch (Exception e) {
                    Utils.showToast(MainActivity.this, "Please enter a valid number.");
                    e.printStackTrace();
                }
            }
        });
        dialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCancelable(true);
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((getIntent() != null) && getIntent().getBooleanExtra("from_service", false))
            openSearchView();
    }

    @Override
    public void openFragment(String fragmentClassName) {
        switch (fragmentClassName) {
            case AppConstants.HISTORY_FRAGMENT:
                HistoryFragment historyFragment = new HistoryFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                        android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.main_fragment, historyFragment, HistoryFragment.class.getSimpleName());
                transaction.addToBackStack(HistoryFragment.class.getSimpleName());
                transaction.commit();
                firebaseAnalytics.logEvent(AppConstants.Event.OPEN_WATCH_HISTORY, new Bundle());
                break;

            case AppConstants.FAVORITE_FRAGMENT:
                FavoriteFragment favoriteFragment = new FavoriteFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                        android.R.animator.fade_in, android.R.animator.fade_out);
                fragmentTransaction.replace(R.id.main_fragment, favoriteFragment, FavoriteFragment.class.getSimpleName());
                fragmentTransaction.addToBackStack(FavoriteFragment.class.getSimpleName());
                fragmentTransaction.commit();
                drawerLayout.closeDrawers();
                firebaseAnalytics.logEvent(AppConstants.Event.OPEN_FAVORITES, new Bundle());
                break;

            default:
                break;
        }
    }
}