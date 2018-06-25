package com.youplay;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by tan on 18/01/17.
 **/

public class NowPlayListAdapter extends RecyclerView.Adapter<NowPlayListAdapter.ItemViewHolder> {

    final String TAG = "NowPlayListAdapter";
    private List<ItemModel.Item> videoModelList = new ArrayList<>();
    Context context;
    public int currentVideoCount;
    public boolean editQueue = false;
    private NowPlayListAdapter.ItemViewHolder videoHolder;


    public NowPlayListAdapter(Context context, List<ItemModel.Item> items) {
        this.context = context;
        this.videoModelList = items;
    }

    @Override
    public NowPlayListAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_now_playlist, null);
        return new ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final NowPlayListAdapter.ItemViewHolder holder, int position) {
        if (editQueue) {
            holder.videoMenu.setVisibility(View.GONE);
            holder.videoRemove.setVisibility(View.VISIBLE);
        } else {
            holder.videoMenu.setVisibility(View.VISIBLE);
            holder.videoRemove.setVisibility(View.GONE);
        }
        if (position == currentVideoCount)
            holder.nowPlaylistVideoLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white_transparent));
        else
            holder.nowPlaylistVideoLayout.setBackgroundResource(0);

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
        holder.videoPublishedAt.setText(Utils.getRelativeDay(videoModelList.get(position).getSnippet().getPublishedAt()));

        holder.nowPlaylistVideoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.getAdapterPosition() == currentVideoCount) return;
                notifyItemChanged(currentVideoCount);
                currentVideoCount = holder.getAdapterPosition();
                notifyItemChanged(currentVideoCount);
                EventBus.getDefault().post(new PlayVideoEvent(videoModelList.get(holder.getAdapterPosition()), true));
            }
        });
        holder.videoRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeVideoFromQueue(holder);
            }
        });
        holder.videoMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoHolder = holder;
                Context contextThemeWrapper = new ContextThemeWrapper(context, R.style.AppTheme);
                PopupMenu popup = new PopupMenu(contextThemeWrapper, holder.videoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_queue_video, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
            }
        });
        holder.nowPlaylistVideoLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                videoHolder = holder;
                Context contextThemeWrapper = new ContextThemeWrapper(context, R.style.AppTheme);
                PopupMenu popup = new PopupMenu(contextThemeWrapper, holder.videoMenu);
                popup.getMenuInflater().inflate(R.menu.menu_queue_video, popup.getMenu());
                popup.setOnMenuItemClickListener(queueVideoMenuListener);
                popup.show();
                return true;
            }
        });

        try {
            holder.videoChannel.setText(videoModelList.get(position).getSnippet().getChannelTitle());
            holder.videoDuration.setText(Utils.formatVideoDuration(
                    videoModelList.get(holder.getAdapterPosition()).getContentDetails().getDuration()));
            holder.videoViews.setText(Utils.formatViewCount(
                    videoModelList.get(holder.getAdapterPosition()).getStatistics().getViewCount()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PopupMenu.OnMenuItemClickListener queueVideoMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
//                case R.id.menu_share_video:
//                    EventBus.getDefault().post(new ServiceEvent(AppConstants.EVENT_SHARE_VIDEO,
//                            videoModelList.get(videoHolder.getAdapterPosition()).getSnippet().getResourceId().getVideoId()));
//                    return true;

                case R.id.menu_remove_video:
                    removeVideoFromQueue(videoHolder);
                    return true;

//                case R.id.menu_open_video_channel:
//                    ((ChannelInterface) activity).onChannelClicked(
//                            videoModelList.get(videoHolder.getAdapterPosition()).getSnippet().getChannelTitle(),
//                            videoModelList.get(videoHolder.getAdapterPosition()).getSnippet().getChannelId(),
//                            "",
//                            videoModelList.get(videoHolder.getAdapterPosition()).getSnippet().getResourceId().getVideoId()
//                    );
//                    return true;

                default:
                    return true;
            }
        }
    };

    private void removeVideoFromQueue(ItemViewHolder viewHolder) {
        if (viewHolder == null) return;

        int itemPosition = viewHolder.getAdapterPosition();
        videoModelList.remove(itemPosition);
        notifyItemRemoved(itemPosition);
        notifyItemRangeChanged(itemPosition, videoModelList.size());
        if (getItemCount() == 0) {
            EventBus.getDefault().post(new ServiceEvent(AppConstants.EVENT_CLEAR_QUEUE));
            return;
        }
        if (itemPosition == currentVideoCount) {
            if (currentVideoCount >= getItemCount()) {
                currentVideoCount = getItemCount() - 1;
                notifyItemChanged(currentVideoCount);
            }
            EventBus.getDefault().post(new PlayVideoEvent(videoModelList.get(currentVideoCount), true));
        } else if (itemPosition < currentVideoCount) {
            currentVideoCount--;
        }
    }

    @Override
    public int getItemCount() {
        return videoModelList.size();
    }

    public void playNextVideo(boolean justPlayNext, int repeat, boolean shuffle) {
        if (getItemCount() == 0) return;
        if (justPlayNext) {
            justPlayNext(shuffle);
            return;
        }

        switch (repeat) {
            case 0:
                break;
            case 1:
                EventBus.getDefault().post(new PlayVideoEvent(videoModelList.get(currentVideoCount), true));
                break;
            case 2:
                justPlayNext(shuffle);
                break;
            default:
                break;
        }
    }

    private void justPlayNext(boolean shuffle) {
        if (shuffle) {
            currentVideoCount = getNextRandomVideoCount();
        } else {
            if (currentVideoCount + 1 < videoModelList.size()) currentVideoCount++;
            else currentVideoCount = 0;
        }

        EventBus.getDefault().post(new PlayVideoEvent(videoModelList.get(currentVideoCount), true));
        notifyDataSetChanged();
    }

    private int getNextRandomVideoCount() {
        int queueVideoCount = getItemCount();
        if (queueVideoCount <= 3) return 0;

        Random random = new Random();
        int nextVideoCountRandom = random.nextInt(queueVideoCount);
        while (currentVideoCount == nextVideoCountRandom)
            nextVideoCountRandom = random.nextInt(getItemCount());
        return nextVideoCountRandom;
    }

    public void playPrevVideo() {
        if (getItemCount() == 0) return;
        if (currentVideoCount == 0) currentVideoCount = videoModelList.size() - 1;
        else currentVideoCount--;

        EventBus.getDefault().post(new PlayVideoEvent(videoModelList.get(currentVideoCount), true));
        notifyDataSetChanged();
    }

    public void addVideo(int position, ItemModel.Item video) {
        videoModelList.add(position, video);
        notifyItemInserted(position);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout nowPlaylistVideoLayout;
        ImageView videoThumbnail;
        TextView videoTitle, videoChannel, videoMenu, videoRemove, videoDuration, videoViews, videoPublishedAt;

        ItemViewHolder(View itemView) {
            super(itemView);
            nowPlaylistVideoLayout = itemView.findViewById(R.id.now_playlist_layout);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoTitle = itemView.findViewById(R.id.video_title);
            videoChannel = itemView.findViewById(R.id.video_channel);
            videoViews = itemView.findViewById(R.id.video_views);
            videoPublishedAt = itemView.findViewById(R.id.video_published_at);
            videoMenu = itemView.findViewById(R.id.video_menu);
            videoDuration = itemView.findViewById(R.id.video_duration);
            videoRemove = itemView.findViewById(R.id.video_remove);
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/material.ttf");
            videoMenu.setTypeface(typeface);
            videoRemove.setTypeface(typeface);
        }
    }
}
