package com.youplay;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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

public class ChannelPlaylistAdapter extends RecyclerView.Adapter<ChannelPlaylistAdapter.ItemViewHolder> {

    final String TAG = "ChannelPlaylistAdapter";
    private List<ItemModel.Item> channelPlaylist = new ArrayList<>();
    private Activity activity;
    private ChannelPlaylistInterface channelPlaylistInterface;

    public ChannelPlaylistAdapter(Activity activity, List<ItemModel.Item> items) {
        this.activity = activity;
        this.channelPlaylist = items;
        this.channelPlaylistInterface = (ChannelPlaylistInterface) activity;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_channel_playlist, null);
        return new ChannelPlaylistAdapter.ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
        String thumbnailUrl = "";
        try {
            thumbnailUrl = channelPlaylist.get(position).getSnippet().getThumbnails().getDefault().getUrl();
            thumbnailUrl = channelPlaylist.get(position).getSnippet().getThumbnails().getMedium().getUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.channelPlaylistLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelPlaylistInterface.onPlaylistClicked(
                        channelPlaylist.get(holder.getAdapterPosition()).getSnippet().getTitle(),
                        channelPlaylist.get(holder.getAdapterPosition()).getSnippet().getPlaylistId(),
                        channelPlaylist.get(holder.getAdapterPosition()).getContentDetails().getItemCount(),
                        channelPlaylist.get(holder.getAdapterPosition()).getSnippet().getChannelId()
                );
            }
        });
        holder.playlistTitle.setText(channelPlaylist.get(position).getSnippet().getTitle());
        holder.videoCountText.setText(String.valueOf(channelPlaylist.get(position).getContentDetails().getItemCount()).concat(" videos"));

        Glide.with(activity)
                .load(thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.youplay_logo_placeholder)
                .error(R.drawable.youplay_logo_placeholder)
//                .override(AppConstants.GLIDE_VIDEO_THUMBNAIL_WIDTH, AppConstants.GLIDE_VIDEO_THUMBNAIL_HEIGHT)
                .into(holder.playlistVideoThumbnail);

        holder.playlistPlayAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showToastLong(activity, "Playing videos now");
                channelPlaylistInterface.playAllFromPlaylist(
                        channelPlaylist.get(holder.getAdapterPosition()).getSnippet().getPlaylistId(), true, TAG);
            }
        });
        holder.playlistAddAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showToastLong(activity, "Adding videos to queue");
                channelPlaylistInterface.playAllFromPlaylist(
                        channelPlaylist.get(holder.getAdapterPosition()).getSnippet().getPlaylistId(), false, TAG);
            }
        });
    }

    @Override
    public int getItemCount() {
        return channelPlaylist.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout channelPlaylistLayout;
        ImageView playlistVideoThumbnail;
        TextView playlistTitle, videoCountText, playlistPlayAll, playlistAddAll;

        public ItemViewHolder(View itemView) {
            super(itemView);
            channelPlaylistLayout = itemView.findViewById(R.id.channel_playlist_layout);
            playlistVideoThumbnail = itemView.findViewById(R.id.playlist_video_thumbnail);
            playlistTitle = itemView.findViewById(R.id.channel_playlist_title);
            videoCountText = itemView.findViewById(R.id.playlist_video_count_text);
            playlistPlayAll = itemView.findViewById(R.id.playlist_play_all);
            playlistAddAll = itemView.findViewById(R.id.playlist_add_all);
        }
    }
}
