package com.youplay;

import android.support.annotation.Keep;

import java.util.Date;

/**
 * Created by tan on 25/01/18.
 **/

@Keep
public class ChatMessageModel {

    private String messageText;
    private String messageUser;
    private String senderUid;
    private long messageTime;

    public ChatMessageModel(String messageText, String messageUser, String senderUid) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.senderUid = senderUid;

        // Initialize to current time
        messageTime = new Date().getTime();
    }

    public ChatMessageModel() {

    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }
}