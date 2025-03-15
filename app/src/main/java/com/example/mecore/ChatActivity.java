package com.example.mecore;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageInput;

    private List<Message> messageList; // List to hold chat messages
    private ChatAdapter chatAdapter; // The adapter to bind the data

    @SuppressLint({"NotifyDataSetChanged", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerView); // Find RecyclerView
        messageInput = findViewById(R.id.messageInput); // Find message input field
        Button sendButton = findViewById(R.id.sendButton); // Find send button

        // Initialize the message list and the adapter
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList); // Initialize ChatAdapter with message list

        // Set up the RecyclerView to display the chat messages
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Use LinearLayoutManager to display messages vertically
        recyclerView.setAdapter(chatAdapter); // Set the adapter for RecyclerView

        // Sample messages (Replace with real data or database/API calls)
        messageList.add(new Message("User1", "Hello!"));
        messageList.add(new Message("User2", "Hi, how are you?"));
        chatAdapter.notifyDataSetChanged(); // Notify the adapter that data has changed

        // Set click listener for the send button to send messages
        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim(); // Get message input
            if (!messageText.isEmpty()) { // If message is not empty
                // Add the message to the list
                messageList.add(new Message("You", messageText));

                // Clear the input field after sending the message
                messageInput.setText("");

                // Notify the adapter to refresh the RecyclerView with the new message
                chatAdapter.notifyItemInserted(messageList.size() - 1);

                // Scroll to the bottom of the chat to show the latest message
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        });
    }
}
