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
 * Created by tan on 28/02/17.
 **/

public class FavoriteFragment extends Fragment implements View.OnClickListener {

    private final String TAG = "FavoriteFragment";
    private Activity activity;
    private List<ItemModel.Item> favoriteItemsList;
    private FavoriteVideoAdapter favoriteVideoAdapter;
    private ItemModel itemModel = new ItemModel();
    private View noFavoritesView, headerLayout;
    private TextView favVideoCount;

    @Inject
    DaoSession daoSession;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_favorite, container, false);
        activity = getActivity();
        ((MyApp) activity.getApplication()).getNetComponent().inject(this);
        EventBus.getDefault().register(this);

        final RecyclerView playlistVideosRV = (RecyclerView) root.findViewById(R.id.favorite_videos_recyclerview);
        playlistVideosRV.setNestedScrollingEnabled(false);
        favoriteItemsList = new ArrayList<>();
        favoriteVideoAdapter = new FavoriteVideoAdapter(activity, favoriteItemsList, daoSession, this);
        playlistVideosRV.setLayoutManager(new LinearLayoutManager(activity));
        playlistVideosRV.setAdapter(favoriteVideoAdapter);

        noFavoritesView = root.findViewById(R.id.no_favorites_view);
        headerLayout = root.findViewById(R.id.favorite_header_layout);
        favVideoCount = (TextView) root.findViewById(R.id.favorite_video_count_text);
        root.findViewById(R.id.favorite_play_all).setOnClickListener(this);
        root.findViewById(R.id.favorite_add_all).setOnClickListener(this);
        root.findViewById(R.id.favorite_clear_all).setOnClickListener(this);

        getFavoriteVideos();
        return root;
    }

    private void getFavoriteVideos() {
        FavoriteDaoModelDao favoriteDao = daoSession.getFavoriteDaoModelDao();
        long count = favoriteDao.queryBuilder().count();
        if (count == 0) {
            headerLayout.setVisibility(View.GONE);
            noFavoritesView.setVisibility(View.VISIBLE);
        } else {
            noFavoritesView.setVisibility(View.GONE);
            headerLayout.setVisibility(View.VISIBLE);
            setVideoCount(count);
            populateFavVideos(favoriteDao);
        }
    }

    public void setVideoCount(long count) {
        if (count == 0) {
            headerLayout.setVisibility(View.GONE);
            noFavoritesView.setVisibility(View.VISIBLE);
        } else {
            noFavoritesView.setVisibility(View.GONE);
            headerLayout.setVisibility(View.VISIBLE);
            if (count > 1) favVideoCount.setText(String.valueOf(count).concat(" videos"));
            else favVideoCount.setText("");
        }
    }

    protected void populateFavVideos(FavoriteDaoModelDao favoriteDao) {
        Gson gson = new Gson();
        Query<FavoriteDaoModel> query = favoriteDao.queryBuilder().orderDesc(FavoriteDaoModelDao.Properties.TimeInMillis).build();
        List<FavoriteDaoModel> favoriteDaoModelList = query.list();
        for (FavoriteDaoModel model : favoriteDaoModelList) {
            ItemModel.Item videoItem = gson.fromJson(model.getVideoObject(), ItemModel.Item.class);
            videoItem.getSnippet().setTimeInMillis(model.getTimeInMillis());
            favoriteItemsList.add(videoItem);
        }
        favoriteVideoAdapter.notifyDataSetChanged();
        itemModel.setItems(favoriteItemsList);
    }

    protected void clearAllConfirmationDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Whoa!")
                .setCancelable(true)
                .setMessage("Are you sure you want to clear your Favorite collection?")
                .setPositiveButton("CLEAR ALL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FavoriteDaoModelDao favoriteDao = daoSession.getFavoriteDaoModelDao();
                        favoriteDao.deleteAll();
                        getFavoriteVideos();
                        favoriteItemsList.clear();
                        favoriteVideoAdapter.notifyDataSetChanged();
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

            case R.id.favorite_play_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_PLAYING);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(itemModel, true, getClass().getSimpleName());
                break;

            case R.id.favorite_add_all:
                Utils.showToastLong(activity, AppConstants.VIDEOS_ADDED_TO_QUEUE);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(itemModel, false, getClass().getSimpleName());
                break;

            case R.id.favorite_clear_all:
                clearAllConfirmationDialog();
                break;

            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ToolbarAndFabInterface) getActivity()).setupToolbar("Favorites", false);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(FavoriteRefreshEvent event) {
        favoriteItemsList.clear();
        getFavoriteVideos();
    }
}
