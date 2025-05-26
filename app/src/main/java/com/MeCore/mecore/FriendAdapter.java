package com.MeCore.mecore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private final List<Friend> friendList;
    private final OnFriendClickListener clickListener;
    private final OnFriendDeleteListener deleteListener;

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    public interface OnFriendDeleteListener {
        void onFriendDelete(Friend friend);
    }

    public FriendAdapter(List<Friend> friendList, OnFriendClickListener clickListener, OnFriendDeleteListener deleteListener) {
        this.friendList = friendList;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.usernameTextView.setText(friend.getUsername());
        holder.itemView.setOnClickListener(v -> clickListener.onFriendClick(friend));
        holder.deleteButton.setOnClickListener(v -> deleteListener.onFriendDelete(friend));
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        ImageView deleteButton;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.friendUsernameTextView);
            deleteButton = itemView.findViewById(R.id.deleteFriendButton);
        }
    }
}