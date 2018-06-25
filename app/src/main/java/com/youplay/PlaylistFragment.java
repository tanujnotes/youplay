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
import android.widget.TextView;

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
 * Created by tan on 20/01/17.
 **/

public class PlaylistFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "PlaylistFragment";

    @Inject
    NetworkService networkService;

    private Activity activity;
    private View root, loadingView, loadMoreBar, playlistHeaderLayout;
    private List<ItemModel.Item> playlistItemsList;
    private PlaylistVideoAdapter playlistVideoAdapter;
    private ItemModel playlistItems;
    private RecyclerView playlistVideosRV;
    private String playlistId;
    private String playlistTitle, nextPageToken = null;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root != null) return root;
        root = inflater.inflate(R.layout.fragment_playlist, container, false);
        activity = getActivity();
        ((MyApp) activity.getApplication()).getNetComponent().inject(this);

        Bundle bundle = getArguments();
        playlistTitle = bundle.getString("playlist_title");
        playlistId = bundle.getString("playlist_id");

        loadingView = root.findViewById(R.id.loading_view);
        loadMoreBar = root.findViewById(R.id.load_more_bar);
        playlistHeaderLayout = root.findViewById(R.id.playlist_header_layout);
        playlistVideosRV = root.findViewById(R.id.playlist_videos_recyclerview);
        playlistVideosRV.setNestedScrollingEnabled(false);
        playlistItemsList = new ArrayList<>();
        playlistVideoAdapter = new PlaylistVideoAdapter(activity, playlistItemsList);
        playlistVideoAdapter.setLoadMoreListener(new PlaylistVideoAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                playlistVideosRV.post(new Runnable() {
                    @Override
                    public void run() {
                        loadMoreBar.setVisibility(View.VISIBLE);
                        ((TextView) root.findViewById(R.id.playlist_video_count_text)).setText(R.string.loading_);
                        getPlaylistVideos(playlistId);
                    }
                });
            }
        });
        playlistVideosRV.setLayoutManager(new LinearLayoutManager(activity));
        playlistVideosRV.setAdapter(playlistVideoAdapter);

        root.findViewById(R.id.playlist_add_all).setOnClickListener(this);
        root.findViewById(R.id.playlist_play_all).setOnClickListener(this);

        getPlaylistVideos(playlistId);
        loadingView.findViewById(R.id.retry_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingView.findViewById(R.id.retry_text).setVisibility(View.GONE);
                loadingView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                getPlaylistVideos(playlistId);
            }
        });

        return root;
    }

    private void getPlaylistVideos(final String playlistId) {
        Utils.isInternetAvailable(activity, true);
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 50);
        map.put("playlistId", playlistId);
        map.put("key", BuildConfig.youtube_key);
        if (nextPageToken != null) map.put("pageToken", nextPageToken);
        networkService.getPlaylistVideos(map)
                .flatMap(new Func1<ItemModel, Observable<ItemModel>>() {
                    @Override
                    public Observable<ItemModel> call(ItemModel itemModel) {
                        nextPageToken = itemModel.getNextPageToken();
                        if (nextPageToken != null) playlistVideoAdapter.setMoreDataAvailable(true);
                        else playlistVideoAdapter.setMoreDataAvailable(false);

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
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
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
                        playlistHeaderLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        loadingView.setVisibility(View.GONE);
                        loadMoreBar.setVisibility(View.GONE);
                        playlistVideosRV.setVisibility(View.VISIBLE);
                        playlistHeaderLayout.setVisibility(View.VISIBLE);
                        playlistItems = model;
                        populatePlaylistVideos(model);
                    }
                });
    }

    private void populatePlaylistVideos(ItemModel playlistItemsModel) {
        playlistItemsList.addAll(playlistItemsModel.getItems());
        playlistVideoAdapter.notifyDataChanged();
        ((TextView) root.findViewById(R.id.playlist_video_count_text))
                .setText(String.valueOf(playlistItemsList.size()).concat(" videos"));

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.playlist_play_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_PLAYING);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(playlistItems, true, getClass().getSimpleName());
                break;

            case R.id.playlist_add_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_ADDED_TO_QUEUE);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(playlistItems, false, getClass().getSimpleName());
                break;

            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ToolbarAndFabInterface) getActivity()).setupToolbar(playlistTitle, false);
    }
}
