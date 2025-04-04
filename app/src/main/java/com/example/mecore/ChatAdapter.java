package com.example.mecore;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;
    private String currentUserId;
    private String opponentUsername;

    public ChatAdapter(List<Message> messages, String opponentUsername) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        this.opponentUsername = opponentUsername;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView senderNameTextView;
        TextView timestampTextView;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);

            // Add null checks for debugging
            if (messageTextView == null) {
                Log.e(TAG, "messageTextView is null in item_message_sent.xml. Check the layout for ID 'messageTextView'.");
            }
            if (senderNameTextView == null) {
                Log.e(TAG, "senderNameTextView is null in item_message_sent.xml. Check the layout for ID 'senderNameTextView'.");
            }
            if (timestampTextView == null) {
                Log.e(TAG, "timestampTextView is null in item_message_sent.xml. Check the layout for ID 'timestampTextView'.");
            }
        }

        void bind(Message message) {
            if (messageTextView != null) {
                messageTextView.setText(message.getText());
            }
            if (senderNameTextView != null) {
                senderNameTextView.setText("Me");
            }
            if (timestampTextView != null) {
                timestampTextView.setText(formatTimestamp(message.getTimestamp()));
            }
        }
    }

    // ViewHolder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView senderNameTextView;
        TextView timestampTextView;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);

            // Add null checks for debugging
            if (messageTextView == null) {
                Log.e(TAG, "messageTextView is null in item_message_received.xml. Check the layout for ID 'messageTextView'.");
            }
            if (senderNameTextView == null) {
                Log.e(TAG, "senderNameTextView is null in item_message_received.xml. Check the layout for ID 'senderNameTextView'.");
            }
            if (timestampTextView == null) {
                Log.e(TAG, "timestampTextView is null in item_message_received.xml. Check the layout for ID 'timestampTextView'.");
            }
        }

        void bind(Message message) {
            if (messageTextView != null) {
                messageTextView.setText(message.getText());
            }
            if (senderNameTextView != null) {
                senderNameTextView.setText(message.getSenderUsername());
            }
            if (timestampTextView != null) {
                timestampTextView.setText(formatTimestamp(message.getTimestamp()));
            }
        }
    }

    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}