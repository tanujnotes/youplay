package com.youplay;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
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
 * Created by tan on 20/01/17.
 **/

public class PlaylistVideoAdapter extends RecyclerView.Adapter<PlaylistVideoAdapter.ItemViewHolder> {

    final String TAG = "PlaylistVideoAdapter";
    private List<ItemModel.Item> playlistItems = new ArrayList<>();
    private Activity activity;
    private OnLoadMoreListener loadMoreListener;
    private PlaylistVideoAdapter.ItemViewHolder videoHolder;
    private boolean isLoading = false, isMoreDataAvailable = true;

    public PlaylistVideoAdapter(Activity activity, List<ItemModel.Item> items) {
        this.activity = activity;
        this.playlistItems = items;
    }

    @Override
    public PlaylistVideoAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_playlist_video, null);
        return new PlaylistVideoAdapter.ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final PlaylistVideoAdapter.ItemViewHolder holder, int position) {
        if (position >= getItemCount() - 3 && isMoreDataAvailable && !isLoading && loadMoreListener != null) {
            isLoading = true;
            loadMoreListener.onLoadMore();
        }
        String thumbnailUrl = "";
        try {
            thumbnailUrl = playlistItems.get(position).getSnippet().getThumbnails().getDefault().getUrl();
            thumbnailUrl = playlistItems.get(position).getSnippet().getThumbnails().getMedium().getUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.playlistVideoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((VideoInterface) activity).onVideoPlay(playlistItems.get(holder.getAdapterPosition()));
            }
        });
        holder.playlistVideoTitle.setText(playlistItems.get(position).getSnippet().getTitle());

        String day = Utils.getRelativeDay(playlistItems.get(position).getSnippet().getPublishedAt());
        holder.playlistVideoPublishedAt.setText(day);
        switch (day) {
            case "Today":
                holder.playlistVideoPublishedAt.setTextColor(ContextCompat.getColor(activity, R.color.green_700));
                break;
            case "Yesterday":
                holder.playlistVideoPublishedAt.setTextColor(ContextCompat.getColor(activity, R.color.colorAccentDark));
                break;
            default:
                holder.playlistVideoPublishedAt.setTextColor(ContextCompat.getColor(activity, R.color.light_grey));
                break;
        }

        try {
            holder.playlistVideoChannel.setText(
                    playlistItems.get(holder.getAdapterPosition()).getSnippet().getChannelTitle());
            holder.playlistVideoDuration.setText(Utils.formatVideoDuration(
                    playlistItems.get(holder.getAdapterPosition()).getContentDetails().getDuration()));
            holder.playlistVideoViews.setText(Utils.formatViewCount(
                    playlistItems.get(holder.getAdapterPosition()).getStatistics().getViewCount()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Glide.with(activity)
                .load(thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.youplay_logo_placeholder)
                .error(R.drawable.youplay_logo_placeholder)
                .override(AppConstants.GLIDE_VIDEO_THUMBNAIL_WIDTH, AppConstants.GLIDE_VIDEO_THUMBNAIL_HEIGHT)
                .into(holder.playlistVideoThumbnail);
        holder.playlistVideoMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.playlistVideoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_playlist_video, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
            }
        });
        holder.playlistVideoLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.playlistVideoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_playlist_video, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
                return true;
            }
        });
    }

    private PopupMenu.OnMenuItemClickListener queueVideoMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_play_next:
                    Utils.showToast(activity, "Added as next video");
                    ((VideoInterface) activity).onVideoAdd(playlistItems.get(videoHolder.getAdapterPosition()), true);
                    return true;

                case R.id.menu_add_to_queue:
                    Utils.showToast(activity, "Added to queue");
                    ((VideoInterface) activity).onVideoAdd(playlistItems.get(videoHolder.getAdapterPosition()), false);
                    return true;

                case R.id.menu_share_video:
                    // playlistId is the videoId
                    String videoId = playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId();
                    String videoTitle = playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                    Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    return true;

                case R.id.menu_open_channel: // playlist id is video id
                    ((ChannelInterface) activity).onChannelClicked(
                            playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelTitle(),
                            playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelId(),
                            "",
                            playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId());
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
        String message = "Title: " + playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle()
                + "\n\nDescription: " + playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getDescription();
        new AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle("Video Details")
                .setMessage(message)
                .setPositiveButton("PLAY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((VideoInterface) activity).onVideoPlay(playlistItems.get(videoHolder.getAdapterPosition()));
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
                        String videoId = playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId();
                        String videoTitle = playlistItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                        Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return playlistItems.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout playlistVideoLayout;
        ImageView playlistVideoThumbnail;
        TextView playlistVideoTitle, playlistVideoChannel, playlistVideoPublishedAt, playlistVideoMenu, playlistVideoDuration, playlistVideoViews;

        public ItemViewHolder(View itemView) {
            super(itemView);
            playlistVideoLayout = itemView.findViewById(R.id.playlist_video_layout);
            playlistVideoThumbnail = itemView.findViewById(R.id.playlist_video_thumbnail);
            playlistVideoTitle = itemView.findViewById(R.id.playlist_video_title);
            playlistVideoChannel = itemView.findViewById(R.id.playlist_video_channel);
            playlistVideoDuration = itemView.findViewById(R.id.playlist_video_duration);
            playlistVideoPublishedAt = itemView.findViewById(R.id.playlist_video_published_at);
            playlistVideoViews = itemView.findViewById(R.id.playlist_video_views);
            playlistVideoMenu = itemView.findViewById(R.id.playlist_video_menu);
            Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "fonts/material.ttf");
            playlistVideoMenu.setTypeface(typeface);

        }
    }

    public void setMoreDataAvailable(boolean moreDataAvailable) {
        isMoreDataAvailable = moreDataAvailable;
    }

    public void notifyDataChanged() {
        notifyDataSetChanged();
        isLoading = false;
    }

    interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }
}
