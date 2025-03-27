package com.example.mecore;

import com.google.firebase.Timestamp;

public class Message {
    private String senderId;
    private String receiverId;
    private String message;
    private Timestamp timestamp;

    // Constructor for Firestore (when loading messages)
    public Message() {
    }

    // Constructor for creating a new message
    public Message(String senderId, String receiverId, String message, Timestamp timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Constructor for the current implementation (temporary, for compatibility)
    public Message(String sender, String message) {
        this.senderId = sender;
        this.message = message;
        this.timestamp = Timestamp.now();
    }

    // Getters and setters
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}