package com.example.mecore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendRequest {
    private final String requestId;
    private final String senderId;
    private final String receiverId;
    private final String username;
    private View itemView;  // This will hold the layout reference

    public FriendRequest(String requestId, String senderId, String receiverId, String username) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.username = username;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getUsername() {
        return username;
    }

    // This method would create a view for the item based on item_friend_request.xml
    public View getItemView(LayoutInflater inflater, ViewGroup parent) {
        if (itemView == null) {
            itemView = inflater.inflate(R.layout.item_friend_request, parent, false);
            // Here, you would typically set data to views from item_friend_request.xml
            ImageView profileImageView = itemView.findViewById(R.id.profileImageView);
            TextView usernameTextView = itemView.findViewById(R.id.usernameTextView);
            Button acceptButton = itemView.findViewById(R.id.acceptButton);
            Button declineButton = itemView.findViewById(R.id.declineButton);

            usernameTextView.setText(this.username);
            // You can load the profile image here too if you have a URL or resource for it
        }
        return itemView;
    }
}
