package com.youplay;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pierfrancescosoffritti.youtubeplayer.player.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayerFullScreenListener;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayerInitListener;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayerView;
import com.pierfrancescosoffritti.youtubeplayer.ui.PlayerUIController;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.DeleteQuery;
import org.greenrobot.greendao.query.QueryBuilder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by tan on 17/05/17.
 **/

public class VideoService extends Service implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    protected static final String TAG = "FloatingVideoService";
    protected static int WINDOW_MANAGER_OVERLAY_TYPE;

    @Inject
    PreferenceManager pm;

    @Inject
    NetworkService networkService;

    @Inject
    DaoSession daoSession;

    private static final int MAX_CLICK_DURATION = 200;
    private int x = 8, y = 8;
    private WindowManager windowManager;
    private TextView helperText;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer = null;
    private WindowManager.LayoutParams params;
    private long startClickTime;
    private List<ItemModel.Item> playlistVideoList = new ArrayList<>();
    private List<ItemModel.Item> recommendationsVideoList = new ArrayList<>();
    private FirebaseAnalytics firebaseAnalytics;
    private LockScreenReceiver lockScreenReceiver;
    private View root;
    private HardwareKeyWatcher hardwareKeyWatcher;
    private LinearLayout queueMenuLayout, controlsLayout, tabLayout;
    private NowPlayListAdapter videoAdapter;
    private RecommendationsAdapter recommendationsAdapter;
    private ItemModel.Item videoCurrentlyPlaying;
    private FrameLayout floatingControlsLayout, youtubeViewContainer;
    private ViewPager viewPager;
    private TextView videoTitle;
    private TextView channelTitle;
    private TextView floatingPlayPause;
    private TextView favorite;
    private TextView playbackRepeat;
    private TextView playbackShuffle;
    private TextView queueTab;
    private TextView recommendationsTab;
    private TextView floatingNextVideo;
    private Switch autoplaySwitch;
    private PlayerUIController playerUIController;
    private boolean videoJustEnded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ((MyApp) getApplication()).getNetComponent().inject(this);
        ((MyApp) getApplication()).setVideoServiceRunning(true);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        EventBus.getDefault().register(this);
        registerLockScreenReceiver();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            WINDOW_MANAGER_OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
        else WINDOW_MANAGER_OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        Display mDisplay = windowManager.getDefaultDisplay();
        Point size = new Point();
        mDisplay.getSize(size);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WINDOW_MANAGER_OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 16;
        params.y = 16 + Utils.getStatusBarHeight(getResources());

        helperText = new TextView(this);
        helperText.setText(R.string.drag_to_move);
        helperText.setBackgroundColor(Color.TRANSPARENT);
        helperText.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        helperText.setPadding(8, 4, 4, 0);

        hardwareKeyWatcher = new HardwareKeyWatcher(this);
        hardwareKeyWatcher.setOnHardwareKeysPressedListenerListener(new HardwareKeyWatcher.OnHardwareKeysPressedListener() {
            @Override
            public void onHomePressed() {
                if (youTubePlayerView.isFullScreen())
                    youTubePlayerView.exitFullScreen();
                playerSmallSize();
            }

            @Override
            public void onRecentAppsPressed() {
                if (youTubePlayerView.isFullScreen())
                    youTubePlayerView.exitFullScreen();
                playerSmallSize();
            }
        });
        hardwareKeyWatcher.startWatch();

        Typeface materialTypeface = Typeface.createFromAsset(getAssets(), "fonts/material.ttf");
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        root = layoutInflater.inflate(R.layout.video_service_layout, null);
        queueMenuLayout = root.findViewById(R.id.queue_menu_layout);
        controlsLayout = root.findViewById(R.id.controls_layout);
        tabLayout = root.findViewById(R.id.tab_layout);
        viewPager = root.findViewById(R.id.service_viewpager);
        viewPager.setAdapter(new CustomPagerAdapter(this));

        RecyclerView recyclerView = root.findViewById(R.id.queue_recyclerview);
        videoAdapter = new NowPlayListAdapter(getApplicationContext(), playlistVideoList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(videoAdapter);

        RecyclerView recommendationsRV = root.findViewById(R.id.recommendations_recyclerview);
        recommendationsAdapter = new RecommendationsAdapter(getApplicationContext(), recommendationsVideoList);
        recommendationsRV.setLayoutManager(new LinearLayoutManager(this));
        recommendationsRV.setAdapter(recommendationsAdapter);

        videoTitle = root.findViewById(R.id.now_playing_video_title);
        channelTitle = root.findViewById(R.id.channel_title);
        TextView goHome = root.findViewById(R.id.go_home);
        favorite = root.findViewById(R.id.video_favorite);
//        TextView videoShare = root.findViewById(R.id.video_share);
        autoplaySwitch = root.findViewById(R.id.autoplay_switch);
        autoplaySwitch.setChecked(pm.getIsAutoplayOn());
        TextView closeWindow = root.findViewById(R.id.close_window);
        TextView back = root.findViewById(R.id.back);
        playbackRepeat = root.findViewById(R.id.playback_repeat);
        playbackShuffle = root.findViewById(R.id.playback_shuffle);
        TextView floatingMenu = root.findViewById(R.id.floating_menu);
        floatingPlayPause = root.findViewById(R.id.floating_play_pause);
        floatingNextVideo = root.findViewById(R.id.floating_next_video);
        floatingControlsLayout = root.findViewById(R.id.floating_controls_layout);
        youtubeViewContainer = root.findViewById(R.id.youtube_view_container);
        queueTab = root.findViewById(R.id.queue_tab);
        recommendationsTab = root.findViewById(R.id.recommendations_tab);
        goHome.setTypeface(materialTypeface);
        favorite.setTypeface(materialTypeface);
//        videoShare.setTypeface(materialTypeface);
        closeWindow.setTypeface(materialTypeface);
        back.setTypeface(materialTypeface);
        playbackRepeat.setTypeface(materialTypeface);
        playbackShuffle.setTypeface(materialTypeface);
        floatingMenu.setTypeface(materialTypeface);
        floatingPlayPause.setTypeface(materialTypeface);
        floatingNextVideo.setTypeface(materialTypeface);
        goHome.setOnClickListener(this);
        favorite.setOnClickListener(this);
//        videoShare.setOnClickListener(this);
        closeWindow.setOnClickListener(this);
        back.setOnClickListener(this);
        playbackRepeat.setOnClickListener(this);
        playbackShuffle.setOnClickListener(this);
        floatingMenu.setOnClickListener(this);
        floatingPlayPause.setOnClickListener(this);
        floatingNextVideo.setOnClickListener(this);
        queueTab.setOnClickListener(this);
        recommendationsTab.setOnClickListener(this);
        queueTab.setPaintFlags(queueTab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        recommendationsTab.setPaintFlags(0);

        populateRepeatIcon();
        populateShuffleIcon();

        helperText = root.findViewById(R.id.helper_text);
        youTubePlayerView = root.findViewById(R.id.youtube_view);
        playerUIController = youTubePlayerView.getPlayerUIController();
        FrameLayout.LayoutParams collapsedParams;
        RelativeLayout.LayoutParams floatingControlsParams;
        if (pm.getIsPlayerLarge()) {
            collapsedParams = new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width_large), ViewGroup.LayoutParams.WRAP_CONTENT);
            floatingControlsParams = new RelativeLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width_large), ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            collapsedParams = new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width), ViewGroup.LayoutParams.WRAP_CONTENT);
            floatingControlsParams = new RelativeLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        youTubePlayerView.setLayoutParams(collapsedParams);
        floatingControlsLayout.setLayoutParams(floatingControlsParams);
        playerUIController.showUI(false);
        playerUIController.showMenuButton(true);
        youTubePlayerView.findViewById(R.id.youtube_button).setVisibility(View.GONE);
        youTubePlayerView.findViewById(R.id.controls_root).setBackgroundColor(ContextCompat.getColor(this, R.color.black_transparent));
        initializePlayer("");
        initializeListeners();

        helperText.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        return true;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration <= MAX_CLICK_DURATION) {
                            helperText.setVisibility(View.GONE);
                            playerFullSize();
                            return true;
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        x = params.x;
                        y = params.y;
                        windowManager.updateViewLayout(root, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(root, params);
    }

    private void initializePlayer(final String videoId) {
        youTubePlayerView.initialize(new YouTubePlayerInitListener() {
            @Override
            public void onInitSuccess(final YouTubePlayer initializedYouTubePlayer) {
                initializedYouTubePlayer.addListener(new AbstractYouTubePlayerListener() {
                    @Override
                    public void onReady() {
                        youTubePlayer = initializedYouTubePlayer;
                        if (videoId == null || videoId.isEmpty()) return;
                        initializedYouTubePlayer.loadVideo(videoId, 0);
                        root.findViewById(R.id.video_buffering_bar).setVisibility(View.GONE);
                    }

                    @Override
                    public void onStateChange(int state) {
                        switch (state) {
                            case PlayerConstants.PlayerState.UNSTARTED:
                                break;
                            case PlayerConstants.PlayerState.ENDED:
                                // To fix the bug where ENDED is being called twice consecutively
                                if (videoJustEnded) return;
                                videoJustEnded = true;
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        videoJustEnded = false;
                                    }
                                }, 2000);

                                Bundle bundle = new Bundle();
                                bundle.putString(AppConstants.Param.VIDEO_ID, pm.getPlaybackVideoId());
                                firebaseAnalytics.logEvent(AppConstants.Event.VIDEO_ENDED_FLOATING, bundle);
                                floatingPlayPause.setText(R.string.ic_play);
                                nextVideoPlease(false);
                                break;
                            case PlayerConstants.PlayerState.PLAYING:
                                playerUIController.showCustomAction1(true);
                                playerUIController.showCustomAction2(true);
                                if (helperText != null) helperText.setText("");
                                floatingPlayPause.setText(R.string.ic_pause);
                                break;
                            case PlayerConstants.PlayerState.PAUSED:
                                floatingPlayPause.setText(R.string.ic_play);
                                break;
                            case PlayerConstants.PlayerState.BUFFERING:
//                                progressBar.setVisibility(View.VISIBLE);
                                break;
                            case PlayerConstants.PlayerState.VIDEO_CUED:
                                break;
                        }
                    }

                    @Override
                    public void onError(int errorCode) {
                        if (playlistVideoList.size() > 1) {
                            nextVideoPlease(true);
                        } else {
                            Utils.showToast(getApplicationContext(), "YouPlay - video not found!");
                            stopSelf();
                        }
                    }
                });
            }
        }, true);
    }

    private void initializeListeners() {
        playerUIController.setCustomAction1(ContextCompat.getDrawable(this, R.drawable.ic_prev), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoAdapter.playPrevVideo();
            }
        });

        playerUIController.setCustomAction2(ContextCompat.getDrawable(this, R.drawable.ic_next), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextVideoPlease(true);
            }
        });

        youTubePlayerView.addFullScreenListener(new YouTubePlayerFullScreenListener() {
            @Override
            public void onYouTubePlayerEnterFullScreen() {
                controlsLayout.setVisibility(View.GONE);
                tabLayout.setVisibility(View.GONE);
                params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                windowManager.updateViewLayout(root, params);
                RelativeLayout.LayoutParams youtubeViewContainerParams = (RelativeLayout.LayoutParams) youtubeViewContainer.getLayoutParams();
                youtubeViewContainerParams.setMargins(0, 0, 0, 0);
                youtubeViewContainer.setLayoutParams(youtubeViewContainerParams);

                Bundle bundle = new Bundle();
                bundle.putString(AppConstants.Param.VIDEO_ID, pm.getPlaybackVideoId());
                firebaseAnalytics.logEvent(AppConstants.Event.FULLSCREEN_ENTER, bundle);
            }

            @Override
            public void onYouTubePlayerExitFullScreen() {
                controlsLayout.setVisibility(View.VISIBLE);
                tabLayout.setVisibility(View.VISIBLE);
                params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                windowManager.updateViewLayout(root, params);
                RelativeLayout.LayoutParams youtubeViewContainerParams = (RelativeLayout.LayoutParams) youtubeViewContainer.getLayoutParams();
                youtubeViewContainerParams.setMargins(0, (int) getResources().getDimension(R.dimen.video_player_margin_top), 0, 0);
                youtubeViewContainer.setLayoutParams(youtubeViewContainerParams);

                Bundle bundle = new Bundle();
                bundle.putString(AppConstants.Param.VIDEO_ID, pm.getPlaybackVideoId());
                firebaseAnalytics.logEvent(AppConstants.Event.FULLSCREEN_EXIT, bundle);
            }
        });

        playerUIController.setCustomMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context contextThemeWrapper = new ContextThemeWrapper(getApplicationContext(), R.style.AppTheme);
                PopupMenu popup = new PopupMenu(contextThemeWrapper, floatingNextVideo);
                popup.getMenuInflater().inflate(R.menu.menu_queue, popup.getMenu());
                popup.setOnMenuItemClickListener(VideoService.this);
                popup.show();
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("onPageScrolled", position + " --- " + positionOffset + " --- " + positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                Log.d("onPageSelected", position + "");
                if (position == 0) {
                    queueTab.setTextColor(getResources().getColor(R.color.grey_300));
                    queueTab.setPaintFlags(queueTab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    recommendationsTab.setTextColor(getResources().getColor(R.color.light_grey));
                    recommendationsTab.setPaintFlags(0);
                } else if (position == 1) {
                    queueTab.setTextColor(getResources().getColor(R.color.light_grey));
                    queueTab.setPaintFlags(0);
                    recommendationsTab.setTextColor(getResources().getColor(R.color.grey_300));
                    recommendationsTab.setPaintFlags(recommendationsTab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d("pageScrollStateChanged", state + "");
            }
        });

        autoplaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                pm.setIsAutoplayOn(isChecked);
                if (isChecked) Utils.showToast(getApplicationContext(), "Autoplay is on!");
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().equals(AppConstants.SHARE_VIDEO_ACTION)) {
            Utils.sendShareVideoIntent(getApplicationContext(),
                    videoCurrentlyPlaying.getSnippet().getResourceId().getVideoId(),
                    videoCurrentlyPlaying.getSnippet().getTitle());
            playerSmallSize();
            firebaseAnalytics.logEvent(AppConstants.Event.FOREGROUND_VIDEO_SHARE, new Bundle());
            return START_NOT_STICKY;
        } else if (intent.getAction() != null && intent.getAction().equals(AppConstants.STOP_FOREGROUND_ACTION)) {
            stopForeground(true);
            stopSelf();
            firebaseAnalytics.logEvent(AppConstants.Event.FOREGROUND_VIDEO_CLOSE, new Bundle());
            return START_NOT_STICKY;
        }

        populateQueueFromPreference();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MyApp) getApplication()).setVideoServiceRunning(false);
        EventBus.getDefault().unregister(this);
        unregisterReceiver(lockScreenReceiver);
        try {
            youTubePlayerView.release();
            windowManager.removeView(root);
            hardwareKeyWatcher.stopWatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    private void playerFullSize() {
        queueMenuLayout.setVisibility(View.VISIBLE);
        controlsLayout.setVisibility(View.VISIBLE);
        floatingControlsLayout.setVisibility(View.GONE);
        tabLayout.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.VISIBLE);

        x = params.x;
        y = params.y;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WINDOW_MANAGER_OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        windowManager.updateViewLayout(root, params);

        FrameLayout.LayoutParams expandedParams =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        youTubePlayerView.setLayoutParams(expandedParams);
        playerUIController.showUI(true);
        helperText.setVisibility(View.GONE);
    }

    private void playerSmallSize() {
        controlsLayout.setVisibility(View.GONE);
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        queueMenuLayout.setVisibility(View.GONE);
        floatingControlsLayout.setVisibility(View.VISIBLE);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WINDOW_MANAGER_OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.x = x;
        params.y = y;
        windowManager.updateViewLayout(root, params);

        FrameLayout.LayoutParams collapsedParams;
        RelativeLayout.LayoutParams floatingControlsParams;
        if (pm.getIsPlayerLarge()) {
            collapsedParams = new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width_large), ViewGroup.LayoutParams.WRAP_CONTENT);
            floatingControlsParams = new RelativeLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width_large), ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            collapsedParams = new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width), ViewGroup.LayoutParams.WRAP_CONTENT);
            floatingControlsParams = new RelativeLayout.LayoutParams((int) getResources().getDimension(R.dimen.youtube_view_collapsed_width), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        RelativeLayout.LayoutParams layoutParams
                = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        root.setLayoutParams(layoutParams);

        youTubePlayerView.setLayoutParams(collapsedParams);
        floatingControlsLayout.setLayoutParams(floatingControlsParams);

        RelativeLayout.LayoutParams youtubeViewContainerParams = (RelativeLayout.LayoutParams) youtubeViewContainer.getLayoutParams();
        youtubeViewContainerParams.setMargins(0, (int) getResources().getDimension(R.dimen.video_player_margin_top), 0, 0);
        youtubeViewContainer.setLayoutParams(youtubeViewContainerParams);

        playerUIController.showUI(false);
        helperText.setVisibility(View.VISIBLE);
    }

    public void playThisVideo(ItemModel.Item video) {
        if (video.getSnippet().getResourceId() == null) {
            ItemModel.ResourceId resourceId = new ItemModel.ResourceId();
            resourceId.setVideoId(video.getSnippet().getPlaylistId());
            video.getSnippet().setResourceId(resourceId);
        }
        String videoId = video.getSnippet().getResourceId().getVideoId();
        if (youTubePlayer != null) youTubePlayer.loadVideo(videoId, 0);
        else initializePlayer(videoId);
        pm.setPlaybackVideoId(videoId);
        videoTitle.setText(video.getSnippet().getTitle());
        playerUIController.setVideoTitle(video.getSnippet().getTitle());
        showForegroundNotification(video.getSnippet().getTitle(), video.getSnippet().getChannelTitle());
        channelTitle.setText(video.getSnippet().getChannelTitle());

        videoCurrentlyPlaying = video;
        getVideoRecommendations();

        Bundle bundle = new Bundle();
        bundle.putString(AppConstants.Param.VIDEO_ID, videoId);
        bundle.putString(AppConstants.Param.PLAYLIST_ID, video.getSnippet().getPlaylistId());
        bundle.putString(AppConstants.Param.CHANNEL_ID, video.getSnippet().getChannelId());
        firebaseAnalytics.logEvent(AppConstants.Event.PLAY_VIDEO_FLOATING, bundle);

        try {
            // Set favorite searchSuggestionIcon
            FavoriteDaoModelDao favoriteDao = daoSession.getFavoriteDaoModelDao();
            long videoCount = favoriteDao.queryBuilder()
                    .where(FavoriteDaoModelDao.Properties.VideoId.eq(video.getSnippet().getResourceId().getVideoId()))
                    .count();
            if (videoCount == 0) favorite.setText(R.string.ic_favorite_off);
            else favorite.setText(R.string.ic_favorite_on);

            // Save video to Watch History
            HistoryDaoModelDao historyDao = daoSession.getHistoryDaoModelDao();
            long historyVideoCount = historyDao.queryBuilder().count();
            QueryBuilder<HistoryDaoModel> queryBuilder = historyDao.queryBuilder();

            // Delete last 100 videos from Watch History if total count > 500
            if (historyVideoCount > 500) {
                List<HistoryDaoModel> historyVideos = queryBuilder.orderDesc(HistoryDaoModelDao.Properties.TimeInMillis).build().list();
                long time = historyVideos.get(100).getTimeInMillis();

                final DeleteQuery<HistoryDaoModel> tableDeleteQuery = daoSession.queryBuilder(HistoryDaoModel.class)
                        .where(HistoryDaoModelDao.Properties.TimeInMillis.lt(time))
                        .buildDelete();
                tableDeleteQuery.executeDeleteWithoutDetachingEntities();
                daoSession.clear();
            }

            if (historyVideoCount == 0) {
                addCurrentVideoToWatchHistory(historyDao);
            } else {
                // Don't add the same video in a row
                HistoryDaoModel latestHistoryVideo = queryBuilder.orderDesc(HistoryDaoModelDao.Properties.TimeInMillis).limit(1).build().list().get(0);
                if (!Objects.equals(latestHistoryVideo.getVideoId(), pm.getPlaybackVideoId())) {
                    addCurrentVideoToWatchHistory(historyDao);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        viewPager.setCurrentItem(0);
    }

    private void addCurrentVideoToWatchHistory(HistoryDaoModelDao historyDao) {
        if (videoCurrentlyPlaying == null) return;

        String videoObject = new Gson().toJson(videoCurrentlyPlaying);
        HistoryDaoModel historyDaoModel = new HistoryDaoModel(
                null,
                pm.getPlaybackVideoId(),
                System.currentTimeMillis(),
                videoObject
        );
        historyDao.insertOrReplace(historyDaoModel);
        EventBus.getDefault().post(new WatchHistoryRefreshEvent());
    }

    private void getVideoRecommendations() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 50);
        map.put("type", "video");
        map.put("relatedToVideoId", videoCurrentlyPlaying.getSnippet().getResourceId().getVideoId());
        map.put("key", BuildConfig.youtube_key);
        networkService.getSearchResults(map)
                .flatMap(new Func1<ItemModel, Observable<ItemModel>>() {
                    @Override
                    public Observable<ItemModel> call(ItemModel itemModel) {
                        StringBuilder videoIds = new StringBuilder();
                        for (ItemModel.Item item : itemModel.getItems())
                            videoIds.append(item.getSnippet().getResourceId().getVideoId()).append(",");

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("part", "snippet,contentDetails,statistics");
                        map.put("id", videoIds.toString().substring(0, videoIds.length() - 1));
                        map.put("key", BuildConfig.youtube_key);
                        return networkService.getVideos(map);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ItemModel>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Error: " + e.getMessage());
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        Log.d(TAG, "Recommendations");
                        recommendationsVideoList.clear();
                        recommendationsVideoList.addAll(model.getItems());
                        recommendationsAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void nextVideoPlease(boolean justPlayNext) {
        if (pm.getIsAutoplayOn() && recommendationsVideoList.size() > 5) {
            videoAdapter.addVideo(videoAdapter.currentVideoCount, recommendationsVideoList.get(new Random().nextInt(5)));
            videoAdapter.notifyDataSetChanged();
            playThisVideo(recommendationsVideoList.get(0));
            return;
        }
        if ((pm.getPlaybackRepeat() == 1) || (pm.getPlaybackRepeat() == 2 && videoAdapter.getItemCount() == 1)) {
            youTubePlayer.seekTo(1);
            return;
        }
        videoAdapter.playNextVideo(justPlayNext, pm.getPlaybackRepeat(), pm.getPlaybackShuffle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(AddVideoEvent event) {
        if (event == null || event.getVideoItem() == null) return;
        if (videoAdapter.getItemCount() == 0) {
            playlistVideoList.clear();
            playlistVideoList.add(event.getVideoItem());
            videoAdapter.notifyDataSetChanged();
            videoAdapter.currentVideoCount = 0;
            playThisVideo(event.getVideoItem());
            return;
        }

        if (event.isPlayNext()) {
            videoAdapter.addVideo(videoAdapter.currentVideoCount + 1, event.getVideoItem());
            videoAdapter.notifyDataSetChanged();
        } else {
            videoAdapter.addVideo(videoAdapter.getItemCount(), event.getVideoItem());
            videoAdapter.notifyDataSetChanged();
        }
        Bundle bundle = new Bundle(); // Playlist id is video id
        bundle.putString(AppConstants.Param.VIDEO_ID, event.getVideoItem().getSnippet().getPlaylistId());
        bundle.putString(AppConstants.Param.CHANNEL_ID, event.getVideoItem().getSnippet().getChannelId());
        firebaseAnalytics.logEvent(AppConstants.Event.ADD_VIDEO_FLOATING, bundle);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(PlayVideoEvent event) {
        if (event == null || event.getVideoItem() == null) return;
        if (event.isFromQueue()) {
            playThisVideo(event.getVideoItem());
            return;
        }
        if (videoAdapter.getItemCount() == 0) {
            playlistVideoList.clear();
            playlistVideoList.add(event.getVideoItem());
            videoAdapter.notifyDataSetChanged();
            videoAdapter.currentVideoCount = 0;
        } else {
            videoAdapter.addVideo(videoAdapter.currentVideoCount, event.getVideoItem());
            videoAdapter.notifyDataSetChanged();
        }
        playThisVideo(event.getVideoItem());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(VideoListEvent event) {
        if (event.getVideoListModel() == null) return;
        if (event.getReplaceQueue()) playlistVideoList.clear();
        autoplaySwitch.setChecked(false);

        playlistVideoList.addAll(event.getVideoListModel().getItems());
        videoAdapter.notifyDataSetChanged();

        if (event.getReplaceQueue()) {
            videoAdapter.currentVideoCount = 0;
            playThisVideo(event.getVideoListModel().getItems().get(0));
        }
        Bundle bundle = new Bundle();
        bundle.putString(AppConstants.Param.FROM_CLASS, event.getClassName());
        bundle.putString(AppConstants.Param.PLAYLIST_ID, event.getVideoListModel().getItems().get(0).getSnippet().getPlaylistId());
        bundle.putString(AppConstants.Param.CHANNEL_ID, event.getVideoListModel().getItems().get(0).getSnippet().getChannelId());
        if (event.getReplaceQueue())
            firebaseAnalytics.logEvent(AppConstants.Event.PLAY_ALL_FROM_PLAYLIST, bundle);
        else
            firebaseAnalytics.logEvent(AppConstants.Event.ADD_ALL_FROM_PLAYLIST, bundle);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(ServiceEvent event) {
        switch (event.getEvent()) {
            case AppConstants.EVENT_CLEAR_QUEUE:
                clearQueue();
                break;

            case AppConstants.EVENT_SET_TIMER:
                Utils.showToast(this.getApplicationContext(),
                        "YouPlay will close after " + event.getTimerValue() + " minutes.");
                Bundle bundle = new Bundle();
                bundle.putInt(AppConstants.Param.TIMER_VALUE, event.getTimerValue());
                firebaseAnalytics.logEvent(AppConstants.Event.TIMER_SET, bundle);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                    }
                }, event.getTimerValue() * 60 * 1000);
                break;

            case AppConstants.EVENT_SHARE_VIDEO:
                // TODO: 20/05/17 Crashing! Not sure why.
//                Utils.sendShareVideoIntent(getApplicationContext(), event.getTimerValue());
//                playerSmallSize();
                break;
        }
    }

    private void registerLockScreenReceiver() {
        lockScreenReceiver = new VideoService.LockScreenReceiver();
        IntentFilter lockFilter = new IntentFilter();
        lockFilter.addAction(Intent.ACTION_SCREEN_ON);
        lockFilter.addAction(Intent.ACTION_SCREEN_OFF);
        lockFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(lockScreenReceiver, lockFilter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video_favorite:
                toggleFavIcon();
                break;

//            case R.id.video_share:
//                Utils.sendShareVideoIntent(getApplicationContext(),
//                        videoCurrentlyPlaying.getSnippet().getResourceId().getVideoId(),
//                        videoCurrentlyPlaying.getSnippet().getTitle());
//                playerSmallSize();
//                break;

            case R.id.close_window:
                stopSelf();
                break;

            case R.id.playback_repeat:
                if (pm.getPlaybackRepeat() < 2) pm.setPlaybackRepeat(pm.getPlaybackRepeat() + 1);
                else pm.setPlaybackRepeat(0);
                populateRepeatIcon();
                break;

            case R.id.go_home:
                playerSmallSize();
                startMainActivity();
                break;

            case R.id.playback_shuffle:
                if (pm.getPlaybackShuffle()) pm.setPlaybackShuffle(false);
                else pm.setPlaybackShuffle(true);
                populateShuffleIcon();
                break;

            case R.id.back:
                if (videoAdapter.editQueue) {
                    videoAdapter.editQueue = false;
                    videoAdapter.notifyDataSetChanged();
                } else {
                    playerSmallSize();
                }
                break;

            case R.id.floating_next_video:
                if (youTubePlayer == null) return;
                nextVideoPlease(true);
                firebaseAnalytics.logEvent(AppConstants.Event.FLOATING_VIDEO_NEXT, new Bundle());
                break;

            case R.id.floating_play_pause:
                if (youTubePlayer == null) return;
                if (youTubePlayer.getCurrentState() == 1) {
                    youTubePlayer.pause();
                    floatingPlayPause.setText(R.string.ic_play);
                } else {
                    youTubePlayer.play();
                    floatingPlayPause.setText(R.string.ic_pause);
                }
                break;

            case R.id.floating_menu:
                Context contextThemeWrapper = new ContextThemeWrapper(getApplicationContext(), R.style.AppTheme);
                PopupMenu popup = new PopupMenu(contextThemeWrapper, floatingPlayPause);
                popup.getMenuInflater().inflate(R.menu.floating_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(VideoService.this);
                popup.show();
                break;

            case R.id.queue_tab:
                viewPager.setCurrentItem(0);
                break;

            case R.id.recommendations_tab:
                viewPager.setCurrentItem(1);
                break;
        }
    }

    private void toggleFavIcon() {
        FavoriteDaoModelDao favoriteDao = daoSession.getFavoriteDaoModelDao();
        if (favorite.getText().equals(getString(R.string.ic_favorite_on))) {
            favorite.setText(getString(R.string.ic_favorite_off));
            FavoriteDaoModel entity = favoriteDao.queryBuilder()
                    .where(FavoriteDaoModelDao.Properties.VideoId.eq(pm.getPlaybackVideoId())).build().unique();
            favoriteDao.delete(entity);
            Bundle bundle = new Bundle();
            bundle.putString(AppConstants.Param.VIDEO_ID, pm.getPlaybackVideoId());
            firebaseAnalytics.logEvent(AppConstants.Event.FAVOURITE_REMOVE_VIDEO, bundle);

        } else {
            favorite.setText(getString(R.string.ic_favorite_on));
            if (videoCurrentlyPlaying == null) return;
            Gson gson = new Gson();
            String videoObject = gson.toJson(videoCurrentlyPlaying);
            FavoriteDaoModel favoriteDaoModel = new FavoriteDaoModel(
                    null,
                    pm.getPlaybackVideoId(),
                    System.currentTimeMillis(),
                    videoObject
            );
            favoriteDao.insertOrReplace(favoriteDaoModel);

            Bundle bundle = new Bundle();
            bundle.putString(AppConstants.Param.VIDEO_ID, pm.getPlaybackVideoId());
            firebaseAnalytics.logEvent(AppConstants.Event.FAVOURITE_ADD_VIDEO, bundle);
            EventBus.getDefault().post(new FavoriteRefreshEvent());
        }
    }

    private void populateRepeatIcon() {
        switch (pm.getPlaybackRepeat()) {
            case 0:
                playbackRepeat.setTextColor(ContextCompat.getColor(this, R.color.light_grey));
                break;
            case 1:
                playbackRepeat.setText(R.string.ic_repeat_1);
                playbackRepeat.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
            case 2:
                playbackRepeat.setText(R.string.ic_repeat);
                playbackRepeat.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
        }
    }

    private void populateShuffleIcon() {
        playbackShuffle.setTextColor(pm.getPlaybackShuffle()
                ? ContextCompat.getColor(this, R.color.white)
                : ContextCompat.getColor(this, R.color.light_grey));
    }


    private void startMainActivity() {
        Intent intent = new Intent(VideoService.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("from_service", true);
        startActivity(intent);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_edit_queue:
                videoAdapter.editQueue = true;
                videoAdapter.notifyDataSetChanged();
                firebaseAnalytics.logEvent(AppConstants.Event.START_QUEUE_EDITOR, new Bundle());
                return true;

            case R.id.menu_clear_queue:
                clearQueue();
                return true;

            case R.id.menu_share_video:
                Utils.sendShareVideoIntent(getApplicationContext(),
                        videoCurrentlyPlaying.getSnippet().getResourceId().getVideoId(),
                        videoCurrentlyPlaying.getSnippet().getTitle());
                return true;

            case R.id.menu_fullscreen:
                playerFullSize();
                youTubePlayerView.enterFullScreen();
                return true;

            case R.id.menu_change_size:
                if (youTubePlayer == null) return true;
                if (pm.getIsPlayerLarge()) {
                    pm.setIsPlayerLarge(false);
                } else {
                    pm.setIsPlayerLarge(true);
                }
                firebaseAnalytics.logEvent(AppConstants.Event.PLAYER_SIZE_CHANGED, new Bundle());
                playerSmallSize();
                return true;

            case R.id.menu_add_to_favourites:
                addVideoToFavourites();
                return true;

            case R.id.menu_video_details:
                showVideoDetailsDialog();
                return true;

            case R.id.menu_close:
                stopSelf();
                return true;

            default:
                return true;
        }
    }

    private void addVideoToFavourites() {
        FavoriteDaoModelDao favoriteDao = daoSession.getFavoriteDaoModelDao();
        if (favorite.getText().equals(getString(R.string.ic_favorite_on))) {
            Utils.showToast(getApplicationContext(), "Already in favourites");
        } else {
            if (videoCurrentlyPlaying == null) {
                Utils.showToast(getApplicationContext(), "Please try again!");
                return;
            }
            favorite.setText(getString(R.string.ic_favorite_on));
            Gson gson = new Gson();
            String videoObject = gson.toJson(videoCurrentlyPlaying);
            FavoriteDaoModel favoriteDaoModel = new FavoriteDaoModel(
                    null,
                    pm.getPlaybackVideoId(),
                    System.currentTimeMillis(),
                    videoObject
            );
            favoriteDao.insertOrReplace(favoriteDaoModel);
            Utils.showToast(getApplicationContext(), "Video added to favourites");

            Bundle bundle = new Bundle();
            bundle.putString(AppConstants.Param.VIDEO_ID, pm.getPlaybackVideoId());
            firebaseAnalytics.logEvent(AppConstants.Event.FAVOURITE_ADD_VIDEO, bundle);
            EventBus.getDefault().post(new FavoriteRefreshEvent());
        }
    }

    private void showVideoDetailsDialog() {
        if (videoCurrentlyPlaying == null) {
            Utils.showToast(getApplicationContext(), "Please try again!");
            return;
        }

        String message = "Title: " + videoCurrentlyPlaying.getSnippet().getTitle()
                + "\nChannel: " + videoCurrentlyPlaying.getSnippet().getChannelTitle()
                + "\n\nDescription: " + videoCurrentlyPlaying.getSnippet().getDescription();

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppTheme_MaterialDialogTheme);
        dialogBuilder.setTitle("Video Details");
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton("SHARE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.sendShareVideoIntent(getApplicationContext(),
                                videoCurrentlyPlaying.getSnippet().getResourceId().getVideoId(),
                                videoCurrentlyPlaying.getSnippet().getTitle());
                        dialog.dismiss();
                    }
                }
        );
        dialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );
        dialogBuilder.setNeutralButton("ADD TO FAVOURITES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addVideoToFavourites();
                        dialog.dismiss();
                    }
                }
        );

        final AlertDialog dialog = dialogBuilder.create();
        final Window dialogWindow = dialog.getWindow();
        if (dialogWindow == null) {
            Utils.showToast(getApplicationContext(), "Unable to show video details");
            return;
        }
        final WindowManager.LayoutParams dialogWindowAttributes = dialogWindow.getAttributes();

        // Set fixed width (280dp) and WRAP_CONTENT height
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialogWindowAttributes);
        layoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280, getResources().getDisplayMetrics());
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialogWindow.setAttributes(layoutParams);

        // Set to TYPE_SYSTEM_ALERT so that the Service can display it
        dialogWindow.setType(WINDOW_MANAGER_OVERLAY_TYPE);
        dialog.show();
        // Source: https://stackoverflow.com/a/33572463
    }


    public void clearQueue() {
        playlistVideoList.clear();
        videoAdapter.notifyDataSetChanged();
//        youTubePlayerView.loadVideo("", 0);
        pm.setPlaybackVideoId("");
        stopSelf();

        firebaseAnalytics.logEvent(AppConstants.Event.CLEAR_QUEUE, new Bundle());
    }

    public class LockScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            if (BuildConfig.DEBUG) return;
//            if (pm.getUserCountry().equals("IN")) return;

//            if (intent != null && intent.getAction() != null) {
//                if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
//                     Screen is on but not unlocked (if any locking mechanism present)
//                    youTubePlayer.pause();
//                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//                     Screen is locked
//                    youTubePlayer.pause();
//                } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
//                     Screen is unlocked
//                    youTubePlayer.play();
//                }
//            }
        }
    }


    private void populateQueueFromPreference() {
        if (pm.getSavedQueue().isEmpty()) {
            Utils.showToast(this, "Please try again!");
            return;
        }

        try {
            Type type = new TypeToken<List<ItemModel.Item>>() {
            }.getType();
            List<ItemModel.Item> savedVideoList = new Gson().fromJson(pm.getSavedQueue(), type);
            playlistVideoList.addAll(savedVideoList);
            videoAdapter.notifyDataSetChanged();
            playThisVideo(playlistVideoList.get(0));

            if (playlistVideoList.size() > 1) {
                autoplaySwitch.setChecked(false);
                pm.setPlaybackRepeat(2);
                populateRepeatIcon();
            } else if (autoplaySwitch.isChecked()) Utils.showToast(this, "Autoplay is on!");

        } catch (Exception e) {
            Utils.showToast(getApplicationContext(), "Please try again");
        }
    }

    private void showForegroundNotification(String videoTitle, String channelTitle) {
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.youplay_launcher_logo);

        // Intent for stopping the service
        Intent closeIntent = new Intent(this, VideoService.class);
        closeIntent.setAction(AppConstants.STOP_FOREGROUND_ACTION);
        PendingIntent pendingCloseIntent = PendingIntent.getService(this, AppConstants.STOP_FOREGROUND_ACTION_CODE, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent for share the video from notification
        Intent shareIntent = new Intent(this, VideoService.class);
        shareIntent.setAction(AppConstants.SHARE_VIDEO_ACTION);
        PendingIntent pendingShareIntent = PendingIntent.getService(this, AppConstants.SHARE_VIDEO_ACTION_CODE, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Main pending intent for the foreground notification
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                AppConstants.FOREGROUND_REQUEST_CODE,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(channelTitle)
                .setTicker("Initializing...")
                .setContentText(videoTitle)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.youplay_logo_white)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_share, "Share Video", pendingShareIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", pendingCloseIntent)
                .build();
        startForeground(R.string.app_name, notification);
    }

    private class CustomPagerAdapter extends PagerAdapter {


        public CustomPagerAdapter(Context context) {
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.queue_recyclerview; //pass id of that view to return, Views will be added in XML.
                    break;
                case 1:
                    resId = R.id.recommendations_recyclerview;
                    break;
            }
            return root.findViewById(resId);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
