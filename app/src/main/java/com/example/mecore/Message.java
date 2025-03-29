package com.example.mecore;

import com.google.firebase.Timestamp;

public class Message {
    private String id; // Added field for the message ID
    private String text;
    private Timestamp timestamp;
    private String senderId;

    public Message() {
        // Required for Firestore
    }

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

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}