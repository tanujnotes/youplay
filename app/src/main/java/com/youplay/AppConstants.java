package com.youplay;

/**
 * Created by tan on 24/01/17.
 **/

public class AppConstants {
    public static final String YOUPLAY = "YouPlay";
    public static final String YOUPLAY_DOWNLOAD_LINK = "https://www.dropbox.com/s/h5tl1eh0cwr7vrm/YouPlay-v1.apk?dl=1";
    public static final String YOUPLAY_DOWNLOAD_SHORT_LINK = "https://bit.ly/2Hz7Mjb";
    public static final String YOUTUBE_API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String APP_DATE_FORMAT = "MMM yyyy";
    public static final String CHAT_DATE_FORMAT = "dd MMM, HH:mm";

    public static final String EVENT_CLEAR_QUEUE = "EVENT_CLEAR_QUEUE";
    public static final String EVENT_SET_TIMER = "EVENT_SET_TIMER";
    public static final String EVENT_SHARE_VIDEO = "EVENT_SHARE_VIDEO";

    public static final String FAVORITE_FRAGMENT = "FAVORITE_FRAGMENT";
    public static final String HISTORY_FRAGMENT = "HISTORY_FRAGMENT";
    public static final String VIDEOS_ADDED_TO_QUEUE = "Videos added to queue";
    public static final String VIDEOS_PLAYING = "Playing videos...";
    public static final String STOP_FOREGROUND_ACTION = "com.youplay.stop_foreground";
    public static final String SHARE_VIDEO_ACTION = "com.youplay.share_video";

    public static final int GLIDE_VIDEO_THUMBNAIL_WIDTH = 320;
    public static final int GLIDE_VIDEO_THUMBNAIL_HEIGHT = 180;
    public static final int GLIDE_VIDEO_THUMBNAIL_WIDTH_LARGE = 640;
    public static final int GLIDE_VIDEO_THUMBNAIL_HEIGHT_LARGE = 360;

    public static final int FOREGROUND_REQUEST_CODE = 999;
    public static final int STOP_FOREGROUND_ACTION_CODE = 998;
    public static final int SHARE_VIDEO_ACTION_CODE = 997;

    public static class Param {
        public static final String VIDEO_ID = "video_id";
        public static final String PLAYLIST_ID = "playlist_id";
        public static final String CHANNEL_ID = "channel_id";
        public static final String FROM_CLASS = "from_class";
        public static final String TIMER_VALUE = "timer_value";
        public static final String SEARCH_QUERY_TEXT = "search_query_text";
        public static final String SEARCH_RESULT_SIZE = "search_result_size";
    }

    public static class Event {
        public static final String CHANNEL_SELECTED = "CHANNEL_SELECTED";
        public static final String PLAYLIST_SELECTED = "PLAYLIST_SELECTED";
        public static final String PLAY_ALL_FROM_PLAYLIST = "PLAY_ALL_FROM_PLAYLIST";
        public static final String ADD_ALL_FROM_PLAYLIST = "ADD_ALL_FROM_PLAYLIST";
        public static final String ADD_VIDEO_FLOATING = "ADD_VIDEO_FLOATING";
        public static final String ADD_RECOMMENDATION_VIDEO = "ADD_RECOMMENDATION_VIDEO";
        public static final String PLAY_VIDEO_FLOATING = "PLAY_VIDEO_FLOATING";
        public static final String PLAY_RECOMMENDATION_VIDEO = "PLAY_RECOMMENDATION_VIDEO";
        public static final String VIDEO_ENDED_FLOATING = "VIDEO_ENDED_FLOATING";
        public static final String START_QUEUE_EDITOR = "START_QUEUE_EDITOR";
        public static final String CLEAR_QUEUE = "CLEAR_QUEUE";
        public static final String APP_SHARE = "APP_SHARE";
        public static final String SEARCH_SUGGESTION_CLICKED = "SEARCH_SUGGESTION_CLICKED";
        public static final String SEARCH_VIDEO_CLICKED = "SEARCH_VIDEO_CLICKED";
        public static final String SEARCH_LOAD_MORE = "SEARCH_LOAD_MORE";
        public static final String TRENDING_LOAD_MORE = "TRENDING_LOAD_MORE";
        public static final String SEARCH_BAR_CLEAR = "SEARCH_BAR_CLEAR";
        public static final String SEARCH_QUERY = "SEARCH_QUERY";
        public static final String OPEN_FAVORITES = "OPEN_FAVORITES";
        public static final String OPEN_WATCH_HISTORY = "OPEN_WATCH_HISTORY";
        public static final String OPEN_SEARCH = "OPEN_SEARCH";
        public static final String OPEN_WITH_YOUPLAY = "OPEN_WITH_YOUPLAY";
        public static final String PLAYER_SIZE_CHANGED = "PLAYER_SIZE_CHANGED";
        public static final String FLOATING_VIDEO_NEXT = "FLOATING_VIDEO_NEXT";
        public static final String FULLSCREEN_ENTER = "FULLSCREEN_ENTER";
        public static final String FULLSCREEN_EXIT = "FULLSCREEN_EXIT";
        public static final String FAVOURITE_ADD_VIDEO = "FAVOURITE_ADD_VIDEO";
        public static final String FAVOURITE_REMOVE_VIDEO = "FAVOURITE_REMOVE_VIDEO";
        public static final String FOREGROUND_VIDEO_SHARE = "FOREGROUND_VIDEO_SHARE";
        public static final String FOREGROUND_VIDEO_CLOSE = "FOREGROUND_VIDEO_CLOSE";
        public static final String TIMER_CLICKED = "TIMER_CLICKED";
        public static final String TIMER_SET = "TIMER_SET";
        public static final String SIGN_OUT = "SIGN_OUT";
    }
}
