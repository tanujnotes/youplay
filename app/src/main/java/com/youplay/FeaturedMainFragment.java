package com.youplay;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by tan on 28/01/18.
 **/

public class FeaturedMainFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "FeaturedMainFragment";

    @Inject
    PreferenceManager pm;
    @Inject
    NetworkService networkService;
    @Inject
    DaoSession daoSession;

    private View root, loadingView, loadMoreBar;
    private String nextPageToken = null;
    private List<ItemModel.Item> playlistItemsList;
    private TrendingVideoAdapter trendingVideoAdapter;
    private RecyclerView playlistVideosRV;
    private FirebaseAnalytics firebaseAnalytics;
    private int videoCategoryId = 0;

    public static FeaturedMainFragment getInstance(int videoCategoryId) {
        FeaturedMainFragment fragment = new FeaturedMainFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("video_category_id", videoCategoryId);
        fragment.setArguments(bundle);
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root != null) return root;
        root = inflater.inflate(R.layout.fragment_featured, container, false);
        Activity activity = getActivity();
        ((MyApp) activity.getApplication()).getNetComponent().inject(this);
        EventBus.getDefault().register(this);
        firebaseAnalytics = FirebaseAnalytics.getInstance(activity);
        videoCategoryId = getArguments().getInt("video_category_id");

        loadingView = root.findViewById(R.id.loading_view);
        loadMoreBar = root.findViewById(R.id.load_more_bar);
        playlistVideosRV = root.findViewById(R.id.playlist_videos_recyclerview);
        playlistVideosRV.setNestedScrollingEnabled(false);
        playlistItemsList = new ArrayList<>();
        trendingVideoAdapter = new TrendingVideoAdapter(activity, playlistItemsList);
        trendingVideoAdapter.setLoadMoreListener(new TrendingVideoAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                playlistVideosRV.post(new Runnable() {
                    @Override
                    public void run() {
                        loadMoreBar.setVisibility(View.VISIBLE);
                        getTrendingVideos();
                    }
                });
            }
        });
        playlistVideosRV.setLayoutManager(new LinearLayoutManager(activity));
        playlistVideosRV.setAdapter(trendingVideoAdapter);

        getTrendingVideos();
        loadingView.findViewById(R.id.retry_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingView.findViewById(R.id.retry_text).setVisibility(View.GONE);
                loadingView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                getTrendingVideos();
            }
        });
        return root;
    }

    private void getTrendingVideos() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 50);
        map.put("chart", "mostPopular");
        map.put("videoCategoryId", videoCategoryId);
        map.put("key", BuildConfig.youtube_key);
        if (!pm.getUserCountry().isEmpty()) map.put("regionCode", pm.getUserCountry());
        if (nextPageToken != null) map.put("pageToken", nextPageToken);

        networkService.getVideos(map)
                .flatMap(new Func1<ItemModel, Observable<ItemModel>>() {
                    @Override
                    public Observable<ItemModel> call(ItemModel itemModel) {
                        nextPageToken = itemModel.getNextPageToken();
                        if (nextPageToken != null) trendingVideoAdapter.setMoreDataAvailable(true);
                        else trendingVideoAdapter.setMoreDataAvailable(false);

                        StringBuilder videoIds = new StringBuilder();
                        for (ItemModel.Item item : itemModel.getItems()) // playlist_id is video_id
                            videoIds.append(item.getSnippet().getPlaylistId()).append(",");

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
                        loadingView.findViewById(R.id.retry_text).setVisibility(View.VISIBLE);
                        loadingView.findViewById(R.id.progress_bar).setVisibility(View.GONE);
                        loadMoreBar.setVisibility(View.GONE);
                        playlistVideosRV.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        loadingView.setVisibility(View.GONE);
                        loadMoreBar.setVisibility(View.GONE);
                        playlistVideosRV.setVisibility(View.VISIBLE);

                        if (trendingVideoAdapter.isLoadingMore())
                            firebaseAnalytics.logEvent(AppConstants.Event.TRENDING_LOAD_MORE, new Bundle());
                        else playlistItemsList.clear();
                        playlistItemsList.addAll(model.getItems());
                        trendingVideoAdapter.notifyDataChanged();
                    }
                });
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(LatestVideosRefreshEvent event) {
        loadingView.findViewById(R.id.retry_text).setVisibility(View.GONE);
        loadingView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        getTrendingVideos();
    }
}