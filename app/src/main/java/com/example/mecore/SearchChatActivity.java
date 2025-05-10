package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Define the OnMessageLongClickListener interface (if not already defined elsewhere)
interface OnMessageLongClickListener {
    void onMessageLongClick(int position);
}

public class SearchChatActivity extends AppCompatActivity {
    private static final String TAG = "SearchChatActivity";
    private String chatId;
    private List<Message> messageList;
    private ChatAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration messageListener;
    private String recipientFcmToken;

    private EditText messageInput;
    private RecyclerView recyclerView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Reusing the same layout as ChatActivity

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Signed in anonymously", Toast.LENGTH_SHORT).show();
                            initializeChat();
                        } else {
                            Toast.makeText(this, "Anonymous sign-in failed", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        } else {
            initializeChat();
        }
    }

    @SuppressLint("SetTextI18n")
    private void initializeChat() {
        String recipientId = getIntent().getStringExtra("recipientId");
        String otherUsername = getIntent().getStringExtra("otherUsername");
        String currentUsername = getIntent().getStringExtra("currentUsername");

        if (recipientId == null) {
            Toast.makeText(this, "Unable to start chat: Missing recipient ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Fetching FCM token for recipient: " + recipientId);
        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        recipientFcmToken = documentSnapshot.getString("fcmToken");
                        if (recipientFcmToken == null) {
                            Log.w(TAG, "Recipient FCM token is null for user: " + recipientId);
                            Toast.makeText(this, "Recipient hasn't set up notifications yet", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Recipient FCM token fetched: " + recipientFcmToken);
                        }
                    } else {
                        Log.w(TAG, "Recipient document not found for user: " + recipientId);
                        Toast.makeText(this, "Recipient not found in database", Toast.LENGTH_SHORT).show();
                    }
                    setupUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch recipient FCM token: " + e.getMessage());
                    Toast.makeText(this, "Failed to fetch recipient info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setupUI();
                });
    }

    @SuppressLint("SetTextI18n")
    private void setupUI() {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        chatId = createChatId(currentUserId, getIntent().getStringExtra("recipientId"));

        messageInput = findViewById(R.id.messageEditText);
        ImageButton sendMessageBtn = findViewById(R.id.sendButton);
        ImageButton backBtn = findViewById(R.id.back_btn);
        TextView otherUsernameTextView = findViewById(R.id.chatTitle);
        recyclerView = findViewById(R.id.chat_recycler_view);

        String otherUsername = getIntent().getStringExtra("otherUsername");
        String recipientId = getIntent().getStringExtra("recipientId");

        if (otherUsername != null) {
            otherUsernameTextView.setText("Chat with " + otherUsername);
        }

        // Navigate to SearchUserProfileActivity when back button is clicked
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(SearchChatActivity.this, SearchUserProfileActivity.class);
            intent.putExtra("userId", recipientId);
            intent.putExtra("username", otherUsername);
            startActivity(intent);
            finish();
        });

        messageList = new ArrayList<>();
        // Add a no-op OnMessageLongClickListener since it's required by ChatAdapter
        adapter = new ChatAdapter(messageList, otherUsername, position -> {
            // No action needed if long-click is not implemented
            Log.d(TAG, "Long click on message at position: " + position);
        });
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        sendMessageBtn.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessageBtn.setEnabled(false);
                sendMessage(messageText);
                new android.os.Handler().postDelayed(() -> sendMessageBtn.setEnabled(true), 1000);
            }
        });

        setupChatRecyclerView();
    }

    private String createChatId(String user1, String user2) {
        List<String> userIds = Arrays.asList(user1, user2);
        userIds.sort(String::compareTo);
        return userIds.get(0) + "_" + userIds.get(1);
    }

    private void setupChatRecyclerView() {
        Query query = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        messageListener = query.addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to load messages: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (querySnapshot != null) {
                for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                    Message message = dc.getDocument().toObject(Message.class);
                    message.setId(dc.getDocument().getId());
                    switch (dc.getType()) {
                        case ADDED:
                            // Check if the message already exists to avoid duplicates
                            boolean exists = false;
                            for (Message existingMessage : messageList) {
                                if (existingMessage.getId().equals(message.getId())) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                messageList.add(0, message);
                                adapter.notifyItemInserted(0);
                                recyclerView.smoothScrollToPosition(0);
                            }
                            break;
                        case MODIFIED:
                            for (int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getId().equals(message.getId())) {
                                    messageList.set(i, message);
                                    adapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            break;
                        case REMOVED:
                            for (int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getId().equals(message.getId())) {
                                    messageList.remove(i);
                                    adapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                            break;
                    }
                }
            }
        });
    }

    private void sendMessage(String messageText) {
        Timestamp timestamp = Timestamp.now();
        Message message = new Message(messageText, timestamp);
        message.setSenderId(Objects.requireNonNull(auth.getCurrentUser()).getUid());
        Log.d(TAG, "sendMessage called with text: " + messageText);

        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // Do not add the message to messageList here; let the Firestore listener handle it
                    messageInput.setText("");
                    sendNotification(messageText);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendNotification(String message) {
        String currentUsername = getIntent().getStringExtra("currentUsername");
        Log.d(TAG, "sendNotification called for message: " + message + " to token: " + recipientFcmToken);

        if (recipientFcmToken == null) {
            Log.w(TAG, "Skipping notification: No recipient token available");
            Toast.makeText(this, "Recipient hasn't set up notifications yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject notificationObj = new JSONObject();
            notificationObj.put("title", currentUsername + ": New Message");
            notificationObj.put("body", message);

            JSONObject dataObj = new JSONObject();
            dataObj.put("userId", Objects.requireNonNull(auth.getCurrentUser()).getUid());
            dataObj.put("username", currentUsername);

            jsonObject.put("notification", notificationObj);
            jsonObject.put("data", dataObj);
            jsonObject.put("to", recipientFcmToken);
            jsonObject.put("tag", message.hashCode() + System.currentTimeMillis());

            callApi(jsonObject);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer YOUR_ACTUAL_FCM_SERVER_KEY_HERE") // Replace with your real FCM Server Key
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Notification request failed: " + e.getMessage());
                    Toast.makeText(SearchChatActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseMessage = response.message();
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "FCM Response: " + response.code() + " - " + responseMessage + " - " + responseBody);
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(SearchChatActivity.this, "Notification sent", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(SearchChatActivity.this, "Notification failed: " + responseBody + " (" + response.code() + ")", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}