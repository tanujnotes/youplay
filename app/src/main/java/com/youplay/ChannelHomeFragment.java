package com.youplay;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

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
 * Created by tan on 28/01/17.
 **/

public class ChannelHomeFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "ChannelPlaylistFragment";

    public static ChannelHomeFragment getInstance(String channelTitle, String channelId, String channelThumbnail) {
        ChannelHomeFragment channelHomeFragment = new ChannelHomeFragment();

        Bundle args = new Bundle();
        args.putString("channel_title", channelTitle);
        args.putString("channel_id", channelId);
        args.putString("channel_thumbnail", channelThumbnail);
        channelHomeFragment.setArguments(args);
        return channelHomeFragment;
    }

    @Inject
    NetworkService networkService;

    @Inject
    DaoSession daoSession;

    private Activity activity;
    private View root, latestVideoLoadingView, popularVideoLoadingView;
    private List<ItemModel.Item> latestItemsList, popularItemsList;
    private RecyclerView channelLatestRV, channelPopularRV;
    private PlaylistVideoAdapter latestVideoAdapter;
    private PlaylistVideoAdapter popularVideoAdapter;
    private String channelTitle, channelId, channelThumbnail;
    private ItemModel popularItemsModel;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root != null) return root;
        root = inflater.inflate(R.layout.fragment_channel_home, container, false);
        activity = getActivity();
        ((MyApp) activity.getApplication()).getNetComponent().inject(this);
        ((TextView) root.findViewById(R.id.channel_latest_videos_see_more))
                .setTypeface(Typeface.createFromAsset(activity.getAssets(), "fonts/material.ttf"));

        Bundle bundle = getArguments();
        channelTitle = bundle.getString("channel_title");
        channelId = bundle.getString("channel_id");
        channelThumbnail = bundle.getString("channel_thumbnail");

        ((TextView) root.findViewById(R.id.channel_title)).setText(channelTitle);
        latestVideoLoadingView = root.findViewById(R.id.latest_video_loading_view);
        popularVideoLoadingView = root.findViewById(R.id.popular_video_loading_view);
        root.findViewById(R.id.channel_latest_play_all).setOnClickListener(this);
        root.findViewById(R.id.channel_popular_play_all).setOnClickListener(this);
        root.findViewById(R.id.channel_latest_videos_see_more).setOnClickListener(this);
        root.findViewById(R.id.play_channel_top_videos).setOnClickListener(this);

        channelLatestRV = root.findViewById(R.id.channel_latest_videos_recyclerview);
        channelLatestRV.setNestedScrollingEnabled(false);
        latestItemsList = new ArrayList<>();
        latestVideoAdapter = new PlaylistVideoAdapter(activity, latestItemsList);
        channelLatestRV.setLayoutManager(new LinearLayoutManager(activity));
        channelLatestRV.setAdapter(latestVideoAdapter);

        channelPopularRV = root.findViewById(R.id.channel_popular_videos_recyclerview);
        channelPopularRV.setNestedScrollingEnabled(false);
        popularItemsList = new ArrayList<>();
        popularVideoAdapter = new PlaylistVideoAdapter(activity, popularItemsList);
        channelPopularRV.setLayoutManager(new LinearLayoutManager(activity));
        channelPopularRV.setAdapter(popularVideoAdapter);

        latestVideoLoadingView.findViewById(R.id.retry_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                latestVideoLoadingView.findViewById(R.id.retry_text).setVisibility(View.GONE);
                latestVideoLoadingView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                getLatestChannelVideos(channelId);
            }
        });

        popularVideoLoadingView.findViewById(R.id.retry_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popularVideoLoadingView.findViewById(R.id.retry_text).setVisibility(View.GONE);
                popularVideoLoadingView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                getPopularChannelVideos(channelId);
            }
        });

        ImageView channelImageView = root.findViewById(R.id.channel_home_thumbnail);
        setChannelThumbnail(channelImageView);
        getLatestChannelVideos(channelId);
        getPopularChannelVideos(channelId);

        return root;
    }

    private void setChannelThumbnail(final ImageView chanelImageView) {
        Glide.with(activity)
                .load(channelThumbnail)
                .asBitmap()
                .centerCrop()
                .override(200, 200)
                .placeholder(R.drawable.youplay_logo_white)
                .error(R.drawable.youplay_logo_white)
                .into(new BitmapImageViewTarget(chanelImageView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        try {
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory
                                    .create(activity.getApplicationContext().getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            chanelImageView.setImageDrawable(circularBitmapDrawable);

                        } catch (Exception e) {
                            Log.e(TAG, "Showing round profile picture failed", e);
                        }
                    }
                });
    }

    private void getLatestChannelVideos(String channelId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 3);
        map.put("playlistId", Utils.getChannelUploadsId(channelId));
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
                        latestVideoLoadingView.findViewById(R.id.retry_text).setVisibility(View.VISIBLE);
                        latestVideoLoadingView.findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        latestVideoLoadingView.setVisibility(View.GONE);
                        channelLatestRV.setVisibility(View.VISIBLE);
                        populateVideoList(model);
                    }
                });
    }

    private void populateVideoList(ItemModel playlistItemsModel) {
        latestItemsList.addAll(playlistItemsModel.getItems());
        latestVideoAdapter.notifyDataSetChanged();
    }

    private void getPopularChannelVideos(String channelId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 20);
        map.put("order", "viewCount");
        map.put("channelId", channelId);
        map.put("type", "video");
        map.put("key", BuildConfig.youtube_key);
        networkService.getPopularChannelUploads(map)
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
                        popularVideoLoadingView.findViewById(R.id.retry_text).setVisibility(View.VISIBLE);
                        popularVideoLoadingView.findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        popularVideoLoadingView.setVisibility(View.GONE);
                        channelPopularRV.setVisibility(View.VISIBLE);
                        popularItemsModel = model;
                        populatePopularVideoList(model);
                        root.findViewById(R.id.play_channel_top_videos).setVisibility(View.VISIBLE);
                    }
                });
    }

    private void populatePopularVideoList(ItemModel playlistItemsModel) {
        popularItemsList.addAll(playlistItemsModel.getItems());
        popularVideoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.channel_latest_play_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_PLAYING);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(
                        Utils.getChannelUploadsId(channelId), true, getClass().getSimpleName());
                break;

            case R.id.play_channel_top_videos:
            case R.id.channel_popular_play_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_PLAYING);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(popularItemsModel, true, getClass().getSimpleName());
                break;

            case R.id.channel_latest_videos_see_more:
                String uploadsId = "UU".concat(channelId.substring(2));
                ((ChannelPlaylistInterface) activity).onPlaylistClicked(
                        channelTitle + " Latest Videos",
                        uploadsId,
                        0,
                        channelId
                );
                break;

        }
    }
}
