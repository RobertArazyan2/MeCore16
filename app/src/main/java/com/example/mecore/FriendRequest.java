package com.example.mecore;

public class FriendRequest {
    private final String requestId;
    private final String senderId;
    private final String username;

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