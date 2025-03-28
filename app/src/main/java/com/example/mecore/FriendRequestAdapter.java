package com.example.mecore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private final List<FriendRequest> friendRequests;
    private final OnFriendRequestActionListener listener;

    public interface OnFriendRequestActionListener {
        void onAccept(FriendRequest request, int position);
        void onDecline(FriendRequest request, int position);
    }

    public FriendRequestAdapter(List<FriendRequest> friendRequests, OnFriendRequestActionListener listener) {
        this.friendRequests = friendRequests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest request = friendRequests.get(position);
        holder.usernameTextView.setText(request.getUsername());

        holder.acceptButton.setOnClickListener(v -> listener.onAccept(request, position));
        holder.declineButton.setOnClickListener(v -> listener.onDecline(request, position));
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    public static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        Button acceptButton;
        Button declineButton;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }
    }
}