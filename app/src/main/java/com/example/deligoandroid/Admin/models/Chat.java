package com.example.deligoandroid.Admin.models;

import com.google.firebase.database.PropertyName;

public class Chat {
    private String messageId;
    private String message;
    private String senderId;
    private String senderName;
    private String senderType;
    @PropertyName("isRead")
    private boolean read;
    private long timestamp;
    private String userId;

    public Chat() {
        // Required empty constructor for Firebase
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    @PropertyName("isRead")
    public boolean isRead() {
        return read;
    }

    @PropertyName("isRead")
    public void setRead(boolean read) {
        this.read = read;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
} 