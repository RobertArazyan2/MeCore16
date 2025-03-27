package com.example.mecore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<User> friendsList;
    private final OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(User user);
    }

    public ChatListAdapter(List<User> friendsList, OnFriendClickListener listener) {
        this.friendsList = friendsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        User user = friendsList.get(position);
        holder.textView.setText(user.getUsername());
        holder.itemView.setOnClickListener(v -> listener.onFriendClick(user));
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}