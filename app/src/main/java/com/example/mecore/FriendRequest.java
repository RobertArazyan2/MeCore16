package com.example.mecore;

public class FriendRequest {
    private final String requestId;  // The document ID of the request in Firestore
    private final String senderId;   // The ID of the user who sent the request
    private final String username;   // The username of the sender

    public FriendRequest(String requestId, String senderId, String username) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.username = username;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getUsername() {
        return username;
    }
}