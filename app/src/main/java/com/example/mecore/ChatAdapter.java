package com.example.mecore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private final List<Message> messageList;
    private String currentUserId;

    public ChatAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = this.currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.messageTextView.setText(message.getMessage());

        // Align message based on sender
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        if (message.getSenderId().equals(currentUserId)) {
            // Sent by current user: align to the right
            params.gravity = android.view.Gravity.END;
            holder.messageContainer.setBackgroundResource(android.R.color.holo_blue_light); // Example color for sent messages
        } else {
            // Received from friend: align to the left
            params.gravity = android.view.Gravity.START;
            holder.messageContainer.setBackgroundResource(android.R.color.darker_gray); // Example color for received messages
        }
        holder.messageContainer.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        LinearLayout messageContainer;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }
    }
}