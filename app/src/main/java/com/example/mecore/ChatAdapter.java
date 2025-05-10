package com.example.mecore;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserId;
    private OnMessageClickListener onMessageClickListener;

    public interface OnMessageClickListener {
        void onMessageClick(int position);
    }

    public ChatAdapter(List<Message> messageList, String otherUsername, OnMessageClickListener listener) {
        this.messageList = messageList;
        this.onMessageClickListener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.senderNameTextView.setText("Me");
            sentHolder.messageTextView.setText(message.getText());
            sentHolder.timestampTextView.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(message.getTimestamp().toDate()));
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.senderNameTextView.setText(message.getSenderUsername() != null ? message.getSenderUsername() : "Unknown");
            receivedHolder.messageTextView.setText(message.getText());
            receivedHolder.timestampTextView.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(message.getTimestamp().toDate()));
        }

        holder.itemView.setOnClickListener(v -> {
            if (onMessageClickListener != null) {
                onMessageClickListener.onMessageClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameTextView, messageTextView, timestampTextView;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }

    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameTextView, messageTextView, timestampTextView;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}