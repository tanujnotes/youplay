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
 * Created by tan on 07/09/17.
 **/

public class HistoryVideoAdapter extends RecyclerView.Adapter<HistoryVideoAdapter.ItemViewHolder> {

    final String TAG = "HistoryVideoAdapter";
    private List<ItemModel.Item> historyItems = new ArrayList<>();
    private Activity activity;
    private HistoryVideoAdapter.ItemViewHolder videoHolder;

    public HistoryVideoAdapter(Activity activity, List<ItemModel.Item> items) {
        this.activity = activity;
        this.historyItems = items;
    }

    @Override
    public HistoryVideoAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_history_video, null);
        return new HistoryVideoAdapter.ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final HistoryVideoAdapter.ItemViewHolder holder, int position) {
        String thumbnailUrl = "";
        try {
            thumbnailUrl = historyItems.get(position).getSnippet().getThumbnails().getDefault().getUrl();
            thumbnailUrl = historyItems.get(position).getSnippet().getThumbnails().getMedium().getUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.historyVideoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((VideoInterface) activity).onVideoPlay(historyItems.get(holder.getAdapterPosition()));
            }
        });

        holder.historyVideoTitle.setText(historyItems.get(position).getSnippet().getTitle());

        try {
            holder.historyVideoChannel.setText(
                    historyItems.get(holder.getAdapterPosition()).getSnippet().getChannelTitle());
            holder.historyVideoDuration.setText(Utils.formatVideoDuration(
                    historyItems.get(holder.getAdapterPosition()).getContentDetails().getDuration()));
            holder.historyVideoViews.setText(Utils.formatViewCount(
                    historyItems.get(holder.getAdapterPosition()).getStatistics().getViewCount()));
            holder.historyVideoPublished.setText(Utils.getRelativeDay(
                    historyItems.get(position).getSnippet().getPublishedAt()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Glide.with(activity)
                .load(thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.youplay_logo_placeholder)
                .error(R.drawable.youplay_logo_placeholder)
                .override(AppConstants.GLIDE_VIDEO_THUMBNAIL_WIDTH, AppConstants.GLIDE_VIDEO_THUMBNAIL_HEIGHT)
                .into(holder.historyVideoThumbnail);
        holder.historyVideoMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.historyVideoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_playlist_video, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
            }
        });
        holder.historyVideoLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.historyVideoMenu);
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
                    ((VideoInterface) activity).onVideoAdd(historyItems.get(videoHolder.getAdapterPosition()), true);
                    return true;

                case R.id.menu_add_to_queue:
                    Utils.showToast(activity, "Added to queue");
                    ((VideoInterface) activity).onVideoAdd(historyItems.get(videoHolder.getAdapterPosition()), false);
                    return true;

                case R.id.menu_share_video:
                    String videoId = historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getResourceId().getVideoId();
                    String videoTitle = historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                    Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    return true;

                case R.id.menu_open_channel: // playlist id is video id
                    ((ChannelInterface) activity).onChannelClicked(
                            historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelTitle(),
                            historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelId(),
                            "",
                            historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId());
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
        String message = "Title: " + historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle()
                + "\n\nDescription: " + historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getDescription();
        new AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle("Video Details")
                .setMessage(message)
                .setPositiveButton("PLAY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((VideoInterface) activity).onVideoPlay(historyItems.get(videoHolder.getAdapterPosition()));
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
                        String videoId = historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId();
                        String videoTitle = historyItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                        Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout historyVideoLayout;
        ImageView historyVideoThumbnail;
        TextView historyVideoTitle, historyVideoDuration, historyVideoChannel, historyVideoPublished,
                historyVideoViews, historyVideoMenu;

        public ItemViewHolder(View itemView) {
            super(itemView);
            historyVideoLayout = itemView.findViewById(R.id.history_video_layout);
            historyVideoThumbnail = itemView.findViewById(R.id.history_video_thumbnail);
            historyVideoTitle = itemView.findViewById(R.id.history_video_title);
            historyVideoDuration = itemView.findViewById(R.id.history_video_duration);
            historyVideoChannel = itemView.findViewById(R.id.history_video_channel);
            historyVideoPublished = itemView.findViewById(R.id.history_video_published_at);
            historyVideoViews = itemView.findViewById(R.id.history_video_views);
            historyVideoMenu = itemView.findViewById(R.id.history_video_menu);
            Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "fonts/material.ttf");
            historyVideoMenu.setTypeface(typeface);
        }
    }
}


