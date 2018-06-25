package com.youplay;

import android.app.Activity;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 12/01/18.
 **/

public class FeaturedPlaylistAdapter extends RecyclerView.Adapter<FeaturedPlaylistAdapter.ItemViewHolder> {
    final String TAG = "FeaturedPlaylistAdapter";
    private List<FeaturedPlaylistModel.Playlist> playlists = new ArrayList<>();
    private Activity activity;
    private ChannelPlaylistInterface channelPlaylistInterface;

    public FeaturedPlaylistAdapter(Activity activity, List<FeaturedPlaylistModel.Playlist> items) {
        this.activity = activity;
        this.playlists = items;
        this.channelPlaylistInterface = (ChannelPlaylistInterface) activity;
    }

    @Override
    public FeaturedPlaylistAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_featured_playlist, null);
        return new FeaturedPlaylistAdapter.ItemViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final FeaturedPlaylistAdapter.ItemViewHolder holder, int position) {
        holder.playlistLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelPlaylistInterface.onPlaylistClicked(
                        playlists.get(holder.getAdapterPosition()).getPlaylistTitle(),
                        playlists.get(holder.getAdapterPosition()).getPlaylistId(),
                        0,
                        "");
            }
        });
        holder.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showToastLong(activity, AppConstants.VIDEOS_PLAYING);
                ((ChannelPlaylistInterface) activity).playAllFromPlaylist(
                        playlists.get(holder.getAdapterPosition()).getPlaylistId(), true, getClass().getSimpleName());
            }
        });

        Glide.with(activity)
                .load(playlists.get(position).getPlaylistThumbnail())
                .centerCrop()
                .placeholder(R.drawable.youplay_logo_placeholder)
                .error(R.drawable.youplay_logo_placeholder)
                .override(AppConstants.GLIDE_VIDEO_THUMBNAIL_WIDTH, AppConstants.GLIDE_VIDEO_THUMBNAIL_HEIGHT)
                .into(holder.playlistThumbnail);
        holder.playlistTitle.setText(playlists.get(position).getPlaylistTitle());
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        LinearLayout playlistLayout;
        ImageView playlistThumbnail;
        TextView playlistTitle;
        FloatingActionButton floatingActionButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            playlistLayout = itemView.findViewById(R.id.featured_playlist_layout);
            playlistThumbnail = itemView.findViewById(R.id.featured_playlist_thumbnail);
            playlistTitle = itemView.findViewById(R.id.featured_playlist_title);
            floatingActionButton = itemView.findViewById(R.id.featured_playlist_play);
        }
    }
}

