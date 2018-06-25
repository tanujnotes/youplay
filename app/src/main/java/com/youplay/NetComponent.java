package com.youplay;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by tan on 02/01/17.
 **/

@Singleton
@Component(modules = {AppModule.class, NetModule.class})
public interface NetComponent {

    void inject(SplashActivity splashActivity);

    void inject(MainActivity mainActivity);

    void inject(MainFragment mainFragment);

    void inject(FeaturedMainFragment featuredMainFragment);

    void inject(ChannelFragment channelFragment);

    void inject(ChannelPlaylistFragment channelPlaylistFragment);

    void inject(ChannelHomeFragment channelHomeFragment);

    void inject(ChannelAboutFragment channelAboutFragment);

    void inject(PlaylistFragment playlistFragment);

    void inject(VideoService videoService);

    void inject(FavoriteFragment favoriteFragment);

    void inject(HistoryFragment historyFragment);
}