package com.youplay;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
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
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 07/03/17.
 **/

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ItemViewHolder> {

    final String TAG = "SearchResultAdapter";
    private List<ItemModel.Item> searchItems = new ArrayList<>();
    private Activity activity;
    private ItemViewHolder videoHolder;
    private OnLoadMoreListener loadMoreListener;
    private boolean isLoading = false, isMoreDataAvailable = true;
    private FirebaseAnalytics firebaseAnalytics;

    public SearchResultAdapter(Activity activity, List<ItemModel.Item> items) {
        this.activity = activity;
        this.searchItems = items;
        this.firebaseAnalytics = FirebaseAnalytics.getInstance(activity);
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_search_results, null);
        return new ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
        if (position >= getItemCount() - 3 && isMoreDataAvailable && !isLoading && loadMoreListener != null) {
            isLoading = true;
            loadMoreListener.onLoadMore();
        }

        String thumbnailUrl = "";
        try {
            thumbnailUrl = searchItems.get(holder.getAdapterPosition()).getSnippet().getThumbnails().getDefault().getUrl();
            thumbnailUrl = searchItems.get(holder.getAdapterPosition()).getSnippet().getThumbnails().getMedium().getUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.searchVideoTitle.setText(searchItems.get(holder.getAdapterPosition()).getSnippet().getTitle());
        holder.searchVideoChannel.setText(searchItems.get(holder.getAdapterPosition()).getSnippet().getChannelTitle());
        holder.searchVideoChannel.setTextColor(ContextCompat.getColor(activity, R.color.light_grey));

        holder.searchVideoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((VideoInterface) activity).onVideoPlay(searchItems.get(holder.getAdapterPosition()));

                Bundle bundleVideo = new Bundle(); // playlist id is video id
                bundleVideo.putString(AppConstants.Param.VIDEO_ID, searchItems.get(holder.getAdapterPosition()).getSnippet().getPlaylistId());
                bundleVideo.putString(AppConstants.Param.CHANNEL_ID, searchItems.get(holder.getAdapterPosition()).getSnippet().getChannelId());
                firebaseAnalytics.logEvent(AppConstants.Event.SEARCH_VIDEO_CLICKED, bundleVideo);
            }
        });

        try {
            holder.searchVideoPublished.setText(Utils.getRelativeDay(
                    searchItems.get(position).getSnippet().getPublishedAt()));
            holder.searchVideoDuration.setText(Utils.formatVideoDuration(
                    searchItems.get(holder.getAdapterPosition()).getContentDetails().getDuration()));
            holder.searchVideoViews.setText(Utils.formatViewCount(
                    searchItems.get(holder.getAdapterPosition()).getStatistics().getViewCount()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Glide.with(activity)
                .load(thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.youplay_logo_placeholder)
                .error(R.drawable.youplay_logo_placeholder)
                .override(AppConstants.GLIDE_VIDEO_THUMBNAIL_WIDTH, AppConstants.GLIDE_VIDEO_THUMBNAIL_HEIGHT)
                .into(holder.searchVideoThumbnail);
        holder.searchVideoMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.searchVideoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_search_result, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
            }
        });
        holder.searchVideoLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                videoHolder = holder;
                PopupMenu popup = new PopupMenu(activity, holder.searchVideoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_search_result, popup.getMenu());
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
                case R.id.menu_share_video: // playlist id is video id
                    String videoId = searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId();
                    String videoTitle = searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                    Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    return true;

                case R.id.menu_play_next:
                    Utils.showToast(activity, "Added as next video");
                    ((VideoInterface) activity).onVideoAdd(searchItems.get(videoHolder.getAdapterPosition()), true);
                    return true;

                case R.id.menu_add_to_queue:
                    Utils.showToast(activity, "Added to queue");
                    ((VideoInterface) activity).onVideoAdd(searchItems.get(videoHolder.getAdapterPosition()), false);
                    return true;

                case R.id.menu_open_channel: // playlist id is video id
                    ((ChannelInterface) activity).onChannelClicked(
                            searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelTitle(),
                            searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getChannelId(),
                            "",
                            searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId());
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
        String message = "Title: " + searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle()
                + "\n\nDescription: " + searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getDescription();
        new AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle("Video Details")
                .setMessage(message)
                .setPositiveButton("PLAY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((VideoInterface) activity).onVideoPlay(searchItems.get(videoHolder.getAdapterPosition()));
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
                        String videoId = searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getPlaylistId();
                        String videoTitle = searchItems.get(videoHolder.getAdapterPosition()).getSnippet().getTitle();
                        Utils.sendShareVideoIntent(activity, videoId, videoTitle);
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return searchItems.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout searchVideoLayout;
        ImageView searchVideoThumbnail;
        TextView searchVideoTitle, searchVideoChannel, searchVideoMenu, searchVideoDuration,
                searchVideoViews, searchVideoPublished;

        public ItemViewHolder(View itemView) {
            super(itemView);
            searchVideoLayout = itemView.findViewById(R.id.search_video_layout);
            searchVideoThumbnail = itemView.findViewById(R.id.search_video_thumbnail);
            searchVideoTitle = itemView.findViewById(R.id.search_video_title);
            searchVideoChannel = itemView.findViewById(R.id.search_video_channel);
            searchVideoDuration = itemView.findViewById(R.id.search_video_duration);
            searchVideoViews = itemView.findViewById(R.id.search_video_views);
            searchVideoPublished = itemView.findViewById(R.id.search_video_published_at);
            searchVideoMenu = itemView.findViewById(R.id.search_video_menu);
            Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "fonts/material.ttf");
            searchVideoMenu.setTypeface(typeface);
        }
    }

    public void setMoreDataAvailable(boolean moreDataAvailable) {
        isMoreDataAvailable = moreDataAvailable;
    }

    public void notifyDataChanged() {
        notifyDataSetChanged();
        isLoading = false;
    }

    public boolean isLoadingMore() {
        return isLoading;
    }

    interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }
}
