package com.youplay;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by tan on 12/01/18.
 **/

public class FeaturedPlaylistModel {
    @SerializedName("playlists")
    @Expose
    private List<Playlist> playlists = null;

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
    }

    public static class Playlist {
        @SerializedName("playlist_title")
        @Expose
        String playlistTitle;
        @SerializedName("playlist_id")
        @Expose
        String playlistId;
        @SerializedName("playlist_thumbnail")
        @Expose
        String playlistThumbnail;

        public String getPlaylistTitle() {
            return playlistTitle;
        }

        public void setPlaylistTitle(String playlistTitle) {
            this.playlistTitle = playlistTitle;
        }

        public String getPlaylistId() {
            return playlistId;
        }

        public void setPlaylistId(String playlistId) {
            this.playlistId = playlistId;
        }

        public String getPlaylistThumbnail() {
            return playlistThumbnail;
        }

        public void setPlaylistThumbnail(String playlistThumbnail) {
            this.playlistThumbnail = playlistThumbnail;
        }
    }
}