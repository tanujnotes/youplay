package com.youplay;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 09/10/17.
 **/

public class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.ItemViewHolder> {

    final String TAG = "RecommendationsAdapter";
    private List<ItemModel.Item> videoModelList = new ArrayList<>();
    Context context;
//    private RecommendationsAdapter.ItemViewHolder videoHolder;


    public RecommendationsAdapter(Context context, List<ItemModel.Item> items) {
        this.context = context;
        this.videoModelList = items;
    }

    @Override
    public RecommendationsAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_recommendations, null);
        return new RecommendationsAdapter.ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final RecommendationsAdapter.ItemViewHolder holder, int position) {
        String thumbnailUrl = "";
        try {
            thumbnailUrl = videoModelList.get(position).getSnippet().getThumbnails().getDefault().getUrl();
            thumbnailUrl = videoModelList.get(position).getSnippet().getThumbnails().getMedium().getUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Glide.with(context)
                .load(thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.youplay_logo_placeholder)
                .error(R.drawable.youplay_logo_placeholder)
                .override(AppConstants.GLIDE_VIDEO_THUMBNAIL_WIDTH, AppConstants.GLIDE_VIDEO_THUMBNAIL_HEIGHT)
                .into(holder.videoThumbnail);

        holder.videoTitle.setText(videoModelList.get(position).getSnippet().getTitle());
        holder.videoChannel.setText(videoModelList.get(position).getSnippet().getChannelTitle());
        holder.videoPublishedAt.setText(Utils.getRelativeDay(videoModelList.get(position).getSnippet().getPublishedAt()));
        holder.recommendationsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ItemModel.Item videoItem = videoModelList.get(holder.getAdapterPosition());
                EventBus.getDefault().post(new PlayVideoEvent(videoItem, false));

                Bundle bundle = new Bundle(); // Playlist id is video id
                bundle.putString(AppConstants.Param.VIDEO_ID, videoItem.getSnippet().getPlaylistId());
                bundle.putString(AppConstants.Param.CHANNEL_ID, videoItem.getSnippet().getChannelId());
                FirebaseAnalytics.getInstance(context).logEvent(AppConstants.Event.PLAY_RECOMMENDATION_VIDEO, bundle);

            }
        });
        holder.videoAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showToast(context, "Video added to queue");
                ItemModel.Item videoItem = videoModelList.get(holder.getAdapterPosition());
                EventBus.getDefault().post(new AddVideoEvent(videoModelList.get(holder.getAdapterPosition()), false));

                Bundle bundle = new Bundle(); // Playlist id is video id
                bundle.putString(AppConstants.Param.VIDEO_ID, videoItem.getSnippet().getPlaylistId());
                bundle.putString(AppConstants.Param.CHANNEL_ID, videoItem.getSnippet().getChannelId());
                FirebaseAnalytics.getInstance(context).logEvent(AppConstants.Event.ADD_RECOMMENDATION_VIDEO, bundle);
            }
        });

        try {
            holder.videoDuration.setText(Utils.formatVideoDuration(
                    videoModelList.get(holder.getAdapterPosition()).getContentDetails().getDuration()));
            holder.videoViews.setText(Utils.formatViewCount(
                    videoModelList.get(holder.getAdapterPosition()).getStatistics().getViewCount()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return videoModelList.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout recommendationsLayout;
        ImageView videoThumbnail;
        TextView videoTitle, videoChannel, videoAdd, videoDuration, videoViews, videoPublishedAt;

        ItemViewHolder(View itemView) {
            super(itemView);
            recommendationsLayout = itemView.findViewById(R.id.recommendations_layout);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoDuration = itemView.findViewById(R.id.video_duration);
            videoTitle = itemView.findViewById(R.id.video_title);
            videoChannel = itemView.findViewById(R.id.video_channel);
            videoViews = itemView.findViewById(R.id.video_views);
            videoPublishedAt = itemView.findViewById(R.id.video_published_at);
            videoAdd = itemView.findViewById(R.id.video_add);
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/material.ttf");
            videoAdd.setTypeface(typeface);
        }
    }
}

