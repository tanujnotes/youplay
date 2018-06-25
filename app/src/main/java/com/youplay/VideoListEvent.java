package com.youplay;

/**
 * Created by tan on 19/05/17.
 **/

public class VideoListEvent {
    private final ItemModel videoListModel;
    private final boolean replaceQueue;
    private final String className;

    public VideoListEvent(ItemModel videoListModel, boolean replaceQueue, String className) {
        this.videoListModel = videoListModel;
        this.replaceQueue = replaceQueue;
        this.className = className;
    }

    public ItemModel getVideoListModel() {
        return videoListModel;
    }

    public boolean getReplaceQueue() {
        return replaceQueue;
    }

    public String getClassName() {
        return className;
    }
}
