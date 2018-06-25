package com.youplay;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by tan on 22/01/17.
 **/

@Singleton
public class PreferenceManager {

    private static final String PREF = "youplay";
    private static final String CURRENT_APP_VERSION = "current_app_version";
    private static final String PLAYBACK_REPEAT = "playback_repeat";
    private static final String PLAYBACK_SHUFFLE = "playback_shuffle";
    private static final String PLAYBACK_VIDEO_ID = "playback_video_id";

    private static final String SAVED_QUEUE = "saved_queue";
    private static final String SAVED_SEARCH_QUERIES = "saved_search_queries";
    private static final String IS_PLAYER_LARGE = "is_player_large";
    private static final String IS_AUTOPLAY_ON = "IS_AUTOPLAY_ON";

    private static final String USER_COUNTRY = "user_country";

    private SharedPreferences sharedPreferences;

    @Inject
    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private SharedPreferences getSharedPref() {
        return sharedPreferences;
    }

    public int getCurrentAppVersion() {
        return getSharedPref().getInt(CURRENT_APP_VERSION, -1);
    }

    public void setCurrentAppVersion(int appVersion) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putInt(CURRENT_APP_VERSION, appVersion);
        edit.apply();
    }

    // 0 -> No repeat
    // 1 -> Repeat one video
    // 2 -> Repeat all
    public int getPlaybackRepeat() {
        return getSharedPref().getInt(PLAYBACK_REPEAT, 2);
    }

    public void setPlaybackRepeat(int repeat) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putInt(PLAYBACK_REPEAT, repeat);
        edit.apply();
    }

    public boolean getPlaybackShuffle() {
        return getSharedPref().getBoolean(PLAYBACK_SHUFFLE, false);
    }

    public void setPlaybackShuffle(boolean shuffle) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putBoolean(PLAYBACK_SHUFFLE, shuffle);
        edit.apply();
    }

    public String getPlaybackVideoId() {
        return getSharedPref().getString(PLAYBACK_VIDEO_ID, "");
    }

    public void setPlaybackVideoId(String videoId) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putString(PLAYBACK_VIDEO_ID, videoId);
        edit.commit();
    }

    public String getSavedQueue() {
        return getSharedPref().getString(SAVED_QUEUE, "");
    }

    public void setSavedQueue(String queue) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putString(SAVED_QUEUE, queue);
        edit.commit();
    }

    public String getSavedSearchQueries() {
        return getSharedPref().getString(SAVED_SEARCH_QUERIES, "");
    }

    public void setSavedSearchQueries(String queryList) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putString(SAVED_SEARCH_QUERIES, queryList);
        edit.apply();
    }

    public boolean getIsPlayerLarge() {
        return getSharedPref().getBoolean(IS_PLAYER_LARGE, false);
    }

    public void setIsPlayerLarge(boolean isPlayerLarge) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putBoolean(IS_PLAYER_LARGE, isPlayerLarge);
        edit.apply();
    }

    public boolean getIsAutoplayOn() {
        return getSharedPref().getBoolean(IS_AUTOPLAY_ON, false);
    }

    public void setIsAutoplayOn(boolean isAutoplayOn) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putBoolean(IS_AUTOPLAY_ON, isAutoplayOn);
        edit.apply();
    }

    public String getUserCountry() {
        return getSharedPref().getString(USER_COUNTRY, "");
    }

    public void setUserCountry(String country) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putString(USER_COUNTRY, country);
        edit.apply();
    }
}
