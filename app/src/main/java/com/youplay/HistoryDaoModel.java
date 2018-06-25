package com.youplay;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by tan on 07/09/17.
 **/

@Entity
public class HistoryDaoModel {
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    String videoId;

    long timeInMillis;
    String videoObject;
    @Generated(hash = 1186639184)
    public HistoryDaoModel(Long id, @NotNull String videoId, long timeInMillis,
            String videoObject) {
        this.id = id;
        this.videoId = videoId;
        this.timeInMillis = timeInMillis;
        this.videoObject = videoObject;
    }
    @Generated(hash = 424508865)
    public HistoryDaoModel() {
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
    public long getTimeInMillis() {
        return this.timeInMillis;
    }
    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }
    public String getVideoObject() {
        return this.videoObject;
    }
    public void setVideoObject(String videoObject) {
        this.videoObject = videoObject;
    }

}
