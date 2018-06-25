package com.youplay;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlaylistItemsModel {

    @SerializedName("kind")
    @Expose
    private String kind;
    @SerializedName("etag")
    @Expose
    private String etag;
    @SerializedName("nextPageToken")
    @Expose
    private String nextPageToken;
    @SerializedName("pageInfo")
    @Expose
    private ItemModel.PageInfo pageInfo;
    @SerializedName("items")
    @Expose
    private List<Item> items = null;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public ItemModel.PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(ItemModel.PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public class Item {

        @SerializedName("kind")
        @Expose
        private String kind;
        @SerializedName("etag")
        @Expose
        private String etag;
        @SerializedName("id")
        @Expose
        private String id;
        @SerializedName("snippet")
        @Expose
        private ItemModel.Snippet snippet;
        @SerializedName("contentDetails")
        @Expose
        private ItemModel.ContentDetails contentDetails;
        @SerializedName("statistics")
        @Expose
        private ItemModel.Statistics statistics;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getEtag() {
            return etag;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ItemModel.Snippet getSnippet() {
            return snippet;
        }

        public void setSnippet(ItemModel.Snippet snippet) {
            this.snippet = snippet;
        }

        public ItemModel.ContentDetails getContentDetails() {
            return contentDetails;
        }

        public void setContentDetails(ItemModel.ContentDetails contentDetails) {
            this.contentDetails = contentDetails;
        }

        public ItemModel.Statistics getStatistics() {
            return statistics;
        }

        public void setStatistics(ItemModel.Statistics statistics) {
            this.statistics = statistics;
        }
    }
}