package com.youplay;

/**
 * Created by tan on 19/05/17.
 **/

public class PlayVideoEvent {
    private final ItemModel.Item videoItem;
    private final boolean fromQueue;

    public PlayVideoEvent(ItemModel.Item videoItem, boolean fromQueue) {
        this.videoItem = videoItem;
        this.fromQueue = fromQueue;
    }

    public ItemModel.Item getVideoItem() {
        return videoItem;
    }

    public boolean isFromQueue() {
        return fromQueue;
    }
}
