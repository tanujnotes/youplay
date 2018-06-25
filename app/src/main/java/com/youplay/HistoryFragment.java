package com.youplay;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.Query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by tan on 07/09/17.
 **/

public class HistoryFragment extends Fragment implements View.OnClickListener {

    private final String TAG = "HistoryFragment";
    private Activity activity;
    private List<ItemModel.Item> historyItemsList;
    private HistoryVideoAdapter historyVideoAdapter;
    private ItemModel itemModel = new ItemModel();
    private View noHistoryView, headerLayout;
    private TextView historyVideoCount;

    @Inject
    DaoSession daoSession;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        activity = getActivity();
        ((MyApp) activity.getApplication()).getNetComponent().inject(this);
        EventBus.getDefault().register(this);

        final RecyclerView playlistVideosRV = root.findViewById(R.id.history_videos_recyclerview);
        playlistVideosRV.setNestedScrollingEnabled(false);
        historyItemsList = new ArrayList<>();
        historyVideoAdapter = new HistoryVideoAdapter(activity, historyItemsList);
        playlistVideosRV.setLayoutManager(new LinearLayoutManager(activity));
        playlistVideosRV.setAdapter(historyVideoAdapter);

        noHistoryView = root.findViewById(R.id.no_history_view);
        headerLayout = root.findViewById(R.id.history_header_layout);
        historyVideoCount = root.findViewById(R.id.history_video_count_text);
        root.findViewById(R.id.history_play_all).setOnClickListener(this);
        root.findViewById(R.id.history_add_all).setOnClickListener(this);
        root.findViewById(R.id.history_clear_all).setOnClickListener(this);

        getHistoryVideos();
        return root;
    }

    private void getHistoryVideos() {
        HistoryDaoModelDao historyDao = daoSession.getHistoryDaoModelDao();
        long count = historyDao.queryBuilder().count();
        if (count == 0) {
            headerLayout.setVisibility(View.GONE);
            noHistoryView.setVisibility(View.VISIBLE);
        } else {
            noHistoryView.setVisibility(View.GONE);
            headerLayout.setVisibility(View.VISIBLE);
            setVideoCount(count);
            populateHistoryVideos(historyDao);
        }
    }

    public void setVideoCount(long count) {
        if (count == 0) {
            headerLayout.setVisibility(View.GONE);
            noHistoryView.setVisibility(View.VISIBLE);
        } else {
            noHistoryView.setVisibility(View.GONE);
            headerLayout.setVisibility(View.VISIBLE);
            if (count > 1) historyVideoCount.setText(String.valueOf(count).concat(" videos"));
            else historyVideoCount.setText("");
        }
    }

    protected void populateHistoryVideos(HistoryDaoModelDao historyDao) {
        Gson gson = new Gson();
        Query<HistoryDaoModel> query = historyDao.queryBuilder().orderDesc(HistoryDaoModelDao.Properties.TimeInMillis).build();
        List<HistoryDaoModel> historyDaoModelList = query.list();
        for (HistoryDaoModel model : historyDaoModelList) {
            ItemModel.Item videoItem = gson.fromJson(model.getVideoObject(), ItemModel.Item.class);
            videoItem.getSnippet().setTimeInMillis(model.getTimeInMillis());
            historyItemsList.add(videoItem);
        }
        historyVideoAdapter.notifyDataSetChanged();
        itemModel.setItems(historyItemsList);
    }

    protected void clearAllConfirmationDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Hey!")
                .setCancelable(true)
                .setMessage("Are you sure you want to clear your Watch History?")
                .setPositiveButton("CLEAR ALL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        HistoryDaoModelDao historyDao = daoSession.getHistoryDaoModelDao();
                        historyDao.deleteAll();
                        getHistoryVideos();
                        historyItemsList.clear();
                        historyVideoAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_delete)
                .show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.history_play_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_PLAYING);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(itemModel, true, getClass().getSimpleName());
                break;

            case R.id.history_add_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_ADDED_TO_QUEUE);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(itemModel, false, getClass().getSimpleName());
                break;

            case R.id.history_clear_all:
                clearAllConfirmationDialog();
                break;

            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ToolbarAndFabInterface) getActivity()).setupToolbar("Watch History", false);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(WatchHistoryRefreshEvent event) {
        historyItemsList.clear();
        getHistoryVideos();
    }
}
