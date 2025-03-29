package com.example.mecore;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private static final String TAG = "ChatAdapter";
    private final List<Message> messageList;

    public ChatAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (message == null) {
            Log.e(TAG, "Message at position " + position + " is null");
            return;
        }

        // Log the message ID for debugging
        Log.d(TAG, "Binding message with ID: " + message.getId());

        // Set message text
        if (holder.messageText != null) {
            holder.messageText.setText(message.getText() != null ? message.getText() : "");
        } else {
            Log.e(TAG, "messageText TextView is null at position " + position);
        }

        // Set timestamp
        if (holder.timestampText != null) {
            if (message.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String timeString = sdf.format(message.getTimestamp().toDate());
                holder.timestampText.setText(timeString);
            } else {
                holder.timestampText.setText("");
            }
        } else {
            Log.e(TAG, "timestampText TextView is null at position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            // messageIdText = itemView.findViewById(R.id.messageIdText); // Uncomment if added to layout

            if (messageText == null) {
                Log.e(TAG, "messageText not found in itemView");
            }
            if (timestampText == null) {
                Log.e(TAG, "timestampText not found in itemView");
            }
        }
    }
}