package com.youplay;

import java.util.List;
import java.util.Map;

import retrofit2.Callback;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Created by tan on 02/01/17.
 **/


public interface NetworkService {

    @GET("playlistItems/")
    Observable<ItemModel> getYoutubeChannelLatestVideos(@QueryMap Map<String, Object> queries);

    @GET("playlistItems/")
    Observable<ItemModel> getPlaylistVideos(@QueryMap Map<String, Object> queries);

    @GET("search/")
    Observable<ItemModel> getPopularChannelUploads(@QueryMap Map<String, Object> queries);

    @GET("search/")
    Observable<ItemModel> getSearchResults(@QueryMap Map<String, Object> queries);

    @GET("playlists/")
    Observable<ItemModel> getPlaylists(@QueryMap Map<String, Object> queries);

    @GET("channels/")
    Observable<ItemModel> getChannelDetails(@QueryMap Map<String, Object> queries);

    @GET("videos?")
    Observable<ItemModel> getVideos(@QueryMap Map<String, Object> queries);

    @GET
    Observable<List<Object>> getSearchSuggestions(@Url String url, @QueryMap Map<String, Object> queries);
}