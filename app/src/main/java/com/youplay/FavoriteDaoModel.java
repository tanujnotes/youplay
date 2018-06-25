package com.youplay;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.NotNull;

/**
 * Created by tan on 28/02/17.
 **/

@Entity
public class FavoriteDaoModel {
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    @Index(unique = true)
    String videoId;

    long timeInMillis;
    String videoObject;

    @Generated(hash = 171497456)
    public FavoriteDaoModel(Long id, @NotNull String videoId, long timeInMillis,
            String videoObject) {
        this.id = id;
        this.videoId = videoId;
        this.timeInMillis = timeInMillis;
        this.videoObject = videoObject;
    }

    @Generated(hash = 1762005005)
    public FavoriteDaoModel() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVideoId() {
        return this.videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoObject() {
        return this.videoObject;
    }

    public void setVideoObject(String videoObject) {
        this.videoObject = videoObject;
    }

    public long getTimeInMillis() {
        return this.timeInMillis;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }
}
