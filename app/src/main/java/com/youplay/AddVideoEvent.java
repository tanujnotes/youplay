package com.youplay;

/**
 * Created by tan on 19/05/17.
 **/

public class AddVideoEvent {
    private final ItemModel.Item videoItem;
    private final boolean playNext;

    public AddVideoEvent(ItemModel.Item videoItem, boolean playNext) {
        this.videoItem = videoItem;
        this.playNext = playNext;
    }

    public ItemModel.Item getVideoItem() {
        return videoItem;
    }

    public boolean isPlayNext() {
        return playNext;
    }
}
