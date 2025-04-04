package com.example.mecore;

import com.google.firebase.Timestamp;

public class Message {
    private String id;
    private String text; // Renamed from messageText to text
    private String senderId;
    private Timestamp timestamp;
    private String senderUsername;

    public Message() {}

    public Message(String text, Timestamp timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    // Deprecated: Remove if not needed elsewhere
    @Deprecated
    public String getMessageText() {
        return text;
    }

    @Deprecated
    public void setMessageText(String messageText) {
        this.text = messageText;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
}