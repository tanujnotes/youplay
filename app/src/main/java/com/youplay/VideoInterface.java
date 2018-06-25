package com.youplay;

/**
 * Created by tan on 20/05/17.
 **/

public interface VideoInterface {
    void onVideoPlay(String videoId);

    void onVideoPlay(ItemModel.Item video);

    void onVideoAdd(ItemModel.Item video, boolean playNext);
}
