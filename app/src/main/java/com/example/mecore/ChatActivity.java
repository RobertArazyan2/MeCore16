package com.example.mecore;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerView;
    private EditText messageInput;

    private List<Message> messageList;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore db;
    private String chatId;
    private String friendId;
    private String currentUserId;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @SuppressLint({"NotifyDataSetChanged", "MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        Button sendButton = findViewById(R.id.sendButton);
        TextView friendUsernameTextView = findViewById(R.id.friendUsernameTextView);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        // Get data from Intent
        chatId = getIntent().getStringExtra("chatId");
        friendId = getIntent().getStringExtra("friendId");
        String friendUsername = getIntent().getStringExtra("friendUsername");

        if (chatId == null || friendId == null || friendUsername == null) {
            Toast.makeText(this, "Error: Missing chat information", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set the friend's username in the header
        friendUsernameTextView.setText("Chatting with " + friendUsername);

        // Initialize the message list and adapter
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Load chat history from Firestore
        loadChatHistory();

        // Set click listener for the send button
        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
                messageInput.setText(""); // Clear the input field
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadChatHistory() {
        executorService.execute(() -> db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Message message = doc.toObject(Message.class);
                            messageList.add(message);
                        }
                        runOnUiThread(() -> {
                            chatAdapter.notifyDataSetChanged();
                            if (!messageList.isEmpty()) {
                                recyclerView.scrollToPosition(messageList.size() - 1);
                            }
                        });
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "loadChatHistory: Failed to load messages: " + errorMessage);
                        runOnUiThread(() -> Toast.makeText(this, "Failed to load messages: " + errorMessage, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    private void sendMessage(String messageText) {
        // Create a new message object
        Message message = new Message(
                currentUserId,
                friendId,
                messageText,
                Timestamp.now()
        );

        // Add the message to the local list for immediate display
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        // Save the message to Firestore
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", message.getSenderId());
        messageData.put("receiverId", message.getReceiverId());
        messageData.put("message", message.getMessage());
        messageData.put("timestamp", message.getTimestamp());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "sendMessage: Message sent successfully: " + documentReference.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "sendMessage: Failed to send message: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_LONG).show());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}