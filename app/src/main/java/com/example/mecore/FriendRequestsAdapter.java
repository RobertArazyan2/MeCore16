package com.example.mecore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendRequestsAdapter extends RecyclerView.Adapter<FriendRequestsAdapter.FriendRequestViewHolder> {
    private final List<FriendRequest> friendRequests;

    // Constructor
    public FriendRequestsAdapter(List<FriendRequest> friendRequests) {
        this.friendRequests = friendRequests;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each friend request item (item_friend_request.xml)
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest currentRequest = friendRequests.get(position);

        // Set the username from the FriendRequest to the TextView
        holder.usernameTextView.setText(currentRequest.getUsername());
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    // ViewHolder class for the friend request item
    public static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameTextView;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the TextView (ensure the ID matches the layout in item_friend_request.xml)
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
        }
    }
}
