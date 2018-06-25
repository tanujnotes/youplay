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
import rx.schedulers.Schedulers;

/**
 * Created by tan on 28/01/17.
 **/

public class ChannelPlaylistFragment extends Fragment {
    private final String TAG = "ChannelPlaylistFragment";

    public static ChannelPlaylistFragment getInstance(String channelTitle, String channelId, String channelThumbnail) {
        ChannelPlaylistFragment channelPlaylistFragment = new ChannelPlaylistFragment();

        Bundle args = new Bundle();
        args.putString("channel_title", channelTitle);
        args.putString("channel_id", channelId);
        args.putString("channel_thumbnail", channelThumbnail);
        channelPlaylistFragment.setArguments(args);
        return channelPlaylistFragment;
    }

    @Inject
    NetworkService networkService;

    private Activity activity;
    private View root, loadingView;
    private List<ItemModel.Item> playlistList;
    private ChannelPlaylistAdapter channelPlaylistAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root != null) return root;
        root = inflater.inflate(R.layout.fragment_channel_playlist, container, false);
        activity = getActivity();
        ((MyApp) activity.getApplication()).getNetComponent().inject(this);

        Bundle bundle = getArguments();
        String channelTitle = bundle.getString("channel_title");
        final String channelId = bundle.getString("channel_id");
        String channelThumbnail = bundle.getString("channel_thumbnail");

        loadingView = root.findViewById(R.id.loading_view);
        RecyclerView channelPlaylistRV = root.findViewById(R.id.channel_playlist_recyclerview);
        playlistList = new ArrayList<>();
        channelPlaylistAdapter = new ChannelPlaylistAdapter(activity, playlistList);
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(activity);
        channelPlaylistRV.setLayoutManager(videoLayoutManager);
        channelPlaylistRV.setAdapter(channelPlaylistAdapter);

        getChannelPlaylist(channelId);

        loadingView.findViewById(R.id.retry_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingView.findViewById(R.id.retry_text).setVisibility(View.GONE);
                loadingView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                getChannelPlaylist(channelId);
            }
        });

        return root;
    }

    private void getChannelPlaylist(String channelId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet,contentDetails");
        map.put("channelId", channelId);
        map.put("maxResults", 50);
        map.put("key", BuildConfig.youtube_key);
        Observable<ItemModel> observable = networkService.getPlaylists(map);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
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
                        populateChannelPlaylist(model);
                    }
                });
    }

    private void populateChannelPlaylist(ItemModel model) {
        if (model.getItems().size() > 0) {
            playlistList.clear();
            playlistList.addAll(model.getItems());
            channelPlaylistAdapter.notifyDataSetChanged();
        } else
            showNoPlaylistMessage();
    }

    private void showNoPlaylistMessage() {
        loadingView.setVisibility(View.VISIBLE);
        loadingView.findViewById(R.id.progress_bar).setVisibility(View.GONE);
        loadingView.findViewById(R.id.retry_text).setVisibility(View.VISIBLE);
        ((TextView) loadingView.findViewById(R.id.retry_text)).setText(activity.getString(R.string.no_playlist_found));
    }
}