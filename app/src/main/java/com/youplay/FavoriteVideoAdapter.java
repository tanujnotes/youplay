package com.youplay;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 28/02/17.
 **/

public class FavoriteVideoAdapter extends RecyclerView.Adapter<FavoriteVideoAdapter.ItemViewHolder> {

    final String TAG = "FavoriteVideoAdapter";
    private List<ItemModel.Item> favoriteItems = new ArrayList<>();
    private Activity activity;
    private FavoriteVideoAdapter.ItemViewHolder videoHolder;
    private DaoSession daoSession;
    private FavoriteFragment favoriteFragment;

    public FavoriteVideoAdapter(Activity activity, List<ItemModel.Item> items, DaoSession daoSession, FavoriteFragment favoriteFragment) {
        this.activity = activity;
        this.favoriteItems = items;
        this.daoSession = daoSession;
        this.favoriteFragment = favoriteFragment;
    }

    @Override
    public FavoriteVideoAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_favorite_video, null);
        return new FavoriteVideoAdapter.ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final FavoriteVideoAdapter.ItemViewHolder holder, int position) {
        String thumbnailUrl = "";
        try {
            thumbnailUrl = favoriteItems.get(position).getSnippet().getThumbnails().getDefault().getUrl();
            thumbnailUrl = favoriteItems.get(position).getSnippet().getThumbnails().getMedium().getUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.favoriteVideoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((VideoInterface) activity).onVideoPlay(favoriteItems.get(holder.getAdapterPosition()));
            }
        });

        holder.favoriteVideoTitle.setText(favoriteItems.get(position).getSnippet().getTitle());

        try {
            holder.favoriteVideoChannel.setText(
                    favoriteItems.get(holder.getAdapterPosition()).getSnippet().getChannelTitle());
            holder.favoriteVideoDuration.setText(Utils.formatVideoDuration(
                    favoriteItems.get(holder.getAdapterPosition()).getContentDetails().getDuration()));
            holder.favoriteVideoViews.setText(Utils.formatViewCount(
                    favoriteItems.get(holder.getAdapterPosition()).getStatistics().getViewCount()));
            holder.favoriteVideoPublished.setText(Utils.getRelativeDay(
                    favoriteItems.get(position).getSnippet().getPublishedAt()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Glide.with(activity)
                .load(thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.youplay_logo_placeholder)
                .error(R.drawable.youplay_logo_placeholder)
                .override(AppConstants.GLIDE_VIDEO_THUMBNAIL_WIDTH, AppConstants.GLIDE_VIDEO_THUMBNAIL_HEIGHT)
                .into(holder.favoriteVideoThumbnail);
        holder.favoriteVideoMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.favoriteVideoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_favorite_adapter, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
            }
        });
        holder.favoriteVideoLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.favoriteVideoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_favorite_adapter, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
                return true;
            }
        });
        holder.favoriteVideoRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeVideoFromFavorites(holder);
            }
        });
    }

    private PopupMenu.OnMenuItemClickListener queueVideoMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_play_next:
                    Utils.showToast(activity, "Added as next video");
                    ((VideoInterface) activity).onVideoAdd(favoriteItems.get(videoHolder.getAdapterPosition()), true);
                    return true;

                case R.id.menu_add_to_queue:
                    Utils.showToast(activity, "Added to queue");
                    ((VideoInterface) activity).onVideoAdd(favoriteItems.get(videoHolder.getAdapterPosition()), false);
                    return true;

                case R.id.menu_share_video:
                    String videoId = favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getResourceId().getVideoId();
                    String videoTitle = favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                    Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    return true;

                case R.id.menu_open_channel: // playlist id is video id
                    ((ChannelInterface) activity).onChannelClicked(
                            favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelTitle(),
                            favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelId(),
                            "",
                            favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId());
                    return true;

                case R.id.menu_remove_video:
                    removeVideoFromFavorites(videoHolder);
                    return true;

                case R.id.menu_video_details:
                    showVideoDetailsDialog();
                    return true;

                default:
                    return true;
            }
        }
    };

    private void showVideoDetailsDialog() {
        String message = "Title: " + favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle()
                + "\n\nDescription: " + favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getDescription();
        new AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle("Video Details")
                .setMessage(message)
                .setPositiveButton("PLAY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((VideoInterface) activity).onVideoPlay(favoriteItems.get(videoHolder.getAdapterPosition()));
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setNeutralButton("SHARE VIDEO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String videoId = favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId();
                        String videoTitle = favoriteItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                        Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    }
                })
                .show();
    }

    private void removeVideoFromFavorites(ItemViewHolder viewHolder) {
        if (viewHolder == null) return;
        int itemPosition = viewHolder.getAdapterPosition();
        deleteVideoFromTable(favoriteItems.get(itemPosition).getSnippet().getResourceId().getVideoId());
        favoriteItems.remove(itemPosition);
        notifyItemRemoved(itemPosition);
        notifyItemRangeChanged(itemPosition, favoriteItems.size());
        favoriteFragment.setVideoCount(favoriteItems.size());
    }

    private void deleteVideoFromTable(String videoId) {
        FavoriteDaoModelDao favoriteDao = daoSession.getFavoriteDaoModelDao();
        FavoriteDaoModel entity = favoriteDao.queryBuilder()
                .where(FavoriteDaoModelDao.Properties.VideoId.eq(videoId)).build().unique();
        favoriteDao.delete(entity);
    }

    @Override
    public int getItemCount() {
        return favoriteItems.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout favoriteVideoLayout;
        ImageView favoriteVideoThumbnail;
        TextView favoriteVideoTitle, favoriteVideoDuration, favoriteVideoChannel,
                favoriteVideoPublished, favoriteVideoViews, favoriteVideoMenu, favoriteVideoRemove;

        public ItemViewHolder(View itemView) {
            super(itemView);
            favoriteVideoLayout = itemView.findViewById(R.id.favorite_video_layout);
            favoriteVideoThumbnail = itemView.findViewById(R.id.favorite_video_thumbnail);
            favoriteVideoTitle = itemView.findViewById(R.id.favorite_video_title);
            favoriteVideoDuration = itemView.findViewById(R.id.favorite_video_duration);
            favoriteVideoChannel = itemView.findViewById(R.id.favorite_video_channel);
            favoriteVideoPublished = itemView.findViewById(R.id.favorite_video_published_at);
            favoriteVideoViews = itemView.findViewById(R.id.favorite_video_views);
            favoriteVideoMenu = itemView.findViewById(R.id.favorite_video_menu);
            favoriteVideoRemove = itemView.findViewById(R.id.favorite_video_remove);
            Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "fonts/material.ttf");
            favoriteVideoMenu.setTypeface(typeface);
            favoriteVideoRemove.setTypeface(typeface);

        }
    }
}

