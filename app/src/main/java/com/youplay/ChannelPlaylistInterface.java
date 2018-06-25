package com.youplay;

/**
 * Created by tan on 20/01/17.
 **/

public interface ChannelPlaylistInterface {

    void onPlaylistClicked(String playlistTitle, String playlistId, int playlistItemCount, String channelId);

    void playAllFromPlaylist(String playlistId, boolean replaceQueue, String className);

    void playAllFromPlaylist(ItemModel itemModel, boolean replaceQueue, String className);

    void showPlaylists(String playlistType);
}
